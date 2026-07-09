package org.mitre.synthea.br.plausibility.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.mitre.synthea.br.plausibility.BreastCancerCodeSets;
import org.mitre.synthea.br.plausibility.ClinicalCode;
import org.mitre.synthea.br.plausibility.HealthRecordScan;
import org.mitre.synthea.br.plausibility.PlausibilityCatalogLoader;
import org.mitre.synthea.br.plausibility.PlausibilityRule;
import org.mitre.synthea.br.plausibility.RuleMetadata;
import org.mitre.synthea.br.plausibility.Violation;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;

/**
 * {@code PLAUS-002}: treatment procedure occurring before the earliest diagnostic exam.
 */
public final class Plaus002TreatmentBeforeDiagnosticExam implements PlausibilityRule {

  static final String RULE_ID = "PLAUS-002";

  private final RuleMetadata metadata;
  private final BreastCancerCodeSets codeSets;

  /**
   * Create rule using catalog metadata and clinical codes.
   *
   * @param catalogLoader loaded catalog
   */
  public Plaus002TreatmentBeforeDiagnosticExam(PlausibilityCatalogLoader catalogLoader) {
    this.metadata = catalogLoader.getRule(RULE_ID);
    this.codeSets = catalogLoader.getCodeSets();
  }

  @Override
  public List<Violation> evaluate(Person person) {
    List<Violation> violations = new ArrayList<>();
    OptionalLong earliestDiagnostic = findEarliestDiagnosticStart(person);
    if (!earliestDiagnostic.isPresent()) {
      return violations;
    }

    String patientId = HealthRecordScan.patientId(person);
    long diagnosticStart = earliestDiagnostic.getAsLong();

    for (Procedure procedure : HealthRecordScan.allProcedures(person)) {
      if (!HealthRecordScan.matchesAnyCode(procedure, codeSets.getTreatmentProcedures())) {
        continue;
      }
      if (procedure.start < diagnosticStart) {
        Map<String, Long> timestamps = new LinkedHashMap<>();
        timestamps.put("treatmentStart", procedure.start);
        timestamps.put("diagnosticStart", diagnosticStart);
        String description = String.format(
            "%s: procedimento de tratamento (%s) em %d antes do exame diagnóstico em %d",
            metadata.getTitle(),
            procedure.type,
            procedure.start,
            diagnosticStart);
        violations.add(new Violation(
            metadata.getId(),
            metadata.getSeverity(),
            patientId,
            description,
            timestamps));
      }
    }

    return violations;
  }

  private OptionalLong findEarliestDiagnosticStart(Person person) {
    OptionalLong earliest = OptionalLong.empty();
    for (Procedure procedure : HealthRecordScan.allProcedures(person)) {
      if (!HealthRecordScan.matchesAnyCode(procedure, codeSets.getDiagnosticProcedures())) {
        continue;
      }
      if (!earliest.isPresent() || procedure.start < earliest.getAsLong()) {
        earliest = OptionalLong.of(procedure.start);
      }
    }
    return earliest;
  }
}
