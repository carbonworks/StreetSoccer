# Street Soccer — Review Pipeline Spec

This document defines the multi-agent code review pipeline that validates every work package before it merges to main. Each WP branch goes through a build gate, parallel specialized review agents, and a meta-reviewer synthesis step before the merge decision is made.

For the no-forward-debt policy that governs merge quality, see `.claude/rules/no-forward-debt.md`. For the coding agent workflow, see `CLAUDE.md` "Parallel Agent Workflow".

---

## 1. Pipeline Architecture

```
WP Branch (worktree, coding agent done)
        |
        v
  Phase 1: BUILD GATE ─── FAIL → reject, return to coding agent
        |
      PASS
        v
  Phase 2: PARALLEL REVIEW (fan-out)
    ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
    │ Agent 1  │ Agent 2  │ Agent 3  │ Agent 4  │ Agent 5  │ Agent 6  │
    │ Behavior │ Arch.    │ Debt     │ Perf.    │ Merge    │ Spec     │
    │ Validator│ Guardian │ Gatekeeper│ Sentinel │ Analyst  │ Auditor  │
    └────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘
         └──────────┴──────────┴──────────┴──────────┴──────────┘
                                    |
                                    v
  Phase 3: META-REVIEWER (synthesis)
    Deduplicate → resolve conflicts → gate decision
        |
    BLOCK → findings to coding agent (max 2 retries) → re-enter Phase 1
    APPROVE → merge to main
```

### Phase 1: Build Gate

Fast, cheap, blocking. No point running LLM agents on code that does not compile.

| Check | Command | Pass Criteria |
|-------|---------|---------------|
| Compile | `./gradlew build` | Zero errors, zero new warnings |
| Static analysis | `./gradlew ktlintCheck` | Zero violations (when integrated) |
| Custom checks | `grep -r 'getComponent(' --include='*.kt' core/src/` | Zero matches in new/modified files |
| Debt language | `grep -rn 'TODO\|FIXME\|HACK\|XXX' --include='*.kt' core/src/` | Zero matches in new/modified files |

If any check fails, the build gate rejects the branch and returns findings to the coding agent. No review agents are launched.

### Phase 2: Parallel Review

All review agents launch simultaneously. Each reads the same inputs (diff, full files, spec docs) independently. No agent sees another agent's output — this prevents the telephone game.

### Phase 3: Meta-Reviewer Synthesis

After all agents complete, the meta-reviewer:
1. Collects all findings from all agents
2. Deduplicates findings that reference the same code location for the same underlying issue
3. Resolves inter-agent conflicts per the authority hierarchy (Section 5)
4. Produces a unified report with findings sorted by severity
5. Makes the gate decision

---

## 2. Agent Roster

### Phased Rollout

| Phase | Agents | Status |
|-------|--------|--------|
| **A** | Debt Gatekeeper + Merge Analyst + Build Gate | Prompts ready |
| **B** | + Architecture Guardian + Performance Sentinel | Prompts ready — deploy after Phase A is tuned |
| **C** | + Behavior Validator + Spec Auditor | Prompts ready — deploy after Phase B stabilizes |

All 6 agent prompt templates are complete in `review/prompts/`. Phased deployment controls which agents are launched — not which prompts exist.

### Agent Summary

| # | Agent | ID Prefix | Mandate | Prompt File |
|---|-------|-----------|---------|-------------|
| 1 | Behavior Validator | BV | Verify acceptance criteria, generate unit tests | `review/prompts/behavior-validator.md` |
| 2 | Architecture Guardian | AG | Enforce `technical-architecture.md` patterns | `review/prompts/architecture-guardian.md` |
| 3 | Debt Gatekeeper | DG | Mechanically check WP Completion Checklist | `review/prompts/debt-gatekeeper.md` |
| 4 | Performance Sentinel | PS | Hot-path allocations, GC pressure, game-dev perf | `review/prompts/performance-sentinel.md` |
| 5 | Merge Analyst | MA | Predict merge conflicts, cross-file consistency | `review/prompts/merge-analyst.md` |
| 6 | Spec Auditor | SA | Code-spec alignment, edit hierarchy compliance | `review/prompts/spec-auditor.md` |

### Agents Rejected (and Why)

