package com.example.filestorageservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// В тестах брокера нет, поэтому листенер очереди не стартуем
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class FileStorageServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
