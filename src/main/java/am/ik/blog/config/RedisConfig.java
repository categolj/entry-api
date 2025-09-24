package am.ik.blog.config;

import am.ik.blog.entry.Entry;
import am.ik.pagination.CursorPage;
import java.util.Map;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson3JsonRedisSerializer;
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
	RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(JsonMapper jsonMapper) {
		return builder -> builder
			.withInitialCacheConfigurations(Map.of("entry",
					RedisCacheConfiguration.defaultCacheConfig()
						.serializeValuesWith(RedisSerializationContext.SerializationPair
							.fromSerializer(new Jackson3JsonRedisSerializer<>(jsonMapper, Entry.class))),
					"latestEntries",
					RedisCacheConfiguration.defaultCacheConfig()
						.serializeValuesWith(RedisSerializationContext.SerializationPair
							.fromSerializer(new Jackson3JsonRedisSerializer<>(jsonMapper, CursorPage.class)))));
	}

}
