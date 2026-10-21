package com.barbearia.api.compartilhado.config;

import com.barbearia.api.compartilhado.seguranca.LimitesPorIp;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(LimitesPorIp.class)
public class AplicacaoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
