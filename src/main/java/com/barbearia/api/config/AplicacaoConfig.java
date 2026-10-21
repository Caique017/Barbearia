package com.barbearia.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

@Configuration
@EnableAsync
public class AplicacaoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
