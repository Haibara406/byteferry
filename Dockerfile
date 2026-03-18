FROM eclipse-temurin:17-jre

LABEL maintainer="Haibara <haibara406@gmail.com>"
LABEL description="ByteFerry Backend Service"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 复制已构建的jar包
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8076

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
