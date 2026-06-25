FROM maven:3.9-eclipse-temurin-21
WORKDIR /app

# Cache all Maven dependencies in a dedicated layer.
# This layer only rebuilds when pom.xml changes — source changes don't trigger a re-download.
COPY pom.xml .
RUN mvn dependency:go-offline -q --no-transfer-progress

COPY src ./src

# Default command: full suite headless against Selenium Grid.
# Overridden by docker-compose with -Dsuite=${SUITE:-full}.
CMD ["mvn", "test", "-Dbrowser=chrome", "--no-transfer-progress"]
