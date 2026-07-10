(function () {
  const form = document.getElementById("generation-form");
  const submitBtn = document.getElementById("submit-btn");
  const clientErrors = document.getElementById("client-errors");
  const statusPanel = document.getElementById("status-panel");
  const statusState = document.getElementById("status-state");
  const statusProgress = document.getElementById("status-progress");
  const statusError = document.getElementById("status-error");
  const statusLinks = document.getElementById("status-links");
  const logTail = document.getElementById("log-tail");
  const gateModeGroup = document.getElementById("gate-mode-group");
  const trajectorySection = document.getElementById("trajectory-section");
  const trajectorySectionHelp = document.getElementById("trajectory-section-help");
  const targetCondition = document.getElementById("targetCondition");
  const presetBtn = document.getElementById("preset-breast-cancer");
  const breastCancerHelp = document.getElementById("breast-cancer-help");
  const exportOptionsContainer = document.getElementById("export-options");
  const aiEnrichment = document.getElementById("aiEnrichment");
  const aiFields = document.getElementById("ai-fields");
  const aiProvider = document.getElementById("aiProvider");
  const aiModel = document.getElementById("aiModel");
  const aiHelp = document.getElementById("ai-help");
  const pathwayFocus = document.getElementById("pathwayFocus");
  const htmlPathwayMode = document.getElementById("htmlPathwayMode");
  const moduleProfile = document.getElementById("moduleProfile");
  const trajectoryMode = document.getElementById("trajectoryMode");
  const simulationWindow = document.getElementById("simulationWindow");
  const pathwayArchetype = document.getElementById("pathwayArchetype");

  const aiApiKeyLabel = document.getElementById("aiApiKeyLabel");

  let pollTimer = null;
  let uiOptions = null;

  function populateSelect(selectEl, options, defaultValue) {
    selectEl.innerHTML = "";
    options.forEach(function (opt) {
      const option = document.createElement("option");
      option.value = opt.value;
      option.textContent = opt.label;
      selectEl.appendChild(option);
    });
    if (defaultValue) {
      selectEl.value = defaultValue;
    }
  }

  function applyFocusedTrajectoryPreset(preset) {
    if (!preset) {
      return;
    }
    pathwayFocus.checked = !!preset.pathwayFocus;
    htmlPathwayMode.value = preset.htmlPathwayMode || "auto";
    moduleProfile.value = preset.moduleProfile || "full";
    trajectoryMode.value = preset.trajectoryMode || "lifespan";
    simulationWindow.value = preset.simulationWindow || "full_lifespan";
    if (preset.pathwayArchetype) {
      pathwayArchetype.value = preset.pathwayArchetype;
    }
  }

  function populateAiModels(providerId) {
    aiModel.innerHTML = "";
    if (!uiOptions || !uiOptions.aiEnrichment) {
      return;
    }
    const provider = uiOptions.aiEnrichment.providers.find(function (p) {
      return p.id === providerId;
    });
    if (!provider) {
      return;
    }
    provider.models.forEach(function (model) {
      const option = document.createElement("option");
      option.value = model.id;
      option.textContent = model.label;
      aiModel.appendChild(option);
    });
    if (provider.defaultModel) {
      aiModel.value = provider.defaultModel;
    }
  }

  function updateAiProviderUi() {
    if (!uiOptions || !uiOptions.aiEnrichment) {
      return;
    }
    const provider = uiOptions.aiEnrichment.providers.find(function (p) {
      return p.id === aiProvider.value;
    });
    if (!provider) {
      return;
    }
    populateAiModels(provider.id);
    if (provider.apiKeyHint) {
      aiApiKeyLabel.textContent = provider.apiKeyHint + " (BYOK — não é armazenada)";
    }
    let help = uiOptions.aiEnrichment.helpText;
    if (provider.providerHelpText) {
      help += " " + provider.providerHelpText;
    }
    aiHelp.textContent = help;
  }

  function updateAiVisibility() {
    const enabled = aiEnrichment.checked;
    aiFields.classList.toggle("hidden", !enabled);
    if (enabled && !document.getElementById("brProfile").checked) {
      document.getElementById("brProfile").checked = true;
    }
  }

  aiEnrichment.addEventListener("change", updateAiVisibility);
  aiProvider.addEventListener("change", updateAiProviderUi);

  async function loadOptions() {
    const response = await fetch("/api/config/options");
    uiOptions = await response.json();
    document.getElementById("seed").value = uiOptions.defaultSeed;
    document.getElementById("population").value = uiOptions.defaultPopulation;

    uiOptions.supportedConditions.forEach(function (key) {
      const option = document.createElement("option");
      option.value = key;
      option.textContent = key;
      targetCondition.appendChild(option);
    });

    if (uiOptions.breastCancerPreset) {
      breastCancerHelp.textContent = uiOptions.breastCancerPreset.helpText;
    }

    if (uiOptions.trajectory) {
      trajectorySectionHelp.textContent = uiOptions.trajectory.sectionHelpText;
      populateSelect(htmlPathwayMode, uiOptions.trajectory.htmlPathwayModes,
          uiOptions.trajectory.defaultHtmlPathwayMode);
      populateSelect(moduleProfile, uiOptions.trajectory.moduleProfiles,
          uiOptions.trajectory.defaultModuleProfile);
      populateSelect(trajectoryMode, uiOptions.trajectory.trajectoryModes,
          uiOptions.trajectory.defaultTrajectoryMode);
      populateSelect(simulationWindow, uiOptions.trajectory.simulationWindows,
          uiOptions.trajectory.defaultSimulationWindow);
      populateSelect(pathwayArchetype, uiOptions.trajectory.pathwayArchetypes,
          uiOptions.trajectory.defaultPathwayArchetype);
      pathwayFocus.checked = uiOptions.trajectory.defaultPathwayFocus;
    }

    uiOptions.exportOptions.forEach(function (opt) {
      const label = document.createElement("label");
      label.className = "checkbox";
      const input = document.createElement("input");
      input.type = "checkbox";
      input.id = opt.field;
      input.name = opt.field;
      if (opt.field === "exportFhir") {
        input.checked = true;
      }
      label.appendChild(input);
      label.appendChild(document.createTextNode(" " + opt.label));
      exportOptionsContainer.appendChild(label);
    });

    if (uiOptions.aiEnrichment) {
      aiHelp.textContent = uiOptions.aiEnrichment.helpText;
      document.getElementById("population").max = uiOptions.aiEnrichment.maxPatients;
      uiOptions.aiEnrichment.providers.forEach(function (p) {
        const option = document.createElement("option");
        option.value = p.id;
        option.textContent = p.label;
        aiProvider.appendChild(option);
      });
      updateAiProviderUi();
    }
    updateGateVisibility();
    updateAiVisibility();
  }

  function updateGateVisibility() {
    const hasCondition = targetCondition.value !== "";
    gateModeGroup.classList.toggle("hidden", !hasCondition);
    trajectorySection.classList.toggle("hidden", !hasCondition);
    presetBtn.classList.toggle("hidden", targetCondition.value !== "breast_cancer");
  }

  targetCondition.addEventListener("change", updateGateVisibility);

  presetBtn.addEventListener("click", function () {
    if (!uiOptions || !uiOptions.breastCancerPreset) {
      return;
    }
    document.getElementById("gender").value = uiOptions.breastCancerPreset.gender;
    document.getElementById("minAge").value = uiOptions.breastCancerPreset.minAge;
    document.getElementById("maxAge").value = uiOptions.breastCancerPreset.maxAge;
    document.getElementById("brProfile").checked = true;
    targetCondition.value = "breast_cancer";
    applyFocusedTrajectoryPreset(uiOptions.breastCancerPreset.focusedTrajectory);
    updateGateVisibility();
  });

  function validateClient() {
    const errors = [];
    const population = parseInt(document.getElementById("population").value, 10);
    const minAgeVal = document.getElementById("minAge").value;
    const maxAgeVal = document.getElementById("maxAge").value;
    if (isNaN(population) || population < 1) {
      errors.push("População deve ser ≥ 1.");
    }
    if (minAgeVal && maxAgeVal && parseInt(minAgeVal, 10) > parseInt(maxAgeVal, 10)) {
      errors.push("Idade mínima não pode ser maior que a máxima.");
    }
    if (aiEnrichment.checked) {
      if (!document.getElementById("aiApiKey").value.trim()) {
        errors.push("Informe a API key para enriquecimento por IA.");
      }
      if (uiOptions && uiOptions.aiEnrichment && population > uiOptions.aiEnrichment.maxPatients) {
        errors.push("Com IA ativa, população máxima é " + uiOptions.aiEnrichment.maxPatients + ".");
      }
    }
    const hasCondition = targetCondition.value !== "";
    const needsTarget = pathwayFocus.checked
        || moduleProfile.value === "pathway_minimal"
        || trajectoryMode.value === "episodic";
    if (needsTarget && !hasCondition) {
      errors.push("Trajetória focada requer condição clínica alvo.");
    }
    if (trajectoryMode.value === "episodic" && targetCondition.value !== "breast_cancer") {
      errors.push("Modo episodic suporta apenas breast_cancer no MVP.");
    }
    if (simulationWindow.value && simulationWindow.value !== "full_lifespan") {
      if (!minAgeVal || !maxAgeVal) {
        errors.push("Janela pre_onset_years requer idade mínima e máxima.");
      } else {
        const match = /^pre_onset_years:(\d+)$/.exec(simulationWindow.value);
        if (match && parseInt(match[1], 10) >= parseInt(minAgeVal, 10)) {
          errors.push("pre_onset_years deve ser menor que a idade mínima.");
        }
      }
    }
    if (errors.length) {
      clientErrors.textContent = errors.join(" ");
      clientErrors.classList.remove("hidden");
      return false;
    }
    clientErrors.classList.add("hidden");
    return true;
  }

  function buildPayload() {
    const payload = {
      seed: parseInt(document.getElementById("seed").value, 10),
      population: parseInt(document.getElementById("population").value, 10),
      gender: document.getElementById("gender").value,
      brProfile: document.getElementById("brProfile").checked,
      targetCondition: targetCondition.value,
      gateMode: document.getElementById("gateMode").value,
      exportFhir: document.getElementById("exportFhir").checked,
      exportCsv: document.getElementById("exportCsv").checked,
      exportHtml: document.getElementById("exportHtml").checked,
      aiEnrichment: aiEnrichment.checked,
      aiProvider: aiProvider.value,
      aiModel: aiModel.value,
      aiApiKey: document.getElementById("aiApiKey").value,
      pathwayFocus: pathwayFocus.checked,
      htmlPathwayMode: htmlPathwayMode.value,
      moduleProfile: moduleProfile.value,
      trajectoryMode: trajectoryMode.value,
      simulationWindow: simulationWindow.value,
      pathwayArchetype: pathwayArchetype.value
    };
    const minAge = document.getElementById("minAge").value;
    const maxAge = document.getElementById("maxAge").value;
    if (minAge !== "" && maxAge !== "") {
      payload.minAge = parseInt(minAge, 10);
      payload.maxAge = parseInt(maxAge, 10);
    }
    return payload;
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  async function pollStatus() {
    const response = await fetch("/api/generate/status");
    const status = await response.json();
    statusPanel.classList.remove("hidden");
    statusState.textContent = status.state;
    let progressText = status.requestedPopulation
      ? status.generatedCount + " / " + status.requestedPopulation
      : "—";
    if (status.phase === "AI_ENRICHMENT" && status.aiEnrichmentTotal > 0) {
      progressText += " | IA: " + status.aiEnrichedCount + " / " + status.aiEnrichmentTotal;
    }
    statusProgress.textContent = progressText;
    logTail.textContent = (status.logTail || []).join("\n");

    statusError.classList.add("hidden");
    statusLinks.classList.add("hidden");

    if (status.state === "failed") {
      statusError.textContent = status.errorMessage || "Falha na geração.";
      statusError.classList.remove("hidden");
      submitBtn.disabled = false;
      stopPolling();
      return;
    }

    if (status.state === "completed") {
      statusLinks.classList.remove("hidden");
      const outputLink = document.getElementById("link-output");
      outputLink.textContent = status.outputDirectory || "./output/";
      outputLink.href = "file:///" + (status.outputDirectory || "./output/").replace(/\\/g, "/");

      document.getElementById("manifest-line").classList.toggle("hidden", !status.manifestPresent);

      const trajectoryLine = document.getElementById("trajectory-summary-line");
      if (status.trajectorySummary) {
        trajectoryLine.classList.remove("hidden");
        document.getElementById("trajectory-summary").textContent = status.trajectorySummary;
      } else {
        trajectoryLine.classList.add("hidden");
      }

      const plausibilityLine = document.getElementById("plausibility-line");
      if (status.plausibilityReportPresent && status.plausibilityReportPath) {
        plausibilityLine.classList.remove("hidden");
        const plausibilityLink = document.getElementById("link-plausibility");
        plausibilityLink.href =
          "file:///" + encodeURI(status.plausibilityReportPath.replace(/\\/g, "/"));
      } else {
        plausibilityLine.classList.add("hidden");
      }

      const htmlLine = document.getElementById("html-line");
      const htmlMissingLine = document.getElementById("html-missing-line");
      const exportPartialLine = document.getElementById("export-partial-line");
      if (status.htmlExportEnabled && status.htmlIndexPath) {
        htmlLine.classList.remove("hidden");
        const htmlLink = document.getElementById("link-html");
        htmlLink.href = "file:///" + status.htmlIndexPath.replace(/\\/g, "/");
        htmlMissingLine.classList.add("hidden");
      } else {
        htmlLine.classList.add("hidden");
        if (status.htmlExportEnabled && status.state === "completed") {
          htmlMissingLine.textContent = status.htmlMissingReason
            || "HTML solicitado, mas nenhum index.html foi gerado.";
          htmlMissingLine.classList.remove("hidden");
        } else {
          htmlMissingLine.classList.add("hidden");
        }
      }

      if (status.exportPartialWarning) {
        exportPartialLine.textContent = status.exportPartialWarning;
        exportPartialLine.classList.remove("hidden");
      } else {
        exportPartialLine.classList.add("hidden");
      }

      submitBtn.disabled = false;
      stopPolling();
    }
  }

  form.addEventListener("submit", async function (event) {
    event.preventDefault();
    if (!validateClient()) {
      return;
    }
    submitBtn.disabled = true;
    stopPolling();

    const response = await fetch("/api/generate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(buildPayload())
    });

    if (response.status === 409) {
      const body = await response.json();
      clientErrors.textContent = body.error || "Geração em andamento.";
      clientErrors.classList.remove("hidden");
      submitBtn.disabled = false;
      return;
    }

    if (!response.ok) {
      const body = await response.json();
      clientErrors.textContent = body.error || "Parâmetros inválidos.";
      clientErrors.classList.remove("hidden");
      submitBtn.disabled = false;
      return;
    }

    clientErrors.classList.add("hidden");
    statusPanel.classList.remove("hidden");
    pollTimer = setInterval(pollStatus, 2000);
    pollStatus();
  });

  loadOptions().catch(function (err) {
    clientErrors.textContent = "Não foi possível carregar opções: " + err.message;
    clientErrors.classList.remove("hidden");
  });
})();
