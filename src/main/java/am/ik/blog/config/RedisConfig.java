package am.ik.blog.config;

import am.ik.blog.entry.CacheNames;
import am.ik.blog.entry.Entry;
import am.ik.pagination.CursorPage;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.ClientResourcesBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.json.JsonMapper;

@Profile("redis")
@Configuration(proxyBeanMethods = false)
@EnableCaching
class RedisConfig implements CachingConfigurer {

	@Override
	public CacheErrorHandler errorHandler() {
		return new LoggingCacheErrorHandler(true);
	}

	@Bean
	ClientResourcesBuilderCustomizer lettuceTracing(ObservationRegistry observationRegistry) {
		return builder -> builder.tracing(new MicrometerTracing(observationRegistry, "entry-redis"));
	}

	@Bean
	RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(JsonMapper jsonMapper) {
		return builder -> builder.withInitialCacheConfigurations(Map.of(CacheNames.ENTRY,
				RedisCacheConfiguration.defaultCacheConfig()
					.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, Entry.class))),
				CacheNames.LATEST_ENTRIES,
				RedisCacheConfiguration.defaultCacheConfig()
					.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, CursorPage.class)))));
	}

}
