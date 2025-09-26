package am.ik.blog.entry;

import am.ik.blog.security.Authorized;
import am.ik.blog.security.Privilege;
import am.ik.pagination.CursorPage;
import am.ik.pagination.CursorPageRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

@Service
@Retryable(includes = { OptimisticLockingFailureException.class }, maxAttempts = 4, delay = 100L, multiplier = 2,
		jitter = 10L)
@Observed
public class EntryService {

	public static final int DEFAULT_PAGE_SIZE = 30;

	private static final CursorPageRequest<Instant> DEFAULT_CURSOR_REQUEST = new CursorPageRequest<>(null,
			DEFAULT_PAGE_SIZE, CursorPageRequest.Navigation.NEXT);

	private final EntryRepository entryRepository;

	public EntryService(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.GET)
	@Cacheable(cacheNames = CacheNames.ENTRY, key = "#entryKey")
	public Optional<Entry> findById(@Nullable @P("tenantId") String tenantId, EntryKey entryKey) {
		return entryRepository.findById(entryKey);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<Entry> findAll(@Nullable @P("tenantId") String tenantId, List<EntryKey> entryKeys) {
		return entryRepository.findAll(entryKeys);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public CursorPage<Entry, Instant> findOrderByUpdated(@Nullable @P("tenantId") String tenantId,
			SearchCriteria searchCriteria, CursorPageRequest<Instant> pageRequest) {
		return entryRepository.findOrderByUpdated(tenantId, searchCriteria, pageRequest);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	@Cacheable(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'")
	public CursorPage<Entry, Instant> findLatest(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findOrderByUpdated(tenantId, SearchCriteria.NULL_CRITERIA, DEFAULT_CURSOR_REQUEST);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<List<Category>> findAllCategories(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findAllCategories(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.LIST)
	public List<TagAndCount> findAllTags(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.findAllTags(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	@Caching(evict = { @CacheEvict(cacheNames = CacheNames.ENTRY, key = "#entry.entryKey"),
			@CacheEvict(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'") })
	public Entry save(@Nullable @P("tenantId") String tenantId, Entry entry) {
		return entryRepository.save(entry);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	public Long nextId(@Nullable @P("tenantId") String tenantId) {
		return entryRepository.nextId(tenantId);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	@Caching(evict = { @CacheEvict(cacheNames = CacheNames.ENTRY, allEntries = true),
			@CacheEvict(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'") })
	public void saveAll(@Nullable @P("tenantId") String tenantId, Entry... entries) {
		entryRepository.saveAll(entries);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	@Caching(evict = { @CacheEvict(cacheNames = CacheNames.ENTRY, allEntries = true),
			@CacheEvict(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'") })
	public void saveAll(@Nullable @P("tenantId") String tenantId, List<Entry> entries) {
		entryRepository.saveAll(entries);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.DELETE)
	@Caching(evict = { @CacheEvict(cacheNames = CacheNames.ENTRY, key = "#entryKey"),
			@CacheEvict(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'") })
	public void deleteById(@Nullable @P("tenantId") String tenantId, EntryKey entryKey) {
		entryRepository.deleteById(entryKey);
	}

	@Authorized(resource = "entry", requiredPrivileges = Privilege.EDIT)
	@Caching(evict = { @CacheEvict(cacheNames = CacheNames.ENTRY, key = "#entryKey"),
			@CacheEvict(cacheNames = CacheNames.LATEST_ENTRIES, key = "#tenantId ?: '_'") })
	public void updateSummary(@Nullable @P("tenantId") String tenantId, EntryKey entryKey, String summary) {
		entryRepository.updateSummary(entryKey, summary);
	}

}
