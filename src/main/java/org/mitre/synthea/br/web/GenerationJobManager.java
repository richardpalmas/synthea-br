package org.mitre.synthea.br.web;

import java.io.File;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.mitre.synthea.br.ai.AiEnrichmentConfig;
import org.mitre.synthea.br.ai.AiEnrichmentProgress;
import org.mitre.synthea.br.plausibility.PlausibilityReportWriter;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.ExportFailureTracker;
import org.mitre.synthea.helpers.Config;

/**
 * Thread-safe single-job manager for web-triggered generation.
 */
public final class GenerationJobManager {

  private static final GenerationJobManager INSTANCE = new GenerationJobManager();

  private final GenerationService generationService = new GenerationService();
  private final Object lock = new Object();
  private volatile String currentJobId;
  private volatile String state = "idle";
  private volatile int requestedPopulation;
  private volatile int generatedCount;
  private volatile String outputDirectory;
  private volatile boolean manifestPresent;
  private volatile boolean htmlExportEnabled;
  private volatile boolean aiEnrichmentRequested;
  private volatile String htmlIndexPath;
  private volatile boolean plausibilityReportPresent;
  private volatile String plausibilityReportPath;
  private volatile long currentJobStartedAtMs;
  private volatile String trajectorySummary;
  private volatile String errorMessage;
  private volatile LogCaptureStream logCapture;
  private final AtomicReference<Generator> activeGenerator = new AtomicReference<>();

  private GenerationJobManager() {
  }

  /**
   * Returns the process-wide singleton instance.
   *
   * @return singleton instance
   */
  public static GenerationJobManager getInstance() {
    return INSTANCE;
  }

  /**
   * Start a generation job when none is running.
   *
   * @param request validated request
   * @return job id
   * @throws IllegalStateException when a job is already running
   */
  public String startJob(GenerationRequest request) {
    synchronized (lock) {
      if ("running".equals(state)) {
        throw new IllegalStateException("Geração em andamento");
      }
      currentJobId = UUID.randomUUID().toString();
      state = "running";
      requestedPopulation = request.population;
      generatedCount = 0;
      errorMessage = null;
      manifestPresent = false;
      htmlExportEnabled = request.exportHtml;
      aiEnrichmentRequested = request.aiEnrichment;
      htmlIndexPath = null;
      plausibilityReportPresent = false;
      plausibilityReportPath = null;
      trajectorySummary = TrajectoryWebConstants.buildTrajectorySummary(request);
      currentJobStartedAtMs = System.currentTimeMillis();
      outputDirectory = null;
      logCapture = new LogCaptureStream(System.out);
      activeGenerator.set(null);

      Thread worker = new Thread(() -> runJob(request), "synthea-web-generation");
      worker.setDaemon(true);
      worker.start();
      return currentJobId;
    }
  }

  private void runJob(GenerationRequest request) {
    try {
      Generator generator = generationService.run(request, logCapture, activeGenerator::set);
      generatedCount = generator.totalGeneratedPopulation.get();
      outputDirectory = Config.get("exporter.baseDirectory", "./output/");
      File outputDir = new File(outputDirectory);
      manifestPresent = new File(outputDir, "manifest.json").exists();
      if (request.exportHtml) {
        File htmlIndex = new File(outputDir, "html/index.html");
        if (htmlIndex.exists()) {
          htmlIndexPath = htmlIndex.getPath();
        }
      }
      File plausibilityReport =
          new File(outputDir, PlausibilityReportWriter.getReportFilename());
      plausibilityReportPresent = plausibilityReport.exists()
          && plausibilityReport.lastModified() >= currentJobStartedAtMs;
      plausibilityReportPath =
          plausibilityReportPresent ? plausibilityReport.getPath() : null;
      state = "completed";
    } catch (IllegalStateException | IllegalArgumentException ex) {
      // GenerationService already translates these into user-friendly PT-BR messages.
      errorMessage = safeMessage(ex.getMessage());
      state = "failed";
    } catch (Exception ex) {
      // Unexpected failure: never leak raw internal messages to the API; log full detail
      // to the server console (System.err was already restored by GenerationService's
      // finally block by the time this catch runs) so operators can investigate.
      ex.printStackTrace();
      errorMessage = "Falha inesperada na geração. Consulte o console do servidor para detalhes.";
      state = "failed";
    } finally {
      activeGenerator.set(null);
      // Defense in depth: never let a BYOK key outlive its job, even if AiEnrichmentService
      // was never reached (e.g. failure earlier in Generator construction/validation).
      AiEnrichmentConfig.clearApiKey();
    }
  }

  private static String safeMessage(String message) {
    if (message == null || message.isEmpty()) {
      return "Falha desconhecida na geração.";
    }
    return message;
  }

