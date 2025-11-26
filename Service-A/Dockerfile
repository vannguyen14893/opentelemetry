#FROM maven:3.9.6-eclipse-temurin-21 AS builder
#WORKDIR /app
#COPY pom.xml .
#COPY src ./src
##COPY ./elastic-apm-agent-1.54.0.jar elastic-apm-agent.jar
#RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
#COPY --from=builder /app/elastic-apm-agent.jar elastic-apm-agent.jar
COPY  /target/*.jar app.jar
EXPOSE 8089
#ENV JAVA_TOOL_OPTIONS="-javaagent:/app/elastic-apm-agent.jar \
#  -Delastic.apm.service_name=service-a \
#  -Delastic.apm.secret_token=fzd4MJLyPLSXZ0oUj7-hzPrtxGC8rKHhTbMKO6wVThVcRib8w5r4Jdt00BzAX-UT-shurF0vtQJLeK3qMqRnPA \
#  -Delastic.apm.application_packages=com.demo.service.a \
#  -Delastic.apm.server_url=http://host.docker.internal:8200 \
#  -Delastic.apm.environment=dev"
ENTRYPOINT ["java", "-jar", "app.jar"]