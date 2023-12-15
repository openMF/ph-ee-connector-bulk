package org.mifos.connector.bulk;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.mifos.connector.bulk.camel.config.HttpClientConfigurerTrustAllCACerts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConnectorBulkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectorBulkApplication.class, args);
	}

	@Bean
	public CsvMapper csvMapper() {
		return new CsvMapper();
	}

	@Bean
	public HttpClientConfigurerTrustAllCACerts httpClientConfigurer() {
		return new HttpClientConfigurerTrustAllCACerts();
	}

}
