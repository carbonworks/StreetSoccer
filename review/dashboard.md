# Review Pipeline Dashboard

Updated after each wave. Data sourced from `review/metrics.json` and `review/results/`.

---

## Current Status

| Pipeline Phase | Agents Active | Status |
|----------------|--------------|--------|
| Build Gate | `./gradlew build` + static checks | Ready |
| Phase A | Debt Gatekeeper, Merge Analyst | Ready — deploy on Wave 8 |
| Phase B | Architecture Guardian, Performance Sentinel | Prompts ready — deploy after Phase A tunes |
| Phase C | Behavior Validator, Spec Auditor | Prompts ready — deploy after Phase B stabilizes |

---

## Pre-Pipeline Baseline (Waves 1-7)

Established from 34 WPs across 7 waves. 17 retroactive defects discovered from Waves 1-4, requiring 2 full rework waves (5-6) to clear.

| Metric | Baseline Value |
|--------|---------------|
| Defect escape rate | 0.85 per WP |
| P0/P1 escape rate | 0.50 per WP |
| Rework wave ratio | 29% (2 of 7 waves) |
| Feature-to-rework WP ratio | 20 feature : 14 rework |

### Defects by Severity

| Severity | Count | Rate per WP | Examples |
|----------|-------|-------------|----------|
| P0 (Critical) | 4 | 0.20 | TrajectorySystem crash, font leaks, null-unsafe accessors, double accumulation |
| P1 (Important) | 6 | 0.30 | getComponent() usage, spiral-of-death, god object, magic numbers |
| P2 (Medium) | 3 | 0.15 | Linear entity search, batch state reset, per-popup font allocation |
| P3 (Polish) | 4 | 0.20 | Log string templates, hardcoded positions, shadow heuristic, lifecycle overrides |

### Which Agents Would Have Caught Them

| Agent | Would Have Caught | Count |
|-------|-------------------|-------|
| Debt Gatekeeper | #26, #27, #28, #29, #30, #34, #37, #38 | 8 |
| Architecture Guardian | #30, #32, #40 | 3 |
| Performance Sentinel | #36, #38, #39 | 3 |
| Spec Auditor | #33, #34, #40 | 3 |
| Behavior Validator | #33 | 1 |
| Merge Analyst | (none — these were single-branch issues) | 0 |

The Debt Gatekeeper alone would have caught 8 of 17 issues (47%). Adding all agents covers 15 of 17 (88%).

---

## Wave Trends

_No pipeline-reviewed waves yet. This section will populate after Wave 8._

### Template (filled after each wave)

```
Wave N | X WPs reviewed | $X.XX total cost ($X.XX/WP)
Verdict: X APPROVE, X APPROVE_WITH_ADVISORIES, X BLOCK
FPAR: XX% | Avg retries: X.XX | Escalations: X
Post-merge escapes: X
```

### False Positive Rate by Agent

```
         DG     MA     AG     PS     BV     SA
Wave 8:  -      -      -      -      -      -
Wave 9:  -      -      -      -      -      -
Wave 10: -      -      -      -      -      -
```

### First-Pass Acceptance Rate Trend

```
Wave 8:  -
Wave 9:  -
Wave 10: -
Target:  60%    Stretch: 80%
```

### Post-Merge Escape Rate

```
Baseline (Waves 1-4): 0.85/WP
Wave 8:  -
Wave 9:  -
Wave 10: -
Target:  0.05/WP    Stretch: 0.00/WP
```

### Cost per WP

```
Wave 8:  -
Wave 9:  -
Wave 10: -
Target:  < $5.00    Stretch: < $2.00
```

### Review Latency (p90)

```
Wave 8:  -
Wave 9:  -
Wave 10: -
Target:  < 180s     Stretch: < 120s
```

---

## Agent Value Assessment

_Updated every 3 waves._

### Template

```
Agent: {name}
  Waves active: N
  Total findings: N (N TP, N FP)
  FPR: X%
  Top categories: {cat1} (N), {cat2} (N)
  Recommendation: KEEP | TUNE | MERGE | RETIRE
```

---

## Alerting Thresholds

| Condition | Threshold | Status |
|-----------|-----------|--------|
| FPR for any agent > 15% | 2 consecutive waves | Not yet triggered |
| FPAR < 40% | 2 consecutive waves | Not yet triggered |
| Cost per WP > $5 | Any single WP | Not yet triggered |
| P0/P1 escape post-merge | Any occurrence | Not yet triggered |
| Escalation rate > 20% | Any wave | Not yet triggered |
| Agent at 0 findings | 3 consecutive waves | Not yet triggered |
| Avg retries > 1.5 | Any wave | Not yet triggered |
| Latency p90 > 5 min | Any wave | Not yet triggered |

---

## Kill Switch Status

| Condition | Threshold | Current | Status |
|-----------|-----------|---------|--------|
| Cost ceiling | > $10/WP for 2 consecutive waves AND no improvement over baseline | N/A | Inactive |
| Escalation ceiling | > 30% escalation rate for 2 consecutive waves | N/A | Inactive |

---

## Prompt Adjustments Log

_Record prompt changes made in response to metrics. Tracks the feedback loop._

| Wave | Agent | Adjustment | Reason |
|------|-------|------------|--------|
| - | - | - | _No adjustments yet_ |

---

## ROI Summary

_Updated every 3 waves._

### Template

```
Pipeline Cost (Waves X-Y):
  Total: $XX.XX (N WPs x $X.XX avg)

Pipeline Value (Waves X-Y):
  Issues caught pre-merge: N (N blocking, N advisory)
  Post-merge escapes: N (vs. 0.85/WP baseline = N expected)
  Estimated rework prevented: N issues x 30 min avg = N hours
  Rework waves prevented: ~N

  ROI: (prevented rework value) / (pipeline cost)
```

---

## Companion Files

| File | Purpose |
|------|---------|
| `review/metrics.json` | Machine-readable cumulative data |
| `review/results/wave-N/wave-summary.json` | Per-wave aggregated metrics |
| `review/results/wave-N/audit.json` | Manual TP/FP tagging |
| `review/results/wave-N/WP-NN/timing.json` | Per-WP latency and cost |
| `review/examples/false-positives.md` | Known FP patterns fed into prompts |
| `review-pipeline.md` | Pipeline architecture spec |
