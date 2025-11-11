package am.ik.blog.entry;

import am.ik.blog.BlogProps;
import am.ik.blog.GitHubProps;
import am.ik.blog.util.Tuple2;
import am.ik.blog.util.Tuples;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class EntryInitializer implements CommandLineRunner {

	private final BlogProps blogProps;

	private final GitHubProps gitHubProps;

	private final EntryFetcher entryFetcher;

	private final EntryRepository entryRepository;

	private final Logger logger = LoggerFactory.getLogger(EntryInitializer.class);

	public EntryInitializer(BlogProps blogProps, GitHubProps gitHubProps, EntryFetcher entryFetcher,
			EntryRepository entryRepository) {
		this.blogProps = blogProps;
		this.gitHubProps = gitHubProps;
		this.entryFetcher = entryFetcher;
		this.entryRepository = entryRepository;
	}

	@Override
	public void run(String... args) throws Exception {
		BlogProps.Init init = this.blogProps.getInit();
		if (!init.isEnabled()) {
			return;
		}
		logger.info("Initializing entries ({})", init);
		String tenantId = init.getTenantId();
		Tuple2<String, String> ownerAndRepo = this.getOwnerAndRepo(tenantId);
		BlogProps.Init.Fetch fetch = init.getFetch();
		logger.info("Importing entries from https://github.com/{}/{} ({}-{})", ownerAndRepo.getT1(),
				ownerAndRepo.getT2(), fetch.getFrom(), fetch.getTo());
		IntStream.rangeClosed(fetch.getFrom(), fetch.getTo()).boxed().map(entryId -> {
			try {
				return this.entryFetcher.fetch(tenantId, ownerAndRepo.getT1(), ownerAndRepo.getT2(),
						String.format("content/%05d.md", entryId));
			}
			catch (HttpClientErrorException e) {
				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					logger.info("Entry not found: {}", entryId);
				}
				else {
					logger.warn(e.getMessage(), e);
				}
				return Optional.<Entry>empty();
			}
		})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.peek(this.entryRepository::save)
			.forEach(e -> logger.info("Import key:{} title:{}", e.entryKey(), e.frontMatter().title()));
		logger.info("Finished importing entries");
	}

	private Tuple2<String, String> getOwnerAndRepo(@Nullable String tenantId) {
		if (tenantId == null) {
			return Tuples.of(this.gitHubProps.getContentOwner(), this.gitHubProps.getContentRepo());
		}
		else {
			final GitHubProps props = this.gitHubProps.getTenants().get(tenantId);
			if (props == null) {
				return this.getOwnerAndRepo(null);
			}
			return Tuples.of(Objects.requireNonNullElse(props.getContentOwner(), this.gitHubProps.getContentOwner()),
					Objects.requireNonNullElse(props.getContentRepo(), this.gitHubProps.getContentRepo()));
		}
	}

}
