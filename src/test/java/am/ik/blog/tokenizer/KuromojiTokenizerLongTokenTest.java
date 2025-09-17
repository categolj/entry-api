package am.ik.blog.tokenizer;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KuromojiTokenizerLongTokenTest {

	private KuromojiTokenizer tokenizer;

	@BeforeEach
	void setUp() {
		tokenizer = new KuromojiTokenizer();
	}

	@Test
	void testLongEnglishTokenTruncation() {
		// Given: A very long English word (over 64 characters)
		String longWord = "a".repeat(100); // 100 characters
		String text = "Hello " + longWord + " world";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: The long word should be truncated to 64 characters
		String expectedTruncated = "a".repeat(64);
		assertThat(tokens).contains("hello", expectedTruncated, "world");
		assertThat(tokens).doesNotContain(longWord); // Original long word should not be
														// present
	}

	@Test
	void testLongJapaneseWordTruncation() {
		// Given: A very long meaningful Japanese word (over 64 characters)
		String longJapanese = "プログラミング".repeat(20); // Over 64 characters
		String text = "これは" + longJapanese + "です";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Check that no token exceeds 64 characters
		assertThat(tokens).allMatch(token -> token.length() <= 64);
		assertThat(tokens).anyMatch(token -> token.contains("プログラミング"));
	}

	@Test
	void testExactly64CharactersToken() {
		// Given: A token that is exactly 64 characters
		String exactly64 = "a".repeat(64);
		String text = "word " + exactly64 + " end";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: The 64-character token should be preserved
		assertThat(tokens).contains("word", exactly64, "end");
	}

	@Test
	void testCamelCasePartTruncation() {
		// Given: A camelCase word with a very long part
		String longPart = "VeryLong" + "A".repeat(80) + "MethodName";
		String text = "call" + longPart + "Function";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: All tokens should be within the 64 character limit
		assertThat(tokens).allMatch(token -> token.length() <= 64);
	}

	@Test
	void testLongTokensAreTruncatedNotSkipped() {
		// Given: A very long word that would be over 64 characters
		String veryLongWord = "supercalifragilisticexpialidocious".repeat(3); // Much
																				// longer
																				// than 64
		String text = "start " + veryLongWord + " end";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Should contain start, end, and a truncated version of the long word
		assertThat(tokens).contains("start", "end");
		assertThat(tokens).anyMatch(token -> token.length() == 64);
		assertThat(tokens).noneMatch(token -> token.length() > 64);
	}

}