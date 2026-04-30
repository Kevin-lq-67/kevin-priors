# kevin-priors

Submission for the "Relevant Priors" radiology challenge.

A Spring Boot HTTP service that decides whether each prior radiology
study should be shown to a radiologist while reading a current study.

## Stack
- Java 21
- Spring Boot 3.5
- Maven
- Anthropic Claude API (planned)

## Run locally

```bash
./mvnw spring-boot:run
```

The service starts on port 8080.

## API
- `POST /predict` — see challenge spec for the request/response schema
- `GET /` — health check

## Status
Work in progress.