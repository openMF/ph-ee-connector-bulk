package org.mifos.connector.phee;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.mifos.connector.phee.camel.config.HttpClientConfigurerTrustAllCACerts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConnectorPheeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectorPheeApplication.class, args);
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
