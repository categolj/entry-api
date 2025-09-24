package am.ik.blog.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import javax.sql.DataSource;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class DataSourceConfig {

	@Bean
	static BeanPostProcessor dataSourceProxyRegistrar(ObjectProvider<OpenTelemetry> openTelemetry) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof DataSource dataSource && !ScopedProxyUtils.isScopedTarget(beanName)) {
					return JdbcTelemetry.builder(openTelemetry.getObject())
						.setTransactionInstrumenterEnabled(true)
						.build()
						.wrap(dataSource);
				}
				else {
					return bean;
				}
			}
		};
	}

}
