package org.mitre.synthea.br.plausibility.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.br.plausibility.BreastCancerCodeSets;
import org.mitre.synthea.br.plausibility.ClinicalCode;
import org.mitre.synthea.br.plausibility.HealthRecordScan;
import org.mitre.synthea.br.plausibility.PlausibilityCatalogLoader;
import org.mitre.synthea.br.plausibility.PlausibilityRule;
import org.mitre.synthea.br.plausibility.RuleMetadata;
import org.mitre.synthea.br.plausibility.Violation;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;

/**
 * {@code PLAUS-001}: treatment procedure without prior breast cancer diagnosis.
 */
public final class Plaus001TreatmentWithoutDiagnosis implements PlausibilityRule {

  static final String RULE_ID = "PLAUS-001";

  private final RuleMetadata metadata;
  private final BreastCancerCodeSets codeSets;

  /**
   * Create rule using catalog metadata and clinical codes.
   *
   * @param catalogLoader loaded catalog
   */
  public Plaus001TreatmentWithoutDiagnosis(PlausibilityCatalogLoader catalogLoader) {
    this.metadata = catalogLoader.getRule(RULE_ID);
    this.codeSets = catalogLoader.getCodeSets();
  }

  @Override
  public List<Violation> evaluate(Person person) {
    List<Violation> violations = new ArrayList<>();
    String patientId = HealthRecordScan.patientId(person);
    ClinicalCode diagnosisCode = codeSets.getBreastCancerDiagnosis();

    for (HealthRecord.Procedure procedure : HealthRecordScan.allProcedures(person)) {
      if (!HealthRecordScan.matchesAnyCode(procedure, codeSets.getTreatmentProcedures())) {
        continue;
      }
      if (!hasDiagnosisBefore(person, diagnosisCode, procedure.start)) {
        violations.add(buildViolation(patientId, procedure, diagnosisCode, "procedure"));
      }
    }

    return violations;
  }

  private boolean hasDiagnosisBefore(Person person, ClinicalCode diagnosisCode, long eventStart) {
    for (HealthRecord.Entry condition : HealthRecordScan.allConditions(person)) {
      if (HealthRecordScan.matchesCode(condition, diagnosisCode) && condition.start < eventStart) {
        return true;
      }
    }
    return false;
  }

  private Violation buildViolation(String patientId, HealthRecord.Entry entry,
      ClinicalCode diagnosisCode, String eventType) {
    Map<String, Long> timestamps = new LinkedHashMap<>();
    timestamps.put(eventType + "Start", entry.start);
    String description = String.format(
        "%s sem diagnóstico prévio de câncer de mama (%s) antes do tratamento",
        metadata.getTitle(),
        diagnosisCode.getCode());
    return new Violation(
        metadata.getId(),
        metadata.getSeverity(),
        patientId,
        description,
        timestamps);
  }
}