  /**
   * Builds a snapshot of the current or last job's status for API responses.
   *
   * @return snapshot of current job status for API responses
   */
  public GenerationStatus getStatus() {
    GenerationStatus status = new GenerationStatus();
    status.state = state;
    status.jobId = currentJobId;
    status.requestedPopulation = requestedPopulation;
    status.outputDirectory = outputDirectory;
    status.manifestPresent = manifestPresent;
    status.htmlExportEnabled = htmlExportEnabled;
    status.htmlIndexPath = htmlIndexPath;
    status.errorMessage = errorMessage;
    status.plausibilityReportPresent = plausibilityReportPresent;
    status.plausibilityReportPath = plausibilityReportPath;
    status.trajectorySummary = trajectorySummary;

    AiEnrichmentProgress aiProgress = AiEnrichmentProgress.getInstance();
    status.aiEnrichmentEnabled = aiEnrichmentRequested;
    status.aiEnrichmentTotal = aiProgress.getTotal();
    status.aiEnrichedCount = aiProgress.getEnriched();
    if (aiProgress.isActive()) {
      status.phase = aiProgress.getPhase();
    } else {
      status.phase = state;
    }

    Generator generator = activeGenerator.get();
    boolean liveCounterAvailable = generator != null && generator.totalGeneratedPopulation != null;
    if ("running".equals(state) && liveCounterAvailable) {
      status.generatedCount = generator.totalGeneratedPopulation.get();
    } else {
      status.generatedCount = generatedCount;
    }

    LogCaptureStream capture = logCapture;
    if (capture != null) {
      status.logTail = capture.tail();
      if ("running".equals(state) && capture.parsedTotal() >= 0) {
        status.generatedCount = capture.parsedTotal();
      }
    }
    status.exportFailureCounts = ExportFailureTracker.snapshot();
    status.htmlMissingReason = inferHtmlMissingReason(status);
    status.exportPartialWarning = buildExportPartialWarning(status);
    return status;
  }

  private static String buildExportPartialWarning(GenerationStatus status) {
    if (status.exportFailureCounts == null || status.exportFailureCounts.isEmpty()) {
      return null;
    }
    if (!"completed".equals(status.state)) {
      return null;
    }
    StringBuilder message = new StringBuilder("Exportação parcial: ");
    boolean first = true;
    for (Map.Entry<String, Integer> entry : status.exportFailureCounts.entrySet()) {
      if (entry.getValue() == null || entry.getValue() <= 0) {
        continue;
      }
      if (!first) {
        message.append("; ");
      }
      message.append(formatExportLabel(entry.getKey()))
          .append(" falhou para ")
          .append(entry.getValue())
          .append(" paciente(s)");
      first = false;
    }
    if (first) {
      return null;
    }
    message.append(". Outros formatos podem estar incompletos.");
    return message.toString();
  }

  private static String formatExportLabel(String formatKey) {
    switch (formatKey) {
      case "csv":
        return "CSV";
      case "html":
        return "HTML";
      case "fhir":
        return "FHIR R4";
      case "json":
        return "JSON";
      case "ccda":
        return "CCDA";
      default:
        return formatKey.toUpperCase();
    }
  }

  private static String inferHtmlMissingReason(GenerationStatus status) {
    if (!status.htmlExportEnabled || status.htmlIndexPath != null) {
      return null;
    }
    if (!"completed".equals(status.state)) {
      return null;
    }
    if (status.logTail != null) {
      for (String line : status.logTail) {
        if (line != null && line.contains("gate (exclude)")
            && (line.contains("exported=0") || line.contains("conforming=0,0%"))) {
          return "HTML solicitado, mas nenhum paciente conforme foi exportado (gate exclude: exported=0).";
        }
      }
    }
    if (status.exportFailureCounts != null && status.exportFailureCounts.getOrDefault("csv", 0) > 0) {
      int csvFailures = status.exportFailureCounts.get("csv");
      return "HTML solicitado, mas a exportação CSV falhou para "
          + csvFailures + " paciente(s) antes do HTML ser composto.";
    }
    return "HTML solicitado, mas nenhum paciente foi exportado para compor o index.html.";
  }

  /**
   * Reports whether a generation job is currently active.
   *
   * @return true when a generation job is active
   */
  public boolean isRunning() {
    return "running".equals(state);
  }

  /**
   * Test hook: simulate running state for concurrent job tests.
   */
  void markRunningForTest() {
    synchronized (lock) {
      state = "running";
      currentJobId = "test-job";
    }
  }

  /**
   * Test hook: reset manager state.
   */
  void resetForTest() {
    synchronized (lock) {
      state = "idle";
      currentJobId = null;
      errorMessage = null;
      logCapture = null;
      aiEnrichmentRequested = false;
      plausibilityReportPresent = false;
      plausibilityReportPath = null;
      currentJobStartedAtMs = 0L;
      trajectorySummary = null;
      activeGenerator.set(null);
    }
  }
}
