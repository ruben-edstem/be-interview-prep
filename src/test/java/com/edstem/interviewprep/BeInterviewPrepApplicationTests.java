package com.edstem.interviewprep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.file-upload.directory=target/test-uploads")
class BeInterviewPrepApplicationTests {

  @Test
  void contextLoads() {}
}
