FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM payara/server-full:7.2025.2

# ROOT.war alone maps to context "/ROOT"; force "/" so docroot is the JSF app (see init_1_generate_deploy_commands.sh + DEPLOY_PROPS).
ENV DEPLOY_PROPS="--contextroot /"

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget ca-certificates \
    && wget -q -O /opt/payara/appserver/glassfish/domains/domain1/lib/postgresql.jar \
        https://jdbc.postgresql.org/download/postgresql-42.7.5.jar \
    && apt-get purge -y wget \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# Payara Docker image scans DEPLOY_DIR (/opt/payara/deployments) at boot via init_1_generate_deploy_commands.sh
COPY --from=build /build/target/accounting.war /opt/payara/deployments/ROOT.war

COPY docker/payara-entrypoint.sh /payara-entrypoint.sh
RUN chmod +x /payara-entrypoint.sh && chown payara:payara /payara-entrypoint.sh

USER payara

ENTRYPOINT ["/payara-entrypoint.sh"]
