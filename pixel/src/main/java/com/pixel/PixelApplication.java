package com.pixel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PixelApplication {

  public static void main(String[] args) {
    SpringApplication.run(PixelApplication.class, args);
  }

}

