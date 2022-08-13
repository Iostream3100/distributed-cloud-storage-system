FROM maven:3.8.6-eclipse-temurin-8-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN mvn package -DskipTests