package org.mitre.synthea.br.plausibility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clinical code sets extracted from the breast cancer plausibility catalog.
 */
public final class BreastCancerCodeSets {

  private final ClinicalCode breastCancerDiagnosis;
  private final List<ClinicalCode> treatmentProcedures;
  private final List<ClinicalCode> diagnosticProcedures;
  private final List<ClinicalCode> chemotherapyMedications;

  /**
   * Parse clinical code sets from catalog JSON.
   *
   * @param clinicalCodes clinicalCodes section of catalog
   */
  public BreastCancerCodeSets(JsonObject clinicalCodes) {
    this.breastCancerDiagnosis = ClinicalCode.fromJson(
        clinicalCodes.getAsJsonObject("breastCancerDiagnosis"));
    this.treatmentProcedures = parseCodeList(clinicalCodes.getAsJsonArray("treatmentProcedures"));
    this.diagnosticProcedures = parseCodeList(
        clinicalCodes.getAsJsonArray("diagnosticProcedures"));
    this.chemotherapyMedications = parseCodeList(
        clinicalCodes.getAsJsonArray("chemotherapyMedications"));
  }

  public ClinicalCode getBreastCancerDiagnosis() {
    return breastCancerDiagnosis;
  }

  public List<ClinicalCode> getTreatmentProcedures() {
    return treatmentProcedures;
  }

  public List<ClinicalCode> getDiagnosticProcedures() {
    return diagnosticProcedures;
  }

  public List<ClinicalCode> getChemotherapyMedications() {
    return chemotherapyMedications;
  }

  private static List<ClinicalCode> parseCodeList(JsonArray array) {
    List<ClinicalCode> codes = new ArrayList<>();
    for (JsonElement element : array) {
      codes.add(ClinicalCode.fromJson(element.getAsJsonObject()));
    }
    return Collections.unmodifiableList(codes);
  }
}
