FROM maven:3.6.0-jdk-11-slim AS build
#FROM maven:3.8.3-jdk-17 AS build
COPY src /home/app/src
COPY pom.xml /home/app
# copy all JSON files with issue/present payload definitions
COPY **/*.json /home/app/
COPY ./AppCreationScripts/aadappcert* /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests
#
# Package stage
#
#FROM openjdk:11-jre-slim
FROM openjdk:20-jdk 
COPY --from=build /home/app/target/java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar
COPY --from=build /home/app/*.json /usr/local/lib
COPY --from=build /home/app/aadappcert* /usr/local/lib
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]
