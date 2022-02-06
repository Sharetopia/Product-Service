FROM openjdk:17-jdk-alpine
COPY . /application/
WORKDIR /application
RUN ./gradlew build -x test
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /application/build/libs
EXPOSE 8080
ENTRYPOINT java -jar product-service-*.jar --spring.data.mongodb.host=$MONGO_HOST --spring.elasticsearch.rest.uris=$ELASTICSEARCH_URI