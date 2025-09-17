package am.ik.blog.entry.dsql;

import am.ik.blog.entry.Author;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryKey;
import am.ik.blog.entry.EntryRepository;
import am.ik.blog.entry.FrontMatter;
import am.ik.blog.entry.SearchCriteria;
import am.ik.blog.entry.Tag;
import am.ik.blog.entry.TagAndCount;
import am.ik.blog.entry.dsql.DsqlQueryConverter.SqlResult;
import am.ik.blog.tokenizer.Tokenizer;
import am.ik.pagination.CursorPage;
import am.ik.pagination.CursorPageRequest;
import am.ik.query.Query;
import am.ik.query.parser.QueryParser;
import java.time.Instant;
import java.time.InstantSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DsqlEntryRepository implements EntryRepository {

	public static final int TOKENS_MAX_CHUK_SIZE = 2500;

	private final JdbcClient jdbcClient;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final Tokenizer tokenizer;

	private final QueryParser queryParser = QueryParser.create();

	private final DsqlQueryConverter queryConverter;

	private final JsonMapper jsonMapper;

	private final InstantSource instantSource;

	private final RowMapper<Entry> entryRowMapper;

	private final TransactionTemplate transactionTemplate;

	private final Logger logger = LoggerFactory.getLogger(DsqlEntryRepository.class);

	public DsqlEntryRepository(JdbcClient jdbcClient, NamedParameterJdbcTemplate jdbcTemplate, JsonMapper jsonMapper,
			Tokenizer tokenizer, DsqlQueryConverter queryConverter, InstantSource instantSource,
			PlatformTransactionManager platformTransactionManager) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
		this.tokenizer = tokenizer;
		this.queryConverter = queryConverter;
		this.jsonMapper = jsonMapper;
		this.instantSource = instantSource;
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager, transactionDefinition);
		TypeReference<List<Tag>> tagsRef = new TypeReference<>() {
		};
		TypeReference<List<Category>> categoriesRef = new TypeReference<>() {
		};
		this.entryRowMapper = (rs, rowNum) -> Entry.builder()
			.entryKey(EntryKey.builder()
				.entryId(rs.getLong("public_entry_id"))
				.tenantId(rs.getString("tenant_id"))
				.build())
			.frontMatter(FrontMatter.builder()
				.title(rs.getString("title"))
				.summary(rs.getString("summary"))
				.tags(jsonMapper.readValue(rs.getString("tags"), tagsRef))
				.categories(jsonMapper.readValue(rs.getString("categories"), categoriesRef))
				.build())
			.content(rs.getString("content"))
			.created(Author.builder()
				.name(rs.getString("created_by"))
				.date(rs.getObject("created_date", OffsetDateTime.class).toInstant())
				.build())
			.updated(Author.builder()
				.name(rs.getString("last_modified_by"))
				.date(rs.getObject("last_modified_date", OffsetDateTime.class).toInstant())
				.build())
			.build();
	}

	@Override
	public Optional<Entry> findById(EntryKey entryKey) {
		return this.jdbcClient.sql("""
				SELECT
				    public_entry_id,
				    title,
				    summary,
				    content,
				    created_by,
				    created_date,
				    last_modified_by,
				    last_modified_date,
				    tenant_id,
				    categories,
				    tags
				FROM entry
				WHERE public_entry_id = :publicEntryId AND tenant_id = :tenantId
				""".trim())
			.param("publicEntryId", entryKey.entryId())
			.param("tenantId", entryKey.tenantId())
			.query(this.entryRowMapper)
			.optional();
	}

	@Override
	public List<Entry> findAll(List<EntryKey> entryKeys) {
		if (entryKeys.isEmpty()) {
			return List.of();
		}
		Set<String> tenantIdSet = entryKeys.stream().map(EntryKey::tenantId).collect(Collectors.toUnmodifiableSet());
		if (tenantIdSet.size() > 1) {
			throw new IllegalArgumentException("All EntryKeys must have the same tenantId");
		}
		String tenantId = tenantIdSet.iterator().next();
		List<Long> publicEntryIds = entryKeys.stream().map(EntryKey::entryId).toList();
		return this.jdbcClient.sql("""
				SELECT
				    public_entry_id,
				    title,
				    summary,
				    content,
				    created_by,
				    created_date,
				    last_modified_by,
				    last_modified_date,
				    tenant_id,
				    categories,
				    tags
				FROM entry
				WHERE public_entry_id IN (:publicEntryIds) AND tenant_id = :tenantId
				""".trim())
			.param("publicEntryIds", publicEntryIds)
			.param("tenantId", tenantId)
			.query(this.entryRowMapper)
			.list();
	}

	@Override
	public CursorPage<Entry, Instant> findOrderByUpdated(@Nullable String tenantId, SearchCriteria searchCriteria,
			CursorPageRequest<Instant> pageRequest) {
		Optional<Instant> cursor = pageRequest.cursorOptional();
		int pageSizePlus1 = pageRequest.pageSize() + 1;
		Map<String, Object> params = new HashMap<>();
		StringBuilder joinTables = new StringBuilder();
		StringBuilder queryCondition = new StringBuilder();
		StringBuilder tagCondition = new StringBuilder();
		StringBuilder categoriesCondition = new StringBuilder();
		if (StringUtils.hasLength(searchCriteria.tag())) {
			joinTables.append(", entry_tags et");
			tagCondition.append("AND e.id = et.entry_id AND et.name = :tag");
			params.put("tag", searchCriteria.tag());
		}
		List<String> categories = searchCriteria.categories();
		if (!CollectionUtils.isEmpty(categories)) {
			categoriesCondition.append("AND e.id IN (");
			categoriesCondition.append("SELECT entry_id FROM entry_categories WHERE");
			for (int i = 1; i <= categories.size(); i++) {
				String name = categories.get(i - 1);
				categoriesCondition.append(i == 1 ? " " : " OR ");
				categoriesCondition.append("(name = :name")
					.append(i)
					.append(" AND position = :position")
					.append(i)
					.append(")");
				params.put("name" + i, name);
				params.put("position" + i, i);
			}
			categoriesCondition.append(" GROUP BY entry_id HAVING COUNT(*) = :categoriesSize");
			categoriesCondition.append(")");
			params.put("categoriesSize", categories.size());
		}
		String query = searchCriteria.query();
		if (StringUtils.hasLength(query)) {
			Query parsed = queryParser.parse(searchCriteria.query());
			SqlResult converted = this.queryConverter.convertToSql(parsed);
			queryCondition.append("AND ").append(converted.whereClause());
			params.putAll(converted.parameters());
		}
		List<Entry> contentPlus1 = this.jdbcClient
			.sql("""
					SELECT DISTINCT
					    public_entry_id,
					    title,
					    summary,
					    '' as content,
					    created_by,
					    created_date,
					    last_modified_by,
					    last_modified_date,
					    tenant_id,
					    categories,
					    tags
					FROM entry e/* JOIN_TABLES */
					WHERE tenant_id = :tenantId
					/* QUERY */
					/* TAG */
					/* CATEGORIES */
					AND last_modified_date < COALESCE(:cursor, 'infinity'::timestamptz)
					ORDER BY last_modified_date DESC
					LIMIT :limit
					""".trim()
				.replace("/* JOIN_TABLES */", joinTables.toString())
				.replace("/* QUERY */", queryCondition.toString())
				.replace("/* TAG */", tagCondition.toString())
				.replace("/* CATEGORIES */", categoriesCondition.toString()))
			.param("tenantId", Objects.requireNonNullElse(tenantId, EntryKey.DEFAULT_TENANT_ID))
			.param("cursor", cursor.map(instant -> instant.atOffset(ZoneOffset.UTC)).orElse(null))
			.param("limit", pageSizePlus1)
			.params(params)
			.query(this.entryRowMapper)
			.list();
		boolean hasPrevious = cursor.isPresent();
		boolean hasNext = contentPlus1.size() == pageSizePlus1;
		List<Entry> content = hasNext ? contentPlus1.subList(0, pageRequest.pageSize()) : contentPlus1;
		return new CursorPage<>(content, pageRequest.pageSize(), Entry::toCursor, hasPrevious, hasNext);
	}

	@Override
	public List<List<Category>> findAllCategories(@Nullable String tenantId) {
		return this.jdbcClient.sql("""
				SELECT DISTINCT categories
				FROM entry
				WHERE tenant_id = :tenantId
				ORDER BY categories
				""")
			.param("tenantId", Objects.requireNonNullElse(tenantId, EntryKey.DEFAULT_TENANT_ID))
			.query((rs, i) -> this.jsonMapper.readValue(rs.getString("categories"),
					new TypeReference<List<Category>>() {
					}))
			.list();
	}

	@Override
	public List<TagAndCount> findAllTags(@Nullable String tenantId) {
		return this.jdbcClient.sql("""
				SELECT
				    et.name,
				    COUNT(*) AS count
				FROM
				    entry_tags et,
				    entry e
				WHERE
				    e.tenant_id = :tenantId
				AND e.id = et.entry_id
				GROUP BY
				    et.name
				ORDER BY
				    et.name
				""")
			.param("tenantId", Objects.requireNonNullElse(tenantId, EntryKey.DEFAULT_TENANT_ID))
			.query((rs, i) -> new TagAndCount(new Tag(rs.getString("name")), rs.getInt("count")))
			.list();
	}

	@Override
	@Transactional
	public Entry save(Entry entry) {
		UUID entryId = upsertEntry(entry);
		logger.info("Upsert entry (id={}, entryKey={})", entryId, entry.entryKey());
		this.deleteAndInsertCategories(entryId, entry);
		this.deleteAndInsertTags(entryId, entry);
		this.deleteAndInsertTokens(entryId, entry);
		return entry;
	}

	private UUID upsertEntry(Entry entry) {
		String sql = """
				INSERT INTO entry (
				    public_entry_id, title, summary, content,
				    created_by, created_date, last_modified_by, last_modified_date,
				    tenant_id, categories, tags
				) VALUES (
				    :publicEntryId, :title, :summary, :content,
				    :createdBy, :createdDate, :lastModifiedBy, :lastModifiedDate,
				    :tenantId, :categories, :tags
				)
				ON CONFLICT (public_entry_id, tenant_id)
				DO UPDATE SET
				    title = EXCLUDED.title,
				    summary = EXCLUDED.summary,
				    content = EXCLUDED.content,
				    last_modified_by = EXCLUDED.last_modified_by,
				    last_modified_date = EXCLUDED.last_modified_date,
				    categories = EXCLUDED.categories,
				    tags = EXCLUDED.tags
				RETURNING id
				""".trim();
		Instant now = this.instantSource.instant();
		FrontMatter frontMatter = entry.frontMatter();
		Author created = entry.created();
		Author updated = entry.updated();
		EntryKey entryKey = entry.entryKey();
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("publicEntryId", entryKey.entryId())
			.addValue("title", frontMatter.title())
			.addValue("summary", frontMatter.summary())
			.addValue("content", entry.content())
			.addValue("createdBy", created.name())
			.addValue("createdDate", (created.date() != null ? created.date() : now).atOffset(ZoneOffset.UTC))
			.addValue("lastModifiedBy", updated.name())
			.addValue("lastModifiedDate", (updated.date() != null ? updated.date() : now).atOffset(ZoneOffset.UTC))
			.addValue("tenantId", entryKey.tenantId())
			.addValue("categories", this.jsonMapper.writeValueAsString(frontMatter.categories()))
			.addValue("tags", this.jsonMapper.writeValueAsString(frontMatter.tags()));
		return this.jdbcTemplate.queryForObject(sql, params, UUID.class);
	}

	private void deleteAndInsertCategories(UUID entryId, Entry entry) {
		// Delete existing categories
		MapSqlParameterSource deleteParams = new MapSqlParameterSource().addValue("entryId", entryId);
		this.jdbcTemplate.update("DELETE FROM entry_categories WHERE entry_id = :entryId", deleteParams);
		// Insert new categories
		List<Category> categories = entry.frontMatter().categories();
		if (categories != null && !categories.isEmpty()) {
			AtomicInteger position = new AtomicInteger(0);
			MapSqlParameterSource[] batchParams = categories.stream()
				.map(category -> new MapSqlParameterSource().addValue("entryId", entryId)
					.addValue("name", category.name())
					.addValue("position", position.incrementAndGet()))
				.toArray(MapSqlParameterSource[]::new);
			this.jdbcTemplate.batchUpdate(
					"INSERT INTO entry_categories (entry_id, name, position) VALUES (:entryId, :name, :position)",
					batchParams);
		}
	}

	private void deleteAndInsertTags(UUID entryId, Entry entry) {
		// Delete existing tags
		MapSqlParameterSource deleteParams = new MapSqlParameterSource().addValue("entryId", entryId);
		this.jdbcTemplate.update("DELETE FROM entry_tags WHERE entry_id = :entryId", deleteParams);
		// Insert new tags
		List<Tag> tags = entry.frontMatter().tags();
		if (tags != null && !tags.isEmpty()) {
			MapSqlParameterSource[] batchParams = tags.stream()
				.map(tag -> new MapSqlParameterSource().addValue("entryId", entryId)
					.addValue("name", tag.name())
					.addValue("version", tag.version()))
				.toArray(MapSqlParameterSource[]::new);
			this.jdbcTemplate.batchUpdate(
					"INSERT INTO entry_tags (entry_id, name, version) VALUES (:entryId, :name, :version)", batchParams);
		}
	}

	private void deleteAndInsertTokens(UUID entryId, Entry entry) {
		// Delete existing tokens
		this.deleteTokens(entryId);
		Set<String> tokens = this.tokenizer.tokenize(entry.content());
		// Insert new tokens
		if (tokens != null && !tokens.isEmpty()) {
			// DSQL limits the number of rows per transaction to 3000.
			// https://docs.aws.amazon.com/aurora-dsql/latest/userguide/working-with-postgresql-compatibility-unsupported-features.html#working-with-postgresql-compatibility-unsupported-limitations
			MapSqlParameterSource[] batchParams = tokens.stream()
				.distinct() // Remove duplicates
				.map(token -> new MapSqlParameterSource().addValue("entryId", entryId).addValue("token", token))
				.toArray(MapSqlParameterSource[]::new);
			if (batchParams.length > TOKENS_MAX_CHUK_SIZE) {
				logger.warn(
						"The number of unique tokens of the entry (id: {}) exceeds {} ({}). Divide tokens into different transactions due to DSQL limitation.",
						entryId, TOKENS_MAX_CHUK_SIZE, batchParams.length);
				// Divide into batchParams into pieces of 2500 chunks
				for (int i = 0; i < batchParams.length; i += TOKENS_MAX_CHUK_SIZE) {
					int end = Math.min(i + TOKENS_MAX_CHUK_SIZE, batchParams.length);
					MapSqlParameterSource[] subBatchParams = Arrays.copyOfRange(batchParams, i, end);
					int[] inserted = this.transactionTemplate.execute(status -> this.jdbcTemplate.batchUpdate(
							"INSERT INTO entry_tokens (entry_id, token) VALUES (:entryId, :token)", subBatchParams));
					logger.info("Inserted {} tokens in iteration {}/{} for id: {}",
							Arrays.stream(Objects.requireNonNull(inserted)).sum(), i / TOKENS_MAX_CHUK_SIZE + 1,
							batchParams.length / TOKENS_MAX_CHUK_SIZE + 1, entryId);
				}
			}
			else {
				this.jdbcTemplate.batchUpdate("INSERT INTO entry_tokens (entry_id, token) VALUES (:entryId, :token)",
						batchParams);
			}
		}
	}

	@Override
	public Long nextId(@Nullable String tenantId) {
		return this.jdbcClient
			.sql("SELECT COALESCE(MAX(public_entry_id), 0) + 1 AS next FROM entry WHERE tenant_id = :tenantId")
			.param("tenantId", Objects.requireNonNullElse(tenantId, EntryKey.DEFAULT_TENANT_ID))
			.query(Long.class)
			.single();
	}

	@Override
	@Transactional
	public void saveAll(Entry... entries) {
		this.saveAll(Arrays.asList(entries));
	}

	@Override
	@Transactional
	public void saveAll(List<Entry> entries) {
		entries.forEach(this::save);
	}

	@Override
	@Transactional
	public void deleteById(EntryKey entryKey) {
		Optional<UUID> entryIdOptional = this.jdbcClient
			.sql("SELECT id FROM entry WHERE public_entry_id = :publicEntryId AND tenant_id = :tenantId")
			.param("publicEntryId", entryKey.entryId())
			.param("tenantId", entryKey.tenantId())
			.query(UUID.class)
			.optional();
		if (entryIdOptional.isEmpty()) {
			return;
		}
		UUID entryId = entryIdOptional.get();
		logger.info("Delete entry (id={}, entryKey={})", entryId, entryKey);
		this.jdbcClient.sql("DELETE FROM entry_tags WHERE entry_id = :entryId").param("entryId", entryId).update();
		this.jdbcClient.sql("DELETE FROM entry_categories WHERE entry_id = :entryId")
			.param("entryId", entryId)
			.update();
		this.deleteTokens(entryId);
		this.jdbcClient.sql("DELETE FROM entry WHERE id = :entryId").param("entryId", entryId).update();
	}

	public void deleteTokens(UUID entryId) {
		Integer numOfTokens = this.jdbcClient.sql("SELECT COUNT(*) FROM entry_tokens WHERE entry_id = :entryId")
			.param("entryId", entryId)
			.query(Integer.class)
			.single();
		if (numOfTokens > TOKENS_MAX_CHUK_SIZE) {
			logger.warn(
					"The number of unique tokens of the entry (id: {}) exceeds {} ({}). Divide tokens into different transactions due to DSQL limitation.",
					entryId, TOKENS_MAX_CHUK_SIZE, numOfTokens);
			int deleteIterations = (numOfTokens / TOKENS_MAX_CHUK_SIZE) + 1;
			for (int i = 0; i < deleteIterations; i++) {
				int deleted = this.transactionTemplate.execute(status -> this.jdbcClient.sql(
						"DELETE FROM entry_tokens WHERE entry_id = :entryId AND token IN (SELECT token FROM entry_tokens WHERE entry_id = :entryId LIMIT :limit)")
					.param("entryId", entryId)
					.param("limit", TOKENS_MAX_CHUK_SIZE)
					.update());
				logger.info("Deleted {} tokens in iteration {}/{} for id: {}", deleted, i + 1, deleteIterations,
						entryId);
			}
		}
		else {
			this.jdbcClient.sql("DELETE FROM entry_tokens WHERE entry_id = :entryId")
				.param("entryId", entryId)
				.update();
		}
	}

}
