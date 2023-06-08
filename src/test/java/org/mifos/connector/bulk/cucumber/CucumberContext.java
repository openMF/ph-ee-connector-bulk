package org.mifos.connector.bulk.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.google.common.truth.Truth.assertThat;

@CucumberContextConfiguration
@SpringBootTest
@CamelSpringBootTest
@UseAdviceWith
@ActiveProfiles("test")
public class CucumberContext {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    void contextLoads() {
        assertThat(producerTemplate).isNotNull();
    }

}