| Candidate | Reason |
|-----------|--------|
| Security/vulnerability scanner | Single-player offline game, no network, no auth. Negligible attack surface. Revisit if online features are added. |
| API contract/interface agent | No published API. Architecture Guardian covers internal interface adherence. |
| Style/consistency agent | Use `ktlint` and `detekt` as static tools — cheaper and more reliable than LLM for style. |
| Changelog generator | Single-dev project. `backlog.md` and `work-packages.md` already serve this purpose. |

---

## 3. Responsibility Matrix

Each concern has exactly one PRIMARY owner. SECONDARY means the agent may notice it but defers to the primary agent.

| Concern | BV | AG | DG | PS | MA | SA |
|---------|:--:|:--:|:--:|:--:|:--:|:--:|
| Acceptance criteria met | **P** | | | | | |
| Unit test generation | **P** | | | | | |
| ECS patterns (mappers, families) | | **P** | S | | | |
| Screen/overlay architecture | | **P** | | | | |
| Box2D integration patterns | | **P** | | | | |
| Resource disposal (Disposable lifecycle) | | | **P** | S | | |
| Per-frame allocations | | | S | **P** | | |
| Magic numbers / constant centralization | | | **P** | | | |
| TODO / debt language | | | **P** | | | |
| begin/end pairing (batch, ShapeRenderer) | | | **P** | | | |
| Batch state reset after Stage.draw() | | | **P** | | | |
| Compiler warnings | | | **P** | | | |
| Hot-path GC pressure | | | | **P** | | |
| Android lifecycle concerns | | | | **P** | | |
| Memory budget compliance | | | | **P** | | |
| Merge conflict prediction | | | | | **P** | |
| Cross-file registration consistency | | | | | **P** | |
| Concurrent WP dependency safety | | | | | **P** | |
| Spec-code value alignment | | | | | | **P** |
| Edit hierarchy compliance | | | | | | **P** |
| Cross-reference validity | | | | | | **P** |

**P** = PRIMARY (authoritative), **S** = SECONDARY (defers to primary)

---

## 4. Finding Schema

All agents produce findings in the same structured JSON format. See `review/schemas/finding.schema.json` for the full schema.

### Finding Fields

| Field | Required | Description |
|-------|----------|-------------|
| `agent` | Yes | Which agent produced this finding |
| `finding_id` | Yes | Unique ID: agent prefix + 3-digit number (e.g., DG-001) |
| `severity` | Yes | P0 / P1 / P2 / P3 (matches `no-forward-debt.md` severity levels) |
| `category` | Yes | Concern category (resource-management, ecs-patterns, etc.) |
| `file` | Yes | Relative path to the file |
| `line_range` | No | Line number(s) in the file |
| `rule` | No | The specific rule or checklist item being evaluated |
| `description` | Yes | What the issue is |
| `evidence` | Yes | Quoted code snippet. If you cannot quote it, do not report it. |
| `suggested_fix` | No | How to resolve the issue |
| `blocks_merge` | Yes | P0/P1 = true. P2/P3 = false. |

### Report Schema

Each agent produces a full report wrapping its findings. See `review/schemas/report.schema.json`. Key fields:

- `verdict`: APPROVE / APPROVE_WITH_ADVISORIES / BLOCK
- `findings`: Array of finding objects
- `summary`: Counts by severity, files reviewed, checklist coverage

---

## 5. Gate Mechanics

### Severity-to-Gate Mapping

| Severity | Definition | Gate Action |
|----------|-----------|-------------|
| **P0** | Crash, data loss, blocks other WPs | **Hard block** — branch cannot merge |
| **P1** | Incorrect player-visible behavior | **Hard block** — branch cannot merge |
| **P2** | Performance, code smell, minor UX | **Advisory** — merge allowed, tracked as backlog item |
| **P3** | Polish, documentation, style | **Note** — merge allowed, included in report |

### Verdict Rules

| Condition | Verdict |
|-----------|---------|
| Any P0 or P1 finding | BLOCK |
| P2 findings but no P0/P1 | APPROVE_WITH_ADVISORIES |
| Only P3 findings or no findings | APPROVE |

### Authority Hierarchy (for Conflict Resolution)

When agents disagree, the meta-reviewer resolves using this priority:

1. Build gate (hard block — does not compile = nothing else matters)
2. Debt Gatekeeper (policy compliance is non-negotiable)
3. Architecture Guardian (structural violations)
4. Behavior Validator (incorrect behavior is a bug)
5. Performance Sentinel (performance has tradeoffs)
6. Merge Analyst (integration is advisory until merge time)
7. Spec Auditor (divergence may be intentional)

---

## 6. Feedback Loop

### Retry Mechanics

