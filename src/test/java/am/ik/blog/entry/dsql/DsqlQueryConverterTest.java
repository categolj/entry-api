package am.ik.blog.entry.dsql;

import am.ik.blog.entry.dsql.DsqlQueryConverter.SqlResult;
import am.ik.blog.tokenizer.KuromojiTokenizer;
import am.ik.query.parser.QueryParser;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DsqlQueryConverterTest {

	QueryParser queryParser = QueryParser.create();

	DsqlQueryConverter queryConverter = new DsqlQueryConverter(new KuromojiTokenizer());

	@Test
	void convertAnd() {
		SqlResult converted = queryConverter.convertToSql(queryParser.parse("hello spring-boot"));
		assertThat(converted.whereClause()).isEqualTo(
				"((e.id IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens1) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize1)) AND (e.id IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens2) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize2)))");
		assertThat(converted.parameters()).containsAllEntriesOf(Map.of("tokens1", Set.of("hello"), "tokensSize1", 1,
				"tokens2", Set.of("spring", "boot"), "tokensSize2", 2));
	}

	@Test
	void convertAndNot() {
		SqlResult converted = queryConverter.convertToSql(queryParser.parse("hello -world"));
		assertThat(converted.whereClause()).isEqualTo(
				"((e.id IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens1) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize1)) AND (e.id NOT IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens2) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize2)))");
		assertThat(converted.parameters()).containsAllEntriesOf(
				Map.of("tokens1", Set.of("hello"), "tokensSize1", 1, "tokens2", Set.of("world"), "tokensSize2", 1));
	}

	@Test
	void convertOr() {
		SqlResult converted = queryConverter.convertToSql(queryParser.parse("hello or world"));
		assertThat(converted.whereClause()).isEqualTo(
				"((e.id IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens1) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize1)) OR (e.id IN (SELECT entry_id FROM entry_tokens WHERE token IN (:tokens2) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :tokensSize2)))");
		assertThat(converted.parameters()).containsAllEntriesOf(
				Map.of("tokens1", Set.of("hello"), "tokensSize1", 1, "tokens2", Set.of("world"), "tokensSize2", 1));
	}

}