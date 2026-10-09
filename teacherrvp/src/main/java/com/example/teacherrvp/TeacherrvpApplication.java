package com.example.teacherrvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeacherrvpApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeacherrvpApplication.class, args);
    }
}
