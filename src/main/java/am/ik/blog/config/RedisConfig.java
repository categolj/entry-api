package am.ik.blog.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("redis")
@Configuration(proxyBeanMethods = false)
@EnableCaching
class RedisConfig {

}
