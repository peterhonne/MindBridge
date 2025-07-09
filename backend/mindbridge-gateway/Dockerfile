FROM openjdk:24-jdk-slim
LABEL authors="Honne"


RUN addgroup --system app && adduser --system --ingroup app app
USER app

ENV JAVA_OPTS="-Dfile.encoding=utf-8 -Duser.timezone=Pacific/Auckland  -Xmx448m" \
    PARAMS="--server.port=8080"


COPY target/mindbridge-gateway-1.0.0.jar app.jar



ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app.jar $PARAMS"]