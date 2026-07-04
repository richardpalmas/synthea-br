package org.mitre.synthea.br.web;

import java.io.PrintStream;
import java.util.function.Consumer;

import org.mitre.synthea.br.ai.AiEnrichmentConfig;
import org.mitre.synthea.br.condition.GateMode;
import org.mitre.synthea.br.condition.UnknownTargetConditionException;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.GenerationRunHelper;

/**
 * Applies {@link GenerationRequest} to {@link Config} and runs {@link Generator}.
 */
public class GenerationService {

  /**
   * Run generation synchronously on the calling thread.
   *
   * @param request validated web request
   * @param logCapture optional log capture (may be null)
   * @return the generator after run (for stats)
   * @throws Exception when configuration or generation fails
   */
  public Generator run(GenerationRequest request, LogCaptureStream logCapture) throws Exception {
    return run(request, logCapture, null);
  }

  /**
   * Run generation synchronously on the calling thread, notifying the caller as soon as the
   * {@link Generator} instance exists (before {@code generator.run()} starts) so callers can
   * expose live progress via {@code generator.totalGeneratedPopulation}.
   *
   * @param request validated web request
   * @param logCapture optional log capture (may be null)
   * @param onGeneratorReady optional callback invoked with the generator instance right after
   *     construction, before generation begins (may be null)
   * @return the generator after run (for stats)
   * @throws Exception when configuration or generation fails
   */
  public Generator run(GenerationRequest request, LogCaptureStream logCapture,
      Consumer<Generator> onGeneratorReady) throws Exception {
    applyRequestToConfig(request);
    GenerationRunHelper.prepareOutputDirectory();

    Config.set("generate.default_population", Integer.toString(request.population));

    Generator.GeneratorOptions options = new Generator.GeneratorOptions();
    options.seed = request.seed;
    options.clinicianSeed = request.seed;
    options.population = request.population;

    String normalizedGender = request.normalizedGender();
    if (normalizedGender != null && !normalizedGender.isEmpty()) {
      options.gender = normalizedGender;
    } else {
      options.gender = null;
    }

    if (request.minAge != null && request.maxAge != null) {
      options.ageSpecified = true;
      options.minAge = request.minAge;
      options.maxAge = request.maxAge;
    } else {
      options.ageSpecified = false;
    }

    // Web UI: -p N = exactly N exported patients per slot (one export per slot).
    // overflow=false disables upstream "overflow" exports: with overflow=true, a patient who
    // dies during simulation is exported and the slot retries, producing N + extras (e.g. 13).
    // With overflow=false, a deceased patient who satisfies the gate counts as one of the N —
    // they remain in the cohort and are not replaced by an additional export.
    options.overflow = false;

    Exporter.ExporterRuntimeOptions exportOptions = new Exporter.ExporterRuntimeOptions();
    GenerationRunHelper.resetOptionsFromConfig(options, exportOptions);

    if (!GenerationRunHelper.validateConfig(options, false)) {
      throw new IllegalStateException(
          "Configuração inválida: data final de simulação incompatível com anos de histórico.");
    }

    PrintStreamRedirect redirect = null;
    try {
      if (logCapture != null) {
        redirect = PrintStreamRedirect.install(logCapture);
      }
      Generator generator = new Generator(options, exportOptions);
      if (onGeneratorReady != null) {
        onGeneratorReady.accept(generator);
      }
      generator.run();
      return generator;
    } catch (UnknownTargetConditionException ex) {
      throw new IllegalArgumentException(
          "Condição alvo não suportada: " + ex.getMessage(), ex);
    } catch (IllegalStateException ex) {
      throw new IllegalStateException(toUserMessage(ex), ex);
    } finally {
      if (redirect != null) {
        redirect.restore();
      }
    }
  }

  private static void applyRequestToConfig(GenerationRequest request) {
    if (request.brProfile) {
      Config.set("br.profile", "br");
    } else {
      Config.remove("br.profile");
    }

    if (request.targetCondition != null && !request.targetCondition.trim().isEmpty()) {
      Config.set("br.target_condition", request.targetCondition.trim());
      Config.set("br.target_condition.gate_mode",
          GateMode.fromConfigValue(request.gateMode).getConfigValue());
    } else {
      Config.remove("br.target_condition");
      Config.remove("br.target_condition.gate_mode");
    }

    Config.set("exporter.fhir.export", Boolean.toString(request.exportFhir));
    Config.set("exporter.csv.export", Boolean.toString(request.exportCsv));
    Config.set("exporter.html.export", Boolean.toString(request.exportHtml));

    // Deceased patients may be part of the cohort; do not force alive-only retries.
    Config.set("generate.only_alive_patients", "false");

    Config.set("br.ai.enrichment.enabled", Boolean.toString(request.aiEnrichment));
    if (request.aiEnrichment) {
      String provider = request.aiProvider == null ? "openai" : request.aiProvider.trim();
      Config.set("br.ai.provider", provider);
      if (request.aiModel != null && !request.aiModel.trim().isEmpty()) {
        Config.set("br.ai.model", request.aiModel.trim());
      }
      AiEnrichmentConfig.setTransientApiKey(request.aiApiKey);
    } else {
      Config.set("br.ai.enrichment.enabled", "false");
      AiEnrichmentConfig.clearApiKey();
    }
  }

  private static String toUserMessage(IllegalStateException ex) {
    String message = ex.getMessage();
    if (message == null) {
      return "Falha na geração. Verifique os parâmetros e tente novamente.";
    }
    if (message.contains("max_attempts") || message.contains("attempts")) {
      return "Gate de condição alvo excedeu o número máximo de tentativas. "
          + "Restrinja gênero/idade (ex.: F, 45–75 para câncer de mama) ou use gate_mode=exclude.";
    }
    if (message.contains("keep module") || message.contains("keep_module")) {
      return "Módulo de gate indisponível para a condição selecionada.";
    }
    return message;
  }

  /**
   * Temporarily redirects System.out/err during generation.
   */
  static final class PrintStreamRedirect {
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final PrintStream capture;

    private PrintStreamRedirect(LogCaptureStream logCapture) {
      this.capture = logCapture.asPrintStream();
      this.originalOut = System.out;
      this.originalErr = System.err;
    }

    static PrintStreamRedirect install(LogCaptureStream logCapture) {
      PrintStreamRedirect redirect = new PrintStreamRedirect(logCapture);
      System.setOut(redirect.capture);
      System.setErr(redirect.capture);
      return redirect;
    }

    void restore() {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }
}