| Tier | Trigger | Action | Max Retries |
|------|---------|--------|-------------|
| **Auto-fix** | Mechanical issues (missing dispose, uncentralized constant) | Meta-reviewer generates a patch, applies it, re-runs affected agents | 1 |
| **Report-and-retry** | Behavioral/architectural issues | Full findings report sent to coding agent with fix instructions; pipeline re-runs | 2 |
| **Escalate** | Ambiguous findings, spec divergence that may be intentional, or retry budget exhausted | Report presented to human developer | 0 (human decides) |

### Retry Budget

Maximum **2 full review cycles** per WP. If the coding agent cannot satisfy reviewers after 2 attempts, escalate to human. This prevents infinite loops.

### Feedback Report Format

When sending findings back to a coding agent, include:

```
## Review Findings for WP-{N}

### BLOCKING (must fix before re-review)
{list of P0 and P1 findings with file, line, evidence, and suggested fix}

### ADVISORY (tracked as backlog items, not blocking)
{list of P2 findings}

### NOTES
{list of P3 findings}

Fix the blocking issues and commit. The review pipeline will re-run automatically.
```

---

## 7. Agent Input Assembly

Each review agent receives a standardized input package. The orchestrator assembles this before launching agents.

### Common Inputs (all agents receive)

| Input | How to Obtain |
|-------|---------------|
| Full diff | `git -C {worktree} diff main...HEAD` |
| Changed file list | `git -C {worktree} diff --name-only main...HEAD` |
| Full content of each changed file | Read each file from the worktree |
| WP definition | Extract from `work-packages.md` |
| `CLAUDE.md` | Read from repo root |

### Agent-Specific Inputs

| Agent | Additional Inputs |
|-------|-------------------|
| Debt Gatekeeper | `no-forward-debt.md`, `TuningConstants.kt`, build output |
| Merge Analyst | Current `main` branch versions of touched files, `work-packages.md` (concurrent WP status) |
| Architecture Guardian | `technical-architecture.md` |
| Performance Sentinel | `testing-performance-plan.md`, `physics-and-tuning.md` |
| Behavior Validator | All spec docs from WP's "Reads" field |
| Spec Auditor | All spec docs from WP's "Reads" field, `game-design-document.md` |

---

## 8. Prompt Engineering Principles

These principles govern all review agent prompts. See individual prompt files in `review/prompts/` for implementation.

### P1: Be Specific, Not General

Bad: "Check for code quality issues."
Good: "Check that every `BitmapFont` created in the diff has a corresponding `dispose()` call in the same class's `dispose()` method or is added to a managed disposal list."

### P2: Provide the Full Rule Text

Do not paraphrase conventions. Paste checklist items verbatim from `no-forward-debt.md`. The agent evaluates against the exact text.

### P3: Require Evidence

Every finding must include a quoted code snippet from the diff. If the agent cannot quote the offending code, the finding is likely hallucinated. Discard it.

### P4: Define What "Not a Finding" Looks Like

Explicitly tell agents what to ignore:
- Pre-existing code not changed in this diff
- Patterns explicitly endorsed by the architecture document
- Implementation details the spec intentionally leaves open

### P5: Use Negative Examples

After initial runs, add false positive examples to prompts with "findings like these are false positives — do not report them."

### P6: Structured Output

Require JSON output per the finding schema. Prevents rambling prose that is hard to parse and act on.

### P7: Read Source Documents at Invocation Time

Review criteria come from version-controlled files (`no-forward-debt.md`, `technical-architecture.md`), not baked into the prompt. When the policy evolves, reviews automatically use the new version.

---

## 9. Operational Metrics

Track these per wave to tune the pipeline. Cumulative data lives in `review/metrics.json`. The human-readable dashboard is `review/dashboard.md`.

### Key Metrics

| Metric | Target | Stretch | What It Tells You |
|--------|--------|---------|-------------------|
| False positive rate per agent | < 10% | < 5% | Prompt is too aggressive |
| First-pass acceptance rate | > 60% | > 80% | Coding agents are internalizing rules |
| Post-merge escape rate (P0/P1) | 0.05/WP | 0.00/WP | Pipeline has gaps |
| Findings per review cycle | 3-8 | — | Too few = not catching enough; too many = noise |
| Retry cycles per WP | <= 1.0 avg | <= 0.5 | Whether coding agents learn from feedback |
| Review latency (p90) | < 3 min | < 2 min | Pipeline is blocking flow |
| Cost per WP reviewed | < $5 | < $2 | Sustainable cost |
| Human escalation rate | < 10% | < 5% | Pipeline generating work vs. automating it |
| Pipeline SNR | > 90% | > 95% | Signal-to-noise ratio (1 - FPR) |

