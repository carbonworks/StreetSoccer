# Known False Positive Patterns

This file catalogs false positive patterns observed during review pipeline runs. Include relevant examples in agent prompts to suppress them.

Update this file after each wave audit (see `review-pipeline.md` Section 9).

---

## Debt Gatekeeper

_No false positives cataloged yet. This file will be populated after Phase A is deployed and tuned._

### Template for Adding Entries

```
### DG-FP-{N}: {short description}
**Agent:** debt-gatekeeper
**Checklist item:** {which item}
**False positive:** {what the agent flagged}
**Why it's wrong:** {why this is not actually a violation}
**Suppression rule:** {what to add to the prompt to prevent this}
```

---

## Merge Analyst

_No false positives cataloged yet._

---

## Architecture Guardian (Phase B)

_Not yet deployed._

---

## Performance Sentinel (Phase B)

_Not yet deployed._

---

## Behavior Validator (Phase C)

_Not yet deployed._

---

## Spec Auditor (Phase C)

_Not yet deployed._
