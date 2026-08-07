package com.braculink.swap;

import com.braculink.swap.engine.CycleMatchingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publishes the matching engine as a bean.
 *
 * <p>This class lives deliberately <em>outside</em> {@code com.braculink.swap.engine}: the engine
 * package stays free of every Spring annotation, while the services that need it still get it
 * through ordinary constructor injection. The engine is stateless, so one shared instance is
 * correct.
 */
@Configuration
public class SwapEngineConfig {

    @Bean
    public CycleMatchingService cycleMatchingService() {
        return new CycleMatchingService();
    }
}
