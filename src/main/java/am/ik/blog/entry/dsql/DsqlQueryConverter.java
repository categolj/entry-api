package am.ik.blog.entry.dsql;

import am.ik.blog.tokenizer.Tokenizer;
import am.ik.query.Query;
import am.ik.query.ast.AndNode;
import am.ik.query.ast.FieldNode;
import am.ik.query.ast.FuzzyNode;
import am.ik.query.ast.NodeVisitor;
import am.ik.query.ast.NotNode;
import am.ik.query.ast.OrNode;
import am.ik.query.ast.PhraseNode;
import am.ik.query.ast.RangeNode;
import am.ik.query.ast.RootNode;
import am.ik.query.ast.TokenNode;
import am.ik.query.ast.WildcardNode;
import am.ik.query.lexer.TokenType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class DsqlQueryConverter implements NodeVisitor<String> {

	private final Tokenizer tokenizer;

	public DsqlQueryConverter(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	// Result record containing parameterized SQL and parameters
	public record SqlResult(String whereClause, Map<String, Object> parameters) {
	}

	private final Map<String, Object> parameters = new HashMap<>();

	private int paramCounter = 1;

	public SqlResult convertToSql(Query query) {
		parameters.clear();
		paramCounter = 1;

		if (query.isEmpty()) {
			return new SqlResult("1=1", Map.of()); // Always true condition
		}

		String sql = query.accept(this);
		return new SqlResult(sql, parameters);
	}

	@Override
	public String visitRoot(RootNode node) {
		return node.children()
			.stream()
			.map(child -> child.accept(this))
			.filter(sql -> !sql.isEmpty())
			.collect(Collectors.joining(" AND "));
	}

	@Override
	public String visitAnd(AndNode node) {
		String result = node.children()
			.stream()
			.map(child -> child.accept(this))
			.filter(sql -> !sql.isEmpty())
			.map(x -> "(" + x + ")")
			.collect(Collectors.joining(" AND "));
		return node.children().size() > 1 ? "(" + result + ")" : result;
	}

	@Override
	public String visitOr(OrNode node) {
		String result = node.children()
			.stream()
			.map(child -> child.accept(this))
			.filter(sql -> !sql.isEmpty())
			.map(x -> "(" + x + ")")
			.collect(Collectors.joining(" OR "));
		return "(" + result + ")";
	}

	@Override
	public String visitNot(NotNode node) {
		// Handle NOT of TokenNode as exclusion (generates NOT LIKE directly)
		if (node.child() instanceof TokenNode tokenNode && tokenNode.type() == TokenType.KEYWORD) {
			return createLikeClause(tokenNode.value(), true);
		}

		String childSql = node.child().accept(this);
		return childSql.isEmpty() ? "" : "NOT " + childSql;
	}

	@Override
	public String visitToken(TokenNode node) {
		return switch (node.type()) {
			case KEYWORD -> createLikeClause(node.value(), false);
			case EXCLUDE -> createLikeClause(node.value(), true);
			default -> "";
		};
	}

	@Override
	public String visitPhrase(PhraseNode node) {
		return createLikeClause(node.phrase(), false);
	}

	private String createLikeClause(String value, boolean negated) {
		int index = paramCounter++;
		String paramName = "tokens" + index;
		String sizeName = "tokensSize" + index;
		Set<String> tokens = this.tokenizer.tokenize(value);
		if (tokens.isEmpty()) {
			return "1=2";
		}
		parameters.put(paramName, tokens);
		parameters.put(sizeName, tokens.size());
		return "e.id %sIN (SELECT entry_id FROM entry_tokens WHERE token IN (:%s) GROUP BY entry_id HAVING COUNT(DISTINCT token) = :%s)"
			.formatted(negated ? "NOT " : "", paramName, sizeName);
	}

	// Ignore field queries, wildcards, etc. for this simple example
	@Override
	public String visitField(FieldNode node) {
		return "";
	}

	@Override
	public String visitWildcard(WildcardNode node) {
		return "";
	}

	@Override
	public String visitFuzzy(FuzzyNode node) {
		return "";
	}

	@Override
	public String visitRange(RangeNode node) {
		return "";
	}

}
