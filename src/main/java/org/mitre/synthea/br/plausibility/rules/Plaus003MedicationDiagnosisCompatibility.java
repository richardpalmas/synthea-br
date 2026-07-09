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
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;

/**
 * {@code PLAUS-003}: chemotherapy medication without breast cancer diagnosis on the patient.
 */
public final class Plaus003MedicationDiagnosisCompatibility implements PlausibilityRule {

  static final String RULE_ID = "PLAUS-003";

  private final RuleMetadata metadata;
  private final BreastCancerCodeSets codeSets;

  /**
   * Create rule using catalog metadata and clinical codes.
   *
   * @param catalogLoader loaded catalog
   */
  public Plaus003MedicationDiagnosisCompatibility(PlausibilityCatalogLoader catalogLoader) {
    this.metadata = catalogLoader.getRule(RULE_ID);
    this.codeSets = catalogLoader.getCodeSets();
  }

  @Override
  public List<Violation> evaluate(Person person) {
    List<Violation> violations = new ArrayList<>();
    if (hasBreastCancerDiagnosis(person)) {
      return violations;
    }

    String patientId = HealthRecordScan.patientId(person);
    ClinicalCode diagnosisCode = codeSets.getBreastCancerDiagnosis();

    for (Medication medication : HealthRecordScan.allMedications(person)) {
      if (!HealthRecordScan.matchesAnyCode(medication, codeSets.getChemotherapyMedications())) {
        continue;
      }
      Map<String, Long> timestamps = new LinkedHashMap<>();
      timestamps.put("medicationStart", medication.start);
      String description = String.format(
          "%s: medicação de quimioterapia (%s) sem diagnóstico de câncer de mama (%s)",
          metadata.getTitle(),
          medication.type,
          diagnosisCode.getCode());
      violations.add(new Violation(
          metadata.getId(),
          metadata.getSeverity(),
          patientId,
          description,
          timestamps));
    }

    return violations;
  }

  private boolean hasBreastCancerDiagnosis(Person person) {
    ClinicalCode diagnosisCode = codeSets.getBreastCancerDiagnosis();
    for (Entry condition : HealthRecordScan.allConditions(person)) {
      if (HealthRecordScan.matchesCode(condition, diagnosisCode)) {
        return true;
      }
    }
    return false;
  }
}
