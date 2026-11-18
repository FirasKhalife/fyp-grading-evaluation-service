# syntax=docker/dockerfile:1
# Shared by every Spring service repo (scripts/check-shared-code.sh in fyp-grading-docker-compose keeps the copies
# identical). Base images are pinned by digest; Dependabot proposes updates.

# 1. Build the jar and extract it into layers, in the layout `java -jar` (and so the AOT cache) can use.
FROM maven:3.9.16-eclipse-temurin-25-alpine@sha256:781f1da87cd3d142e66e47cc149c4582a652fb7f574648206d1e956170c0dc90 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline || true
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -Dmaven.test.skip=true -Dspotless.check.skip=true -Dcheckstyle.skip=true \
 && java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# 2. A Java runtime with only the modules this service uses (jdeps), plus the ones it reaches by reflection or
#    service loading (DNS for MongoDB, JMX for Actuator, crypto providers, the zip file system).
FROM eclipse-temurin:25.0.5_8-jdk-alpine-3.24@sha256:c7c3d0bdf7f2bbc5a05a6712ed334d198360105634ea9adf76fca94f381b1e3f AS jre
COPY --from=build /app/target/extracted /x
RUN jdeps --ignore-missing-deps -q --multi-release 25 --print-module-deps \
      --class-path '/x/dependencies/lib/*' /x/application/app.jar > /modules \
 && jlink --add-modules "$(cat /modules),java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.crypto.cryptoki,jdk.management,jdk.naming.dns,jdk.net,jdk.unsupported,jdk.zipfs" \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /jre \
 && /jre/bin/java -Xshare:dump

# 3. Runtime: Alpine, upgraded, without its package manager. Busybox stays for the health check (wget).
FROM alpine:3.24@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
RUN apk upgrade --no-cache \
 && adduser -S -D -H -u 10001 app \
 && rm -rf /sbin/apk /etc/apk /lib/apk /usr/share/apk /var/cache/apk
ENV JAVA_HOME=/opt/java PATH=/opt/java/bin:$PATH
COPY --from=jre /jre /opt/java
WORKDIR /app
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./

# AOT cache (JEP 483/514/515): a training run starts the app until its context is refreshed, with no database,
# broker, registry or config server, and records the classes it loaded and linked. Every start then maps them from
# app.aot instead of loading them again. Measured on admin-service: 11.4 s to started without it, 6.0 s with it.
# The training run uses the run-time GC and compiler (which the cache must match), but none of the size caps: the
# cache is assembled in metaspace.
RUN JDK_JAVA_OPTIONS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1" java -XX:AOTCacheOutput=app.aot \
      -Dspring.context.exit=onRefresh -Dspring.config.import= -Dspring.cloud.config.enabled=false \
      -Dspring.data.mongodb.auto-index-creation=false \
      -Deureka.client.register-with-eureka=false -Deureka.client.fetch-registry=false \
      -Dspring.liquibase.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -Dspring.cloud.config.server.git.clone-on-start=false \
      -jar app.jar

# Small-heap defaults (the production compose sets its own, sized from each memory limit), with the GC and compiler
# the cache was trained with.
ENV JDK_JAVA_OPTIONS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xmx144m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError"
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "app.jar"]
