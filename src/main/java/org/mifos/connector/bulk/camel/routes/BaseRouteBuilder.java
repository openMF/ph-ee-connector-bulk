package org.mifos.connector.bulk.camel.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.mifos.connector.bulk.config.OperationsAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public abstract class BaseRouteBuilder extends RouteBuilder {

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    public OperationsAppConfig operationsAppConfig;

    @Autowired
    ZeebeClient zeebeClient;

//    @Value("#{'${tenants}'.split(',')}")
//    protected List<String> tenants;

    @Value("${cloud.aws.s3-base-url}")
    protected String awsS3BaseUrl;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public RouteDefinition getBaseExternalApiRequestRouteDefinition(String routeId, HttpRequestMethod httpMethod) {
        return from(String.format("direct:%s", routeId))
                .id(routeId)
                .log("Starting external API request route: " + routeId)
                .removeHeader("*")
                .setHeader(Exchange.HTTP_METHOD, constant(httpMethod.text))
                .setHeader("X-Date", simple(ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )))
                .setHeader("Content-Type", constant("application/json;charset=UTF-8"))
                .setHeader("Accept", constant("application/json, text/plain, */*"));
    }

    protected enum HttpRequestMethod {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE")
        ;

        private final String text;

        HttpRequestMethod(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
