# Experiments

I built a Spring Boot 3.5 service for this challenge. It exposes a
single endpoint, `POST /predict`, which takes a list of patient cases
and returns one prediction per prior study indicating whether it is
relevant to the current study.

The service is hosted on Render at:
https://kevin-priors.onrender.com/predict

## Setup
The service is built and deployed with the following stack:

- Java 21, Spring Boot 3.5
- Maven
- Deployed to Render free tier via a multi-stage Dockerfile
- Uses Anthropic Claude Haiku 4.5 for the relevance decision

## Baseline: return true for everything

My first version returned `true` for every prior. The point was to
verify the API contract before adding any real logic.

| Version | Accuracy |
|---------|----------|
| All true | 20.23% (35 of 173) |

The number was a wake-up call: only 20% of priors in the public eval
are actually labeled relevant. A "return false for everything"
baseline would already score ~80%, so my real version had to beat
that to mean anything.

## What I tried

After the all-true baseline I switched to calling Claude.

For each case I send Claude the current study and the list of priors,
and ask for a JSON array of `{study_id, is_relevant}` — one entry per
prior. Body-region matching from study descriptions is something an
LLM should handle well.

A few things I had to handle along the way:
- Claude sometimes wraps the JSON in ```json ... ``` even when I tell
  it not to. I strip that before parsing.
- Claude occasionally returns fewer entries than there were priors,
  so I fill the gaps with `false` (the grader counts missing
  predictions as wrong).
- If the API call fails for any reason — auth, rate limit, network —
  I fall back to returning `false` for every prior. Given the ~80%
  false rate, this loses less than returning `true`.

## What worked

- Sending all priors for one case in a single Claude call (per-prior
  calls would have timed out — this is also what the challenge hints
  warn about).
- Using Claude Haiku 4.5. The task is short-string classification, so
  the bigger models weren't worth the extra cost.
- The `false`-default fallback. Given the ~80% false rate, "fail to
  false" is a much safer floor than "fail to true".

## What didn't work

- I initially had a billing sync issue: my account showed credits but
  the API rejected calls with `credit balance is too low` for ~30
  minutes. During that window every Claude call hit my fallback,
  which was eye-opening — it confirmed the fallback strategy actually
  matters, it isn't just paranoia.

## How I would improve this with more time

- **Caching.** Within a single request, if the same `(current_study,
  prior)` pair shows up more than once, I currently call Claude
  twice. A small map keyed by the pair would cut tokens and latency.
- **Concurrency.** Right now cases are processed sequentially.
  Parallelizing Claude calls per case would help stay under the 360s
  budget when requests have many cases.
- **Retry on transient errors.** Currently any failure goes straight
  to fallback. A single retry on 5xx / timeout would catch most blips
  before giving up.

## Notes

- Patient names and IDs are PHI. The DTOs flag them as such and
  neither the logger nor the Claude prompt ever sees them — only
  study descriptions and dates leave this server.
- The repo still has a `BaselineRelevanceService` (returns true for
  everything). I left it in as a working fallback I can flip back to
  if needed.