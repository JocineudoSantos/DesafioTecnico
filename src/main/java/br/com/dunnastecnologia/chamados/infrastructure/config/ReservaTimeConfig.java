package br.com.dunnastecnologia.chamados.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ReservaTimeConfig {

    @Bean
    public ZoneId appZoneId(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        return ZoneId.of(timezone);
    }

    @Bean
    public Clock appClock(ZoneId appZoneId) {
        return Clock.system(appZoneId);
    }
}
