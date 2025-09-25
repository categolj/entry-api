package am.ik.blog.config;

import am.ik.blog.GitHubProps;
import am.ik.blog.entry.web.EntryController;
import am.ik.pagination.web.CursorPageRequestHandlerMethodArgumentResolver;
import am.ik.webhook.spring.WebhookVerifierRequestBodyAdvice;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CursorPageRequestHandlerMethodArgumentResolver<>(Instant::parse,
				props -> props.withSizeDefault(EntryController.DEFAULT_PAGE_SIZE).withSizeMax(1024)));
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		String viewName = "forward:/index.html";
		registry.addViewController("/").setViewName(viewName);
		registry.addViewController("/console/**").setViewName(viewName);
	}

	@Bean
	WebhookVerifierRequestBodyAdvice webhookVerifierRequestBodyAdvice(GitHubProps gitHubProps) {
		return WebhookVerifierRequestBodyAdvice.githubSha256(gitHubProps.getWebhookSecret());
	}

}
