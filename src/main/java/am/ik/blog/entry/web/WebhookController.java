package am.ik.blog.entry.web;

import am.ik.blog.GitHubProps;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryFetcher;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.EntryRepository;
import am.ik.webhook.WebhookAuthenticationException;
import am.ik.webhook.WebhookVerifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static am.ik.webhook.WebhookHttpHeaders.X_HUB_SIGNATURE_256;
import static java.util.stream.Collectors.toUnmodifiableMap;

@RestController
public class WebhookController {

	private final EntryFetcher entryFetcher;

	private final EntryRepository entryRepository;

	private final WebhookVerifier webhookVerifier;

	private final Map<String, WebhookVerifier> tenantsWebhookVerifier;

	private final JsonMapper jsonMapper;

	public WebhookController(GitHubProps props, EntryFetcher entryFetcher, EntryRepository entryRepository,
			JsonMapper jsonMapper) {
		this.entryFetcher = entryFetcher;
		this.entryRepository = entryRepository;
		this.webhookVerifier = WebhookVerifier.gitHubSha256(props.getWebhookSecret());
		this.tenantsWebhookVerifier = props.getTenants()
			.entrySet()
			.stream()
			.collect(toUnmodifiableMap(Map.Entry::getKey,
					e -> WebhookVerifier.gitHubSha256(e.getValue().getWebhookSecret())));
		this.jsonMapper = jsonMapper;
	}

	@PostMapping(path = { "/webhook", "/tenants/{tenantId}/webhook" })
	public ResponseEntity<?> webhook(@RequestHeader(name = X_HUB_SIGNATURE_256) String signature,
			@RequestBody String payload, @PathVariable(required = false) String tenantId) {
		WebhookVerifier verifier = tenantId == null ? this.webhookVerifier
				: this.tenantsWebhookVerifier.getOrDefault(tenantId, this.webhookVerifier);
		try {
			verifier.verify(payload, signature);
		}
		catch (WebhookAuthenticationException e) {
			return ResponseEntity.badRequest()
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid signature: " + signature));
		}
		return this.processWebhook(payload, tenantId)
			.<ResponseEntity<?>>map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.badRequest()
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid payload: " + payload)));
	}

	Optional<List<Map<String, EntryKey>>> processWebhook(String payload, @Nullable String tenantId) {
		final JsonNode node = this.jsonMapper.readValue(payload, JsonNode.class);
		final String[] repository = node.get("repository").get("full_name").asText().split("/", 2);
		final String owner = repository[0];
		final String repo = repository[1];
		if (!node.has("commits")) {
			return Optional.empty();
		}
		final Stream<JsonNode> commits = StreamSupport.stream(node.get("commits").spliterator(), false);
		final List<Map<String, EntryKey>> result = new ArrayList<>();
		commits.forEach(commit -> {
			Stream.of("added", "modified").forEach(key -> {
				this.paths(commit.get(key)).forEach(path -> {
					Optional<Entry> fetch = this.entryFetcher.fetch(tenantId, owner, repo, path);
					fetch.ifPresent(entry -> {
						result.add(Map.of(key, entry.entryKey()));
						this.entryRepository.save(entry);
					});
				});
			});
			this.paths(commit.get("removed")).forEach(path -> {
				Optional<EntryKey> fetch = this.entryFetcher.fetch(tenantId, owner, repo, path).map(Entry::entryKey);
				fetch.ifPresent(entryKey -> {
					result.add(Map.of("removed", entryKey));
					this.entryRepository.deleteById(entryKey);
				});
			});
		});
		return Optional.of(result);
	}

	Stream<String> paths(JsonNode paths) {
		return StreamSupport.stream(paths.spliterator(), false).map(JsonNode::asText);
	}

}
