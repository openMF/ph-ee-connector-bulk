package org.mifos.connector.bulk.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component
public class ZeebeProcessStarter {

    private static final Logger logger = LoggerFactory.getLogger(ZeebeProcessStarter.class);

    @Autowired
    private ZeebeClient zeebeClient;

    public static void zeebeVariablesToCamelHeaders(Map<String, Object> variables, Exchange exchange, String... names) {
        Arrays.stream(names).forEach(name -> {
            Object value = variables.get(name);
            if (value == null) {
                logger.error("failed to find Zeebe variable name {}", name);
            }
            exchange.getIn().setHeader(name, value);
        });
    }

    public static void camelHeadersToZeebeVariables(Exchange exchange, Map<String, Object> variables, String... names) {
        Arrays.stream(names).forEach(name -> {
            String header = exchange.getIn().getHeader(name, String.class);
            if (header == null) {
                logger.error("failed to find Camel Exchange header {}", name);
            }
            variables.put(name, header);
        });
    }

    public void startZeebeWorkflow(String workflowId, Map<String, Object> extraVariables) {

        Map<String, Object> variables = new HashMap<>();
        variables.put(ZeebeVariables.ORIGIN_DATE, Instant.now().toEpochMilli());
        if(extraVariables != null) {
            variables.putAll(extraVariables);
        }

        // TODO if successful transfer response arrives in X timeout return it otherwise do callback
        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(workflowId)
                .latestVersion()
                .variables(variables)
                .send()
                .join();


        logger.info("zeebee workflow instance from process {}", workflowId);
    }

    public String startZeebeWorkflow(String workflowId, String request, Map<String, Object> extraVariables) {
        String transactionId = generateTransactionId();

        Map<String, Object> variables = new HashMap<>();
        variables.put(ZeebeVariables.TRANSACTION_ID, transactionId);
        variables.put(ZeebeVariables.CHANNEL_REQUEST, request);
        variables.put(ZeebeVariables.ORIGIN_DATE, Instant.now().toEpochMilli());
        if(extraVariables != null) {
            variables.putAll(extraVariables);
        }

        // TODO if successful transfer response arrives in X timeout return it otherwise do callback
        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId(workflowId)
                .latestVersion()
                .variables(variables)
                .send()
                .join();


        logger.info("zeebee workflow instance from process {} started with transactionId {}", workflowId, transactionId);
        return transactionId;
    }

    // TODO generate proper cluster-safe transaction id
    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}
