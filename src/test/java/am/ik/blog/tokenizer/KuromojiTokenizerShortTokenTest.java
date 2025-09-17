package am.ik.blog.tokenizer;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KuromojiTokenizerShortTokenTest {

	private KuromojiTokenizer tokenizer;

	@BeforeEach
	void setUp() {
		tokenizer = new KuromojiTokenizer();
	}

	@Test
	void testShortEnglishTokensAreExcluded() {
		// Given: Text with short English tokens (1-2 characters) and some meaningful ones
		String text = "I am a go to it and be or if so we up an my on at in";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Meaningful two-character words should be included, others excluded
		assertThat(tokens).contains("go", "to", "it", "be", "or", "if", "so", "we", "up", "my", "on", "at", "in");
		assertThat(tokens).doesNotContain("i", "am", "a", "an"); // Single char or
																	// non-meaningful
	}

	@Test
	void testThreeCharacterEnglishTokensAreIncluded() {
		// Given: Text with 3+ character English tokens
		String text = "the cat and dog run for you but not";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: 3+ character tokens should be included (except stop words)
		assertThat(tokens).contains("cat", "dog", "run");
		assertThat(tokens).doesNotContain("the", "and", "for", "you", "but", "not"); // These
																						// are
																						// stop
																						// words
	}

	@Test
	void testMixedLengthEnglishTokens() {
		// Given: Text with mixed length English tokens
		String text = "A big cat ate my fresh apple pie today";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: 3+ character tokens and meaningful 2-character tokens should be included
		assertThat(tokens).contains("big", "cat", "ate", "my", "fresh", "apple", "pie", "today");
		assertThat(tokens).doesNotContain("a"); // Single character excluded
	}

	@Test
	void testCamelCaseWithShortParts() {
		// Given: CamelCase with some short parts
		String text = "getElementById myApp aProp";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: 3+ character parts and meaningful 2-character parts should be included
		assertThat(tokens).contains("getelementbyid", "get", "element", "by", "id", "myapp", "my", "app", "aprop",
				"prop");
	}

	@Test
	void testJapaneseTokensNotAffected() {
		// Given: Japanese text with mixed English
		String text = "これはテストです。I go to school。頑張って。";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Japanese tokens should not be affected by the character rule
		assertThat(tokens).contains("テスト", "school", "頑張る", "go", "to");
		assertThat(tokens).doesNotContain("i"); // Single character excluded
	}

	@Test
	void testTwoCharacterEnglishHandling() {
		// Given: Text with exactly 2-character English words (mix of meaningful and
		// non-meaningful)
		String text = "go in on up at by my ai cf vs ui";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Meaningful 2-character words should be included
		assertThat(tokens).contains("go", "in", "on", "up", "at", "by", "my", "ai", "cf", "vs", "ui");
	}

	@Test
	void testMeaningfulAbbreviations() {
		// Given: Text with common technical abbreviations
		String text = "AI vs ML and DB with UI/UX design using JS and Go";

		// When
		Set<String> tokens = tokenizer.tokenize(text);

		// Then: Technical abbreviations should be included
		assertThat(tokens).contains("ai", "vs", "ml", "db", "ui", "ux", "design", "using", "js", "go");
	}

}