package com.edstem.interviewprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BeInterviewPrepApplication {

  public static void main(String[] args) {
    SpringApplication.run(BeInterviewPrepApplication.class, args);
  }
}
