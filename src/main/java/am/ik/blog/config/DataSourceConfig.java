package am.ik.blog.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.proxy.GlobalConnectionIdManager;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.observation.tracing.ConnectionTracingObservationHandler;
import net.ttddyy.observation.tracing.DataSourceObservationListener;
import net.ttddyy.observation.tracing.QueryTracingObservationHandler;
import net.ttddyy.observation.tracing.ResultSetTracingObservationHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
class DataSourceConfig {

	private static final int ORDER = MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER
			- 1000;

	@Bean
	static BeanPostProcessor dataSourceProxyRegistrar(ObjectProvider<ObservationRegistry> observationRegistry) {
		DataSourceObservationListener listener = new DataSourceObservationListener(observationRegistry::getObject);
		GlobalConnectionIdManager connectionIdManager = new GlobalConnectionIdManager();
		return new BeanPostProcessor() {
			@Override
			public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof DataSource dataSource && !ScopedProxyUtils.isScopedTarget(beanName)) {
					return ProxyDataSourceBuilder.create(beanName, dataSource)
						.logQueryBySlf4j(SLF4JLogLevel.DEBUG)
						.asJson()
						.logSlowQueryBySlf4j(3, TimeUnit.SECONDS, SLF4JLogLevel.WARN)
						.listener(listener)
						.connectionIdManager(connectionIdManager)
						.buildProxy();
				}
				else {
					return bean;
				}
			}
		};
	}

	@Bean
	@Order(ORDER)
	ConnectionTracingObservationHandler connectionTracingObservationHandler(Tracer tracer) {
		return new ConnectionTracingObservationHandler(tracer);
	}

	@Bean
	@Order(ORDER)
	QueryTracingObservationHandler queryTracingObservationHandler(Tracer tracer) {
		return new QueryTracingObservationHandler(tracer);
	}

	@Bean
	@Order(ORDER)
	ResultSetTracingObservationHandler resultSetTracingObservationHandler(Tracer tracer) {
		return new ResultSetTracingObservationHandler(tracer);
	}

}
