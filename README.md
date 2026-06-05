# taskrunner

A tiny background job processor I threw together. Submit work over HTTP,
it gets queued, worker threads pick it up, and failed jobs get retried
with exponential backoff. Status of any job is a single GET away.

Stack: Java 17 + Spring Boot 3. No database — jobs live in memory. If you
need them to survive restarts, swap `JobStore` for a JPA repo and you're
set.

## Run it

```bash
mvn spring-boot:run
```

App boots on **http://localhost:8080**.

Or build a jar and run it:

```bash
mvn clean package
java -jar target/taskrunner-app-1.0.0.jar
```

## Endpoints

| Method | URL                                           | What it does                  |
|--------|-----------------------------------------------|-------------------------------|
| GET    | http://localhost:8080/                        | service info + handler list   |
| GET    | http://localhost:8080/health                  | health + queue depth          |
| POST   | http://localhost:8080/api/jobs                | submit a job                  |
| GET    | http://localhost:8080/api/jobs/{id}           | check a job's status          |
| GET    | http://localhost:8080/api/jobs?limit=25       | recent jobs                   |

## Try it

Submit an echo job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"echo","payload":{"msg":"hello world"}}'
```

Response:

```json
{
  "id": "b6d3...",
  "status": "QUEUED",
  "statusUrl": "http://localhost:8080/api/jobs/b6d3..."
}
```

Then poll:

```bash
curl http://localhost:8080/api/jobs/b6d3...
```

### See the retry logic in action

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"flaky","payload":{"failRate":0.8}}'
```

Watch the `attempts` counter climb each time you poll. After 3 failed
attempts it's marked `FAILED`; otherwise eventually `SUCCEEDED`.

### Slow job (good for watching the queue back up)

```bash
for i in $(seq 1 10); do
  curl -s -X POST http://localhost:8080/api/jobs \
    -H "Content-Type: application/json" \
    -d '{"type":"sleep","payload":{"ms":2000}}' >/dev/null
done
curl http://localhost:8080/api/jobs
```

## Config knobs

In `src/main/resources/application.properties`:

- `server.port=8080` — change the listen port
- `taskrunner.workers=4` — concurrent workers
- `taskrunner.max-retries=3` — give-up threshold
- `taskrunner.retry-backoff-ms=500` — base backoff (doubles each retry)

## Adding your own job type

Implement `JobHandler`, drop `@Component` on it, done:

```java
@Component
public class EmailHandler implements JobHandler {
    public String type() { return "send-email"; }
    public Object handle(Job job) {
        // ... do the thing
        return "sent";
    }
}
```

Then POST `{"type":"send-email","payload":{...}}`.

## Deploy

Any host that runs a JVM is fine. I usually do:

```bash
mvn clean package
scp target/taskrunner-app-1.0.0.jar user@server:/opt/taskrunner/
# on the server:
java -jar /opt/taskrunner/taskrunner-app-1.0.0.jar
```

Stick it behind nginx or just open port 8080 if you don't care.

For Docker, a 5-line Dockerfile does it:

```dockerfile
FROM eclipse-temurin:17-jre
COPY target/taskrunner-app-1.0.0.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```
