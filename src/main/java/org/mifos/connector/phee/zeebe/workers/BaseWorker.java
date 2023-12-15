package org.mifos.connector.phee.zeebe.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.connector.phee.camel.routes.RouteId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

// TODO: Duplicate file (Also exists in <service-name>)
@Component
public abstract class BaseWorker {

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private CsvMapper csvMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    protected WorkerConfig workerConfig;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostConstruct
    public abstract void setup();

    public void newWorker(Worker worker, JobHandler handler) {
        zeebeClient.newWorker()
                .jobType(worker.getValue())
                .handler(handler)
                .name(worker.getValue())
                .maxJobsActive(workerMaxJobs)
                .open();
    }

    public void sendToCamelRoute(RouteId routeId, Exchange exchange) {
        producerTemplate.send(routeId.getValue(), exchange);
    }

}
