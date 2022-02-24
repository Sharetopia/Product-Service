FROM openjdk:17-jdk-alpine
COPY . /application/
WORKDIR /application
RUN ./gradlew build -x test
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown spring ./logs/application.log
USER spring:spring
EXPOSE 8080
ENTRYPOINT java -jar ./build/libs/product-service-*.jar --spring.data.mongodb.host=$MONGO_HOST --spring.elasticsearch.rest.uris=$ELASTICSEARCH_URI