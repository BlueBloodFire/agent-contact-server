FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY . .
RUN mvn package -DskipTests --no-transfer-progress

FROM openjdk:17-jdk-slim
ENV PARAMS=""
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY --from=build /build/ai-agent-contact-app/target/agent-scaffold-app.jar /agent-scaffold-app.jar
EXPOSE 8092
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /agent-scaffold-app.jar $PARAMS"]
