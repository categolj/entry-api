package am.ik.blog.config;

import am.ik.blog.GitHubProps;
import am.ik.blog.github.Committer;
import am.ik.blog.github.GitCommit;
import am.ik.blog.github.GitCommitter;
import am.ik.blog.github.GitHubClient;
import am.ik.blog.github.GitHubUserContentClient;
import am.ik.blog.github.Parent;
import am.ik.blog.github.Tree;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(GitHubConfig.RuntimeHints.class)
class GitHubConfig {

	final Predicate<HttpStatusCode> allwaysTrueStatusPredicate = __ -> true;

	final RestClient.ResponseSpec.ErrorHandler noOpErrorHandler = (req, res) -> {
	};

	@Bean
	GitHubClient gitHubClient(GitHubProps props, RestClient.Builder restClientBuilder) {
		RestClient restClient = restClientBuilder.baseUrl(props.getApiUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "token %s".formatted(props.getAccessToken()))
			.build();
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
			.build();
		return factory.createClient(GitHubClient.class);
	}

	@Bean
	Map<String, GitHubClient> tenantsGitHubClient(GitHubProps props, RestClient.Builder restClientBuilder) {
		return props.getTenants().entrySet().stream().collect(Collectors.toUnmodifiableMap(Entry::getKey, e -> {
			GitHubProps tenantProps = e.getValue();
			RestClient restClient = restClientBuilder.baseUrl(props.getApiUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "token %s".formatted(tenantProps.getAccessToken()))
				.build();
			HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build();
			return factory.createClient(GitHubClient.class);
		}));
	}

	@Bean
	GitHubUserContentClient gitHubUserContentClient(GitHubProps props, RestClient.Builder restClientBuilder) {
		RestClient restClient = restClientBuilder.baseUrl("https://raw.githubusercontent.com")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "token %s".formatted(props.getAccessToken()))
			.defaultStatusHandler(allwaysTrueStatusPredicate, noOpErrorHandler)
			.build();
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
			.build();
		return factory.createClient(GitHubUserContentClient.class);
	}

	static class RuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(org.springframework.aot.hint.RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerConstructor(GitCommit.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(GitCommitter.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Committer.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Parent.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Tree.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE);
		}

	}

}
