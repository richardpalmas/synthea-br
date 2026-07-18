---
project_name: 'synthea'
user_name: 'Boss'
date: '2026-06-29'
sections_completed:
  - technology_stack
  - language_rules
  - framework_rules
  - testing_rules
  - quality_rules
  - workflow_rules
  - anti_patterns
status: complete
rule_count: 72
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- **Java**: 17 mínimo (`sourceCompatibility`); CI testa JDK 17 (testes completos) e JDK 25 (compile only)
- **Build**: Gradle 9.2.1, Shadow plugin 8.3.9 — `./gradlew check` roda testes + checkstyle + JaCoCo
- **Entry point**: `App` na raiz do source set (sem pacote) — exceção intencional
- **Pacote base**: `org.mitre.synthea.*`
- **FHIR**: HAPI FHIR 6.1.0 — DSTU2, STU3, R4 com validação; não misturar APIs de versões diferentes no mesmo exportador
- **Serialização**: Gson 2.9.0 (módulos JSON), SnakeYAML 1.33, Jackson CSV 2.13.4
- **Templates**: FreeMarker 2.3.31 para exportação C-CDA (`src/main/resources/templates/ccda/`)
- **Testes**: JUnit 4.13.2 (não JUnit 5), Mockito 4.7.0, PowerMock 2.0.9, WireMock 2.33.2
- **Qualidade**: Checkstyle 8.4 (Google Java Style, max 100 chars), JaCoCo 0.8.7 (cobre apenas `org.mitre.*`)
- **JARs locais**: `lib/sbscl/`, `lib/custom/` — não publicados no Maven; não substituir por dependências remotas sem validação
- **Log4j**: versão estritamente 2.17.0 — não introduzir dependências transitivas com versões vulneráveis
- **Config padrão**: `src/main/resources/synthea.properties` — override via flag `-c` no CLI

## Critical Implementation Rules

### Language-Specific Rules

- **Pacotes**: todo código novo em `org.mitre.synthea.*`; nunca criar classes de produção fora deste namespace (exceto `App` na raiz)
- **Imports**: explícitos apenas — star imports violam Checkstyle (`AvoidStarImport`)
- **Interfaces**: prefixo `I` para contratos de comportamento (`IProviderFinder`, `IPayerAdjustment`)
- **Utilitários**: classes helper como `abstract` com métodos `static` — não instanciar
- **Config**: ler propriedades via `Config.get("chave")`; novas flags vão em `synthea.properties` com documentação inline
- **Serialização**: Gson para módulos JSON; não misturar com Jackson no mesmo fluxo de parsing de módulos
- **Checked exceptions**: propagar `IOException` em I/O de arquivos; não engolir silenciosamente
- **Generics**: usar `@SuppressWarnings` apenas quando unavoidable — padrão existente no codebase
- **Recursos estáticos**: carregar via `Class.getResourceAsStream("/path")` ou `Config.load(File)` — paths relativos a `src/main/resources/`
- **Módulos Java**: estender `org.mitre.synthea.engine.Module` ou implementar padrões em `org.mitre.synthea.modules.*`
- **Dados de referência**: CSV/JSON/YAML em `src/main/resources/` — não hardcodar dados demográficos ou clínicos no Java

### Framework-Specific Rules

