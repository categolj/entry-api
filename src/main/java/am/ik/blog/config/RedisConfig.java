package am.ik.blog.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("redis")
@Configuration(proxyBeanMethods = false)
@EnableCaching
class RedisConfig implements CachingConfigurer {

	@Override
	public CacheErrorHandler errorHandler() {
		return new LoggingCacheErrorHandler(true);
	}

}
