package org.mifos.connector.phee.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ZeebeClientConfiguration {

    @Value("${zeebe.broker.contactpoint}")
    private String zeebeBrokerContactPoint;

    @Value("${zeebe.client.max-execution-threads}")
    private int zeebeClientMaxThreads;

    @Value("${zeebe.client.max-jobs-active}")
    private int zeebeMaxJobsActive;

    @Bean
    public ZeebeClient setup() {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(zeebeBrokerContactPoint)
                .usePlaintext()
                .defaultJobPollInterval(Duration.ofMillis(1))
                .defaultJobWorkerMaxJobsActive(zeebeMaxJobsActive)
                .numJobWorkerExecutionThreads(zeebeClientMaxThreads)
                .build();
    }

}