### Pre-Pipeline Baseline

Established from 34 WPs across 7 waves. See `review/metrics.json` for full data.

| Baseline Metric | Value |
|----------------|-------|
| Defect escape rate | 0.85 per WP |
| P0/P1 escape rate | 0.50 per WP |
| Rework wave ratio | 29% (2 of 7 waves) |

### Kill Switch

Stop the pipeline if:
- Cost per WP > $10 for 2 consecutive waves AND no measurable improvement over baseline
- Human escalation rate > 30% for 2 consecutive waves

### Post-Wave Audit Workflow

After each wave merges (~15 minutes of manual effort):

1. Review meta-reviewer summaries. Tag each finding as TP/FP in `review/results/wave-N/audit.json`
2. Note any prompt adjustments needed
3. Update `review/metrics.json` with the wave's data
4. Update `review/examples/false-positives.md` with new FP patterns
5. Update `review/dashboard.md` with trend data
6. Every 3 waves: produce ROI report and agent value assessment

### Alerting Thresholds

| Condition | Trigger | Action |
|-----------|---------|--------|
| FPR > 15% for any agent | 2 consecutive waves | Update prompt, add negative examples |
| FPAR < 40% | 2 consecutive waves | Rules too strict, or coding prompts need review rules embedded |
| P0/P1 escape post-merge | Any occurrence | Root cause: which agent should have caught it? Update prompt |
| Agent at 0 findings | 3 consecutive waves | Evaluate retirement (check if category also has 0 escapes) |
| Avg retries > 1.5 | Any wave | Embed top review rules into coding agent prompts |
| Cost per WP > $5 | Any single WP | Check diff size; consider splitting WPs or cheaper models |

### Data Storage

```
review/
  metrics.json                    # Cumulative tracking across all waves
  dashboard.md                    # Human-readable health dashboard
  results/
    wave-N/
      WP-NN/
        cycle-1/
          build-gate.json         # Build gate output
          debt-gatekeeper.json    # Agent report (report.schema.json)
          merge-analyst.json      # Agent report
          meta-reviewer.json      # Synthesis report
          meta-reviewer.md        # Human-readable summary
          timing.json             # Latency and cost data (timing.schema.json)
        cycle-2/                  # If retry
          ...
      wave-summary.json           # Aggregated metrics (wave-summary.schema.json)
      audit.json                  # Manual TP/FP tagging
```

---

## 10. Static Analysis Complement

These static tools run in the build gate (Phase 1). They handle the mechanical subset of review concerns, leaving LLM agents for semantic analysis.

| Tool | What It Catches | Status |
|------|----------------|--------|
| `ktlint` | Code formatting, import ordering, naming | Planned |
| `detekt` | Code smells, complexity, unused code, exception handling | Planned |
| Custom: `getComponent(` grep | ECS mapper violations | Planned |
| Custom: `TODO` grep | Debt language | Planned |

### Integration Plan

1. Add `ktlint` and `detekt` Gradle plugins to `core/build.gradle.kts`
2. Add custom Gradle task `checkCodeConventions` that greps for `getComponent(` and `TODO`
3. Wire all checks into the build gate sequence
4. Findings from static tools are reported in the same finding schema as LLM agents

---

## Companion Documents

| Document | Relevance |
|----------|-----------|
| `.claude/rules/no-forward-debt.md` | The WP Completion Checklist that Agent 3 (Debt Gatekeeper) enforces |
| `technical-architecture.md` | The architecture patterns that Agent 2 (Architecture Guardian) enforces |
| `testing-performance-plan.md` | The performance budgets that Agent 4 (Performance Sentinel) validates |
| `work-packages.md` | WP definitions with acceptance criteria for Agent 1 (Behavior Validator) |
| `CLAUDE.md` | Orchestration instructions for running the pipeline |
| `review/prompts/*.md` | Individual agent prompt templates |
| `review/schemas/*.json` | JSON schemas for findings, reports, timing, and wave summaries |
| `review/metrics.json` | Cumulative metrics tracking with pre-pipeline baseline |
| `review/dashboard.md` | Human-readable pipeline health dashboard |
| `review/examples/false-positives.md` | Known false positive patterns for prompt tuning |
