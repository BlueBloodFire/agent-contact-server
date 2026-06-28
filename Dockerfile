FROM openjdk:17-jdk-slim
ENV PARAMS=""
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY ai-agent-contact-app/target/agent-scaffold-app.jar /agent-scaffold-app.jar
EXPOSE 8092
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /agent-scaffold-app.jar $PARAMS"]
