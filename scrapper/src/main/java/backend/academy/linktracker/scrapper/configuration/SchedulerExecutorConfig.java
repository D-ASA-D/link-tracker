package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService linkUpdateExecutor(SchedulerProperties properties) {
        int threads = properties.getThreads() <= 0 ? 1 : properties.getThreads();
        return Executors.newFixedThreadPool(threads);
    }
}
