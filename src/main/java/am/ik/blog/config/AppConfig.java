package am.ik.blog.config;

import am.ik.blog.BlogProps;
import am.ik.blog.tokenizer.KuromojiTokenizer;
import am.ik.blog.tokenizer.Tokenizer;
import am.ik.blog.tokenizer.TrigramTokenizer;
import java.time.Clock;
import java.time.InstantSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AppConfig {

	@Bean
	InstantSource instantSource() {
		return Clock.systemDefaultZone();
	}

	@Bean
	Tokenizer tokenizer(BlogProps props) {
		return switch (props.getTokenizerType()) {
			case KUROMOJI -> new KuromojiTokenizer();
			case TRIGRAM -> new TrigramTokenizer();
		};
	}

	@Bean
	AccessLogger accessLogger() {
		return AccessLogger.builder().filter(httpExchange -> {
			String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator")
					|| uri.startsWith("/_static"));
		}).addKeyValues(true).build();
	}

}