**Generic Module Framework (GMF 2.0):**
- Módulos de doença preferencialmente em JSON (`src/main/resources/modules/`) — seguir [GMF wiki](https://github.com/synthetichealth/synthea/wiki/Generic-Module-Framework)
- Módulos JSON são **compartilhados** entre pacientes — clonar `State` antes de executar; nunca mutar o master module
- JSONPath em módulos usa Gson provider — expressões devem ser compatíveis com `GsonJsonProvider`
- Módulos Java customizados: registrar em `Module.loadModules()` ou carregar via `-d` (diretório local)

**Engine & Simulação:**
- `Person` é a entidade central — toda lógica clínica opera sobre `person.attributes` e `person.record`
- `HealthRecord` (em `world.concepts`) é o modelo interno — exportadores traduzem para FHIR/C-CDA/CSV
- Behaviors plugáveis via interfaces em `world.agents.behaviors.*` (provider finder, plan finder, eligibility)
- Seeds determinísticos — preservar reprodutibilidade ao alterar lógica de random

**Exportação:**
- Novos exportadores: estender `Exporter` ou `RIFExporter` conforme o formato
- Manter exportadores FHIR em classes separadas por versão (DSTU2/STU3/R4) — não unificar APIs HAPI
- C-CDA: templates FreeMarker em `src/main/resources/templates/ccda/` — não gerar XML inline no Java
- Ativar exportações via `synthea.properties` (`exporter.*`) — não hardcodar flags de export
- Flexporter (`export.flexporter`) para transformações FHIR pós-geração — não modificar exportadores base para customizações pontuais

### Testing Rules

- **Framework**: JUnit 4 apenas — não introduzir JUnit 5 sem migração explícita do projeto
- **Localização**: testes em `src/test/java/org/mitre/synthea/` espelhando o pacote da classe testada
- **Nomenclatura**: `{ClassName}Test.java` com métodos `test*` ou nomes descritivos anotados com `@Test`
- **Asserts**: preferir static imports de `org.junit.Assert` — padrão existente no codebase
- **Fixtures**: propriedades/dados de teste em `src/test/resources/` — não embutir paths absolutos
- **Helpers**: reutilizar `TestHelper`, `ParallelTestingService` para setup de `Person`/`Generator`
- **Exportadores FHIR**: validar output com HAPI `ValidationResult` — não apenas assert de string
- **Config em testes**: carregar via `Config.load(file)` com properties de teste; restaurar estado se mutar global
- **Módulos compartilhados**: resetar/clonar estado de `Module` entre testes que mutam states
- **Execução**: `./gradlew check` deve passar antes de PR — inclui testes + checkstyle + JaCoCo
- **Memória**: testes pesados já configurados com 6 GB heap — não reduzir sem motivo

### Code Quality & Style Rules

- **Checkstyle**: Google Java Style via `config/checkstyle/checkstyle.xml` — severidade warning, mas `./gradlew check` falha se violações forem tratadas como erros
- **Linha**: máximo 100 caracteres (exceto package/import/URLs)
- **Indentação**: espaços, nunca tabs; sem trailing whitespace
- **Imports**: um por linha, sem wildcards; ordem: java → javax → terceiros → org.mitre
- **Chaves**: obrigatórias em if/else/for/while — estilo K&R do Google guide
- **Estrutura de pacotes**: respeitar domínios existentes (`engine`, `modules`, `export`, `helpers`, `world`, `identity`)
- **Classes**: uma top-level class por arquivo (`OneTopLevelClass`)
- **Métodos**: camelCase; nomes descritivos — evitar abreviações não usadas no codebase
- **Javadoc**: obrigatório em APIs públicas novas — `@param`, `@return`, `@throws` quando aplicável
- **Constantes**: `UPPER_SNAKE_CASE` para `static final` (ex.: `GMF_VERSION`)
- **Empty catch**: variável deve se chamar `expected` se intencional (`EmptyCatchBlock`)

### Development Workflow Rules

- **Build local**: `./gradlew check` antes de abrir PR — cobre compile, testes, checkstyle e JaCoCo
- **CI**: GitHub Actions valida JDK 17 (testes completos) e JDK 25 (compile only) — código deve compilar em ambos
- **Branch principal**: `master` — deploy automático ao merge
- **Dependências**: declarar em `build.gradle` com versão explícita — não usar `+` ou ranges
- **Novas propriedades**: adicionar em `synthea.properties` com comentário explicativo — documentar no wiki se user-facing
- **Módulos JSON**: testar com `./run_synthea` ou testes de integração — validar com Graphviz (`./gradlew graphviz`) se alterar fluxos
- **Artefatos gerados**: output de pacientes vai para `output/` (gitignored) — nunca commitar dados sintéticos gerados
- **JARs locais**: mudanças em `lib/` requerem justificativa — preferir dependências Maven quando possível
- **Documentação**: alterações de comportamento user-facing devem atualizar wiki ou README

### Critical Don't-Miss Rules

**NUNCA:**
- Mutar states/modules JSON compartilhados em runtime — sempre clonar antes de executar por `Person`
- Misturar `org.hl7.fhir.dstu2`, `stu3` e `r4` model classes no mesmo exportador
- Commitar arquivos em `output/` ou dados de pacientes gerados
- Adicionar dependências sem versão explícita ou com log4j < 2.17.0
- Criar módulos de doença só em Java quando JSON (GMF) seria suficiente
- Alterar `person.record` diretamente nos exportadores — exportadores leem, módulos escrevem

**SEMPRE:**
- Validar módulos JSON novos contra GMF 2.0 e testar com seed fixo para reprodutibilidade
- Adicionar testes ao alterar lógica de exportação FHIR — validar com HAPI validator
- Verificar impacto em todos os exportadores ao alterar `HealthRecord` ou `Person`
- Rodar `./gradlew check` localmente — CI em JDK 17 é o gate real de qualidade
- Documentar novas properties em `synthea.properties` com comentário

**Edge Cases:**
- `Module.modules` é static e compartilhado — cuidado com estado em testes paralelos
- Physiology simulation usa JAR local (`lib/sbscl/`) — não disponível via Maven
- GraalVM JS embutido para Graphviz fallback — não assumir Graphviz instalado no ambiente
- `identity` module para populações com variantes — não confundir com `Person` attributes simples

---

## Usage Guidelines

**For AI Agents:**

- Read this file before implementing any code
- Follow ALL rules exactly as documented
- When in doubt, prefer the more restrictive option
- Update this file if new patterns emerge

**For Humans:**

- Keep this file lean and focused on agent needs
- Update when technology stack changes
- Review quarterly for outdated rules
- Remove rules that become obvious over time

Last Updated: 2026-06-29
