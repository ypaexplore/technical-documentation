# Prompt Engineering Guide for Technical Leaders & Product Owners

> A practical guide to writing effective prompts for any LLM — and using that skill to build AI-powered products.

---

## Table of Contents

1. [Part 1 — Prompt Engineering Fundamentals](#part-1--prompt-engineering-fundamentals)
   - [The 5 Layers of a Good Prompt](#the-5-layers-of-a-good-prompt)
   - [Key Principles](#key-principles)
   - [Prompt Template](#prompt-template)
2. [Part 2 — Building Products as a Technical Leader → Product Owner](#part-2--building-products-as-a-technical-leader--product-owner)
   - [Phase 1 — Discover](#phase-1--discover-use-llm-as-a-sparring-partner)
   - [Phase 2 — Define](#phase-2--define-structure-the-product)
   - [Phase 3 — Build](#phase-3--build-use-prompts-inside-the-product)
   - [Phase 4 — Measure](#phase-4--measure-eval-driven-product-iteration)
3. [Applied Example — Settlement System](#applied-example--settlement-system)
4. [Quick Reference Cheatsheet](#quick-reference-cheatsheet)

---

## Part 1 — Prompt Engineering Fundamentals

### The 5 Layers of a Good Prompt

Every well-crafted prompt is built from five layers. The more precise each layer, the less the model has to guess.

```
┌─────────────────────────────────────────────────────────────┐
│                   ANATOMY OF A GOOD PROMPT                  │
├──────────────────────┬──────────────────────────────────────┤
│  Layer               │  What it does                        │
├──────────────────────┼──────────────────────────────────────┤
│  1. Role / Persona   │  Who the model should act as         │
│  2. Context          │  Background the model needs          │
│  3. Task             │  What to do, clearly stated          │
│  4. Output Format    │  Length, structure, tone             │
│  5. Examples         │  Show, don't just tell (few-shot)    │
└──────────────────────┴──────────────────────────────────────┘
```

---

### Key Principles

#### 1. Be positive, not vague
Instead of:
> ❌ "Don't be too long"

Write:
> ✅ "Respond in exactly 3 bullet points, maximum 2 lines each"

LLMs handle explicit, positive instructions much better than vague constraints.

#### 2. Assign a role
Instead of:
> ❌ (blank system prompt)

Write:
> ✅ "You are a senior settlement operations analyst at a European investment bank. You specialise in SWIFT message processing and trade lifecycle management."

A precise role anchors tone, vocabulary, and reasoning depth.

#### 3. Use structured tags for complex prompts
For multi-part prompts — especially in agentic or API contexts — wrap sections in XML-style tags:

```xml
<role>You are a senior settlement operations analyst at a European bank.</role>

<context>
The system processes MT541 (receive against payment) instructions via RabbitMQ.
A message has been stuck in the COMPLIANCE step for 47 minutes.
Error code: HOLD-AML-002.
</context>

<task>
Diagnose the root cause and suggest the most likely resolution steps.
Think step by step before answering.
</task>

<output_format>
- Root cause (1–2 sentences)
- Recommended actions (numbered list, max 4 steps)
- Risk level: LOW / MEDIUM / HIGH
</output_format>
```

Claude and other Anthropic models respond especially well to this structure.

#### 4. Chain of thought for reasoning tasks
Add this phrase to unlock deeper reasoning:

> "Think step by step before answering."

This is particularly powerful for diagnosis, debugging, and multi-step decision-making tasks — exactly the kind of work in settlement operations.

#### 5. Use constraints deliberately
| Type | Example |
|------|---------|
| **Exclusion** | "Do not suggest actions that bypass compliance holds." |
| **Scope** | "Only consider MT54x message types." |
| **Format** | "Always include a confidence score (LOW/MEDIUM/HIGH)." |
| **Length** | "Keep your response under 150 words." |

#### 6. Iterate like code
Treat prompts like source code:
- Version them (v1, v2, v3)
- A/B test them
- Log inputs and outputs
- Refactor when you find edge cases

---

### Prompt Template

Copy and adapt this template for any use case:

```
[ROLE]
You are a [title] at [company/context]. You specialise in [domain].

[CONTEXT]
[Relevant background, data, system state, or constraints the model needs.]

[TASK]
[Clear, specific instruction. Use imperative verbs: "Analyse", "Generate", "Summarise".]
Think step by step before answering.

[OUTPUT FORMAT]
Respond with:
- [Section 1]: [description]
- [Section 2]: [description]
Maximum length: [X words / Y bullet points].

[EXAMPLES]
Good example: [show what success looks like]
Bad example: [show what to avoid]
```

---

## Part 2 — Building Products as a Technical Leader → Product Owner

Your background as a technical leader is a major asset. You think in systems — backends, queues, APIs — and now you layer **product thinking** on top. The four phases below mirror both the product lifecycle and how prompts power each stage.

---

### Phase 1 — Discover (Use LLM as a sparring partner)

Use the model to accelerate user research and market understanding **before** writing a line of code.

#### User interview prep
```
You are a product researcher.
I am building a [product] for [user persona].
Generate 10 open-ended interview questions to uncover their biggest workflow pain points.
Focus on: time wasted, errors made, workarounds used.
```

#### Feedback synthesis
```
You are a product analyst.
Here are raw notes from 8 user interviews: [paste notes]
Identify the top 5 recurring pain themes.
For each theme: name it, describe it in 1 sentence, and quote 1 example from the notes.
```

#### Competitor analysis
```
You are a product strategist.
Compare [Product A] vs [Product B] across: core features, pricing model, target user, key weakness.
Format as a markdown table.
End with: "The biggest gap in the market is ___."
```

---

### Phase 2 — Define (Structure the product)

Turn discoveries into a structured backlog. This is where your technical fluency becomes a superpower — you can write user stories **and** their acceptance criteria in one pass.

#### User stories
```
You are a product owner for a [domain] product.
Given this user pain: "[pain statement]"
Write 3 user stories in the format:
"As a [user], I want [goal], so that [value]."
Then score each on RICE: Reach, Impact, Confidence, Effort (1–10 each).
```

#### Acceptance criteria
```
You are a QA engineer and product owner.
Given this user story: "[story]"
And these technical constraints: [list constraints]
Write 5 acceptance criteria in Gherkin format:
GIVEN [context] WHEN [action] THEN [outcome].
```

#### Backlog prioritization
```
You are a product manager with a backlog of 10 features.
Here is the list: [feature list]
Score each using MoSCoW: Must Have / Should Have / Could Have / Won't Have.
Justify each decision in 1 sentence.
Assume: 2-week sprint, team of 4, MVP target.
```

---

### Phase 3 — Build (Use prompts inside the product)

Here, prompts become **the product**. The system prompt you write for your AI feature is core IP — it defines the persona, guardrails, and reasoning style of your agent.

#### System prompt (AI agent persona)
```
You are a settlement diagnostic assistant at [Bank].
You help operations teams identify and resolve failed or delayed trade instructions.

CAPABILITIES:
- Analyse SWIFT MT540/541/542/543 messages
- Identify failures across: INGEST, ENRICH, COMPLIANCE, ACCOUNTING steps
- Suggest resolution actions based on error codes

CONSTRAINTS:
- Never suggest bypassing a compliance hold
- Always flag value date risk when instruction is T+0 or T-1
- If confidence is below 70%, say so and recommend escalation to a human

OUTPUT FORMAT:
1. Root cause (1–2 sentences)
2. Recommended actions (max 4 steps)
3. Risk level: LOW / MEDIUM / HIGH
4. Confidence: [0–100%]
```

#### Prompt chaining (multi-step agentic workflows)

Design prompts that pass output from one step as input to the next:

```
Step 1 — Classify:
"Read this error log. Classify the failure type as one of:
[MESSAGE_PARSE_ERROR, COMPLIANCE_HOLD, ENRICHMENT_FAILURE, ACCOUNTING_REJECT]"

Step 2 — Diagnose (using Step 1 output):
"Given failure type: {step_1_output}
And this raw error: {error_details}
What is the most likely root cause?"

Step 3 — Act (using Step 2 output):
"Given diagnosis: {step_2_output}
What is the recommended remediation action?
Only suggest actions within the operator's permissions: [list permissions]"
```

#### Guardrails for compliance-critical products
```
HARD RULES (never break these):
- Never generate or suggest an instruction that bypasses a compliance flag
- Never confirm a settlement without receiving gateway acknowledgment
- If the user asks you to force an instruction, always require confirmation:
  "This action will force instruction [ID]. Type CONFIRM to proceed."

SOFT RULES (use judgment):
- Prefer read-only diagnostic actions before write actions
- Always mention value date risk for same-day instructions
- Log the reason for every forced action
```

---

### Phase 4 — Measure (Eval-driven product iteration)

Treat your AI features with the same rigor as your Java services — measure, monitor, and iterate.

#### Prompt A/B testing framework
```
Test: Does adding "think step by step" improve diagnostic accuracy?

Variant A (control):
"Given this error log, what is the root cause?"

Variant B (treatment):
"Given this error log, think step by step and identify the root cause."

Metric: % of responses where the root cause matched the actual ticket resolution.
Sample size: 50 real error logs from production (anonymised).
```

#### Output quality evaluation
```
You are a quality evaluator for an AI settlement assistant.
Given this AI response: [response]
And this ground truth: [correct answer]
Score the response on:
- Accuracy (0–10): Does the root cause match?
- Completeness (0–10): Are all key steps included?
- Safety (0–10): Does it avoid suggesting non-compliant actions?
```

#### Production monitoring checklist
| Signal | How to measure |
|--------|---------------|
| Response accuracy | Compare to human expert decisions (sample weekly) |
| Hallucination rate | Flag responses citing non-existent error codes |
| Latency | P50/P95 response time per prompt chain step |
| User correction rate | How often ops team overrides the AI suggestion |
| Guardrail triggers | Count of refused / escalated requests per day |

---

## Applied Example — Settlement System

Here is a complete, real-world prompt combining all 5 layers for the Diagnostic Agent in a settlement infrastructure:

```xml
<role>
You are a senior settlement operations analyst at a European investment bank.
You specialise in SWIFT MT54x message processing and trade lifecycle management.
</role>

<context>
System: Java backend → RabbitMQ → Angular ops dashboard
Processing chain: INGEST → ENRICH → COMPLIANCE → ACCOUNTING → MARKET_GATEWAY

Current issue:
- Instruction ID: IST-2024-98234
- Message type: MT541 (receive against payment, equities)
- Current step: COMPLIANCE
- Time in step: 52 minutes (SLA breach at 30 minutes)
- Error code: HOLD-AML-002
- Counterparty: [MASKED]
- Value date: TODAY (T+0)
</context>

<task>
Diagnose the most likely root cause of this compliance hold.
Think step by step before answering.
Then recommend the resolution path available to a Level 2 operations user.
Do not suggest actions that require compliance officer override unless flagged as escalation.
</task>

<output_format>
1. Root cause (2 sentences max)
2. Recommended actions (numbered, max 4 steps)
3. Escalation required: YES / NO
4. Value date risk: flag if T+0 settlement is now at risk
5. Confidence: LOW / MEDIUM / HIGH
</output_format>

<examples>
Good response:
"Root cause: AML screening flagged the counterparty BIC against the OFAC watchlist update
deployed at 09:15 today. The hold code HOLD-AML-002 specifically indicates a name-match
pending manual review..."

Bad response:
"You should bypass the compliance hold and force the instruction to ACCOUNTING."
(Never suggest bypassing compliance)
</examples>
```

---

## Quick Reference Cheatsheet

```
PROMPT QUALITY CHECKLIST
─────────────────────────────────────────
□ Role defined with specific title + domain
□ Context includes all relevant state/data
□ Task uses imperative verbs (Analyse, Generate, Write)
□ "Think step by step" added for reasoning tasks
□ Output format specifies structure AND length
□ At least 1 positive example included
□ Constraints stated as "Do X" not "Don't be vague"
□ Sensitive actions require explicit confirmation step
□ Prompt versioned (v1, v2...) for iteration tracking

PROMPT ANTI-PATTERNS TO AVOID
─────────────────────────────────────────
✗ "Write a good summary" → too vague
✗ "Don't be too long" → use positive constraint instead
✗ Leaving system prompt blank → always set a role
✗ One giant paragraph → use tags/sections
✗ No examples → add at least one good/bad pair
✗ Treating prompts as static → iterate like code
```

---

## Resources

- [Anthropic Prompt Engineering Docs](https://docs.claude.ai/en/docs/build-with-claude/prompt-engineering/overview)
- [OpenAI Prompt Engineering Guide](https://platform.openai.com/docs/guides/prompt-engineering)
- [LangChain Prompt Templates](https://python.langchain.com/docs/concepts/prompt_templates/)

---

*Guide authored for technical leaders transitioning into product ownership roles, with a focus on AI-powered financial infrastructure products.*
