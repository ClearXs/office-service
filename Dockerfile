FROM --platform=linux/amd64 openjdk:21

LABEL cc.allio.uno.turbo.author=jiangw1027@gmail.com

RUN mkdir -p /turbo

WORKDIR /turbo

COPY ./target/office-service.jar app.jar

EXPOSE 8700

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx256m -Djava.security.egd=file:/dev/./urandom --add-opens java.base/java.lang=ALL-UNNAMED"

CMD java $JAVA_OPTS -jar app.jar
