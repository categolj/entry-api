package am.ik.blog.config;

import io.micrometer.core.instrument.config.MeterFilter;
import java.util.function.Predicate;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MicrometerConfig {

	@Bean
	MeterRegistryCustomizer<?> meterRegistryCustomizer(UriFilter uriFilter) {
		final Predicate<String> negate = uriFilter.negate();
		return registry -> registry.config() //
			.meterFilter(MeterFilter.deny(id -> {
				final String uri = id.getTag("uri");
				return negate.test(uri);
			}));
	}

	// @Bean
	// ProcessorMetrics processorMetrics() {
	// Tags extraTags = Tags.empty();
	// return new ProcessorMetrics(extraTags, new
	// OpenTelemetryJvmCpuMeterConventions(extraTags));
	// }
	//
	// @Bean
	// JvmMemoryMetrics jvmMemoryMetrics() {
	// Tags extraTags = Tags.empty();
	// return new JvmMemoryMetrics(extraTags, new
	// OpenTelemetryJvmMemoryMeterConventions(extraTags));
	// }
	//
	// @Bean
	// JvmThreadMetrics jvmThreadMetrics() {
	// Tags extraTags = Tags.empty();
	// return new JvmThreadMetrics(extraTags, new
	// OpenTelemetryJvmThreadMeterConventions(extraTags));
	// }
	//
	// @Bean
	// ClassLoaderMetrics classLoaderMetrics() {
	// return new ClassLoaderMetrics(new OpenTelemetryJvmClassLoadingMeterConventions());
	// }

}