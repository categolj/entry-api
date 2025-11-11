package am.ik.blog.tokenizer;

import com.atilika.kuromoji.ipadic.Token;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class KuromojiTokenizer implements Tokenizer {

	private final com.atilika.kuromoji.ipadic.Tokenizer tokenizer;

	// Maximum token length
	private static final int MAX_TOKEN_LENGTH = 64;

	// Pattern to identify camelCase and PascalCase
	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z]+|[A-Z][a-z]+|[A-Z]+(?=[A-Z][a-z]|\\b))");

	// Two-character meaningful English abbreviations/acronyms to keep
	private static final Set<String> MEANINGFUL_TWO_CHAR_WORDS = Set.of("ai", "cf", "az", "vs", "ok", "ui", "ux", "os",
			"db", "ip", "id", "io", "js", "go", "it", "is", "if", "or", "my", "no", "up", "on", "in", "at", "by", "so",
			"we", "he", "me", "be", "to", "do", "ml", "ci", "cd", "dx", "ex", "ut");

	// Common English stop words to filter out (excluding meaningful two-character words)
	private static final Set<String> ENGLISH_STOP_WORDS = Set.of("the", "and", "a", "that", "have", "i", "for", "not",
			"with", "as", "you", "this", "but", "his", "from", "was", "are", "been", "its", "an", "will", "one", "all",
			"would", "there", "their", "can", "had", "has", "her", "were", "she", "which", "when", "what", "who",
			"where", "why", "how");

	public KuromojiTokenizer() {
		this.tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer.Builder()
			.mode(com.atilika.kuromoji.ipadic.Tokenizer.Mode.SEARCH)
			.build();
	}

	@Override
	public Set<String> tokenize(@Nullable String text) {
		if (text == null || text.trim().isEmpty()) {
			return Set.of();
		}

		Set<String> tokens = new HashSet<>();
		List<Token> kuromojitokens = tokenizer.tokenize(text);

		for (Token token : kuromojitokens) {
			String surface = token.getSurface();

			// Check if token is English or mixed alphanumeric
			if (isEnglishOrAlphanumeric(surface)) {
				// Process as English
				processEnglishToken(surface, tokens);
			}
			else if (shouldIndexJapaneseToken(token)) {
				// Process Japanese token
				String baseForm = token.getBaseForm() != null ? token.getBaseForm() : surface;

				String normalizedToken = normalizeJapanese(baseForm);

				if (!normalizedToken.isEmpty()) {
					// Truncate if too long
					if (normalizedToken.length() > MAX_TOKEN_LENGTH) {
						normalizedToken = normalizedToken.substring(0, MAX_TOKEN_LENGTH);
					}
					tokens.add(normalizedToken);
				}
			}
		}

		return Set.copyOf(tokens);
	}

	/**
	 * Check if the token is English or contains alphanumeric characters
	 */
	private boolean isEnglishOrAlphanumeric(String token) {
		// Check if token contains any English letters
		return token.matches(".*[a-zA-Z].*");
	}

	/**
	 * Process English and alphanumeric tokens
	 */
	private void processEnglishToken(String token, Set<String> tokens) {
		// Normalize the token first
		String normalized = normalizeEnglish(token);

		// Skip if too short after normalization, but allow meaningful two-character words
		if (normalized.length() < 2 || (normalized.length() == 2 && !isMeaningfulTwoCharWord(normalized))) {
			return;
		}

		// Skip if less than 3 characters and not a meaningful two-character word
		if (normalized.length() < 3 && !isMeaningfulTwoCharWord(normalized)) {
			return;
		}

		// Truncate if too long
		if (normalized.length() > MAX_TOKEN_LENGTH) {
			normalized = normalized.substring(0, MAX_TOKEN_LENGTH);
		}

		// Handle camelCase and PascalCase (e.g., "JavaScript", "getElementById")
		if (containsCamelCase(token)) {
			// Add the whole word
			if ((normalized.length() >= 3 || isMeaningfulTwoCharWord(normalized))
					&& !isCommonEnglishStopWord(normalized)) {
				tokens.add(normalized);
			}

			// Also split and add parts for better search coverage
			Matcher matcher = CAMEL_CASE_PATTERN.matcher(token);
			while (matcher.find()) {
				String part = matcher.group().toLowerCase();
				if ((part.length() >= 3 || isMeaningfulTwoCharWord(part)) && !isCommonEnglishStopWord(part)) {
					// Truncate if too long
					if (part.length() > MAX_TOKEN_LENGTH) {
						part = part.substring(0, MAX_TOKEN_LENGTH);
					}
					tokens.add(part);
				}
			}
		}
		else {
			// Regular English word
			if (!isCommonEnglishStopWord(normalized)) {
				tokens.add(normalized);
			}
		}
	}

	/**
	 * Check if string contains camelCase or PascalCase
	 */
	private boolean containsCamelCase(String token) {
		// Has both uppercase and lowercase letters
		return token.matches(".*[a-z].*") && token.matches(".*[A-Z].*");
	}

	/**
	 * Normalize English tokens
	 */
	private String normalizeEnglish(String token) {
		if (token == null) {
			return "";
		}

		// Convert to lowercase
		String normalized = token.toLowerCase();

		// Remove non-alphanumeric characters from edges
		normalized = normalized.replaceAll("^[^a-z0-9]+", "");
		normalized = normalized.replaceAll("[^a-z0-9]+$", "");

		// Remove possessive 's
		normalized = normalized.replaceAll("'s$", "");

		return normalized.trim();
	}

	/**
	 * Check if the word is a common English stop word
	 */
	private boolean isCommonEnglishStopWord(String word) {
		return ENGLISH_STOP_WORDS.contains(word);
	}

	/**
	 * Check if the word is a meaningful two-character word/abbreviation
	 */
	private boolean isMeaningfulTwoCharWord(String word) {
		return word.length() == 2 && MEANINGFUL_TWO_CHAR_WORDS.contains(word);
	}

	/**
	 * Determine if the Japanese token should be indexed based on its part of speech
	 */
	private boolean shouldIndexJapaneseToken(Token token) {
		String pos = token.getPartOfSpeechLevel1();

		// Parts of speech to index
		switch (pos) {
			case "名詞": // Noun
				String subPos = token.getPartOfSpeechLevel2();
				return !subPos.equals("非自立") && // non-independent
						!subPos.equals("代名詞") && // pronoun
						!subPos.equals("数"); // number
			case "動詞": // Verb
				// Only independent verbs
				return token.getPartOfSpeechLevel2().equals("自立");
			case "形容詞": // Adjective
			case "副詞": // Adverb
				return true;
			case "記号": // Symbol - check if it's actually English
				String surface = token.getSurface();
				return isEnglishOrAlphanumeric(surface) && surface.length() >= 2;
			default:
				return false;
		}
	}

	/**
	 * Normalize Japanese tokens for consistent indexing
	 */
	private String normalizeJapanese(String token) {
		if (token == null) {
			return "";
		}

		// Convert full-width alphanumeric to half-width
		String normalized = convertFullWidthToHalfWidth(token);

		// Convert to lowercase for any embedded English
		normalized = normalized.toLowerCase().trim();

		// Exclude single character tokens (e.g., Japanese particles)
		if (normalized.length() <= 1) {
			return "";
		}

		// Exclude number-only tokens
		if (normalized.matches("^[0-9]+$")) {
			return "";
		}

		// Exclude punctuation-only tokens
		if (normalized.matches("^[\\p{Punct}]+$")) {
			return "";
		}

		return normalized;
	}

	/**
	 * Convert full-width alphanumeric characters to half-width
	 */
	private String convertFullWidthToHalfWidth(String input) {
		StringBuilder result = new StringBuilder();

		for (char c : input.toCharArray()) {
			if (c >= 'Ａ' && c <= 'Ｚ') {
				// Full-width uppercase to half-width uppercase
				result.append((char) (c - 'Ａ' + 'A'));
			}
			else if (c >= 'ａ' && c <= 'ｚ') {
				// Full-width lowercase to half-width lowercase
				result.append((char) (c - 'ａ' + 'a'));
			}
			else if (c >= '０' && c <= '９') {
				// Full-width digit to half-width digit
				result.append((char) (c - '０' + '0'));
			}
			else {
				// Keep other characters as-is
				result.append(c);
			}
		}

		return result.toString();
	}

}