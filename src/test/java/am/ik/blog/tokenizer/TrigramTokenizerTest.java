package am.ik.blog.tokenizer;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrigramTokenizerTest {

	private TrigramTokenizer tokenizer;

	@BeforeEach
	void setUp() {
		tokenizer = new TrigramTokenizer();
	}

	@Test
	void tokenize_basicHiragana() {
		String text = "こんにちは";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("こんに", "んにち", "にちは").hasSize(3);
	}

	@Test
	void tokenize_katakanaToHiragana() {
		String text = "コンニチハ";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("こんに", "んにち", "にちは").hasSize(3);
	}

	@Test
	void tokenize_uppercaseToLowercase() {
		String text = "Hello";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("hel", "ell", "llo").hasSize(3);
	}

	@Test
	void tokenize_fullWidthToHalfWidth() {
		String text = "Ｈｅｌｌｏ";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("hel", "ell", "llo").hasSize(3);
	}

	@Test
	void tokenize_withPunctuationAndJapanese() {
		String text = "こんにちは、世界！";
		Set<String> result = tokenizer.tokenize(text);
		// Trigrams include punctuation (full-width ！ is normalized to half-width !)
		assertThat(result).contains("こんに", "んにち", "にちは", "ちは、", "は、世", "、世界", "世界!").hasSize(7);
	}

	@Test
	void tokenize_duplicateTrigramsGetCounted() {
		String text = "あああああ";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("あああ").hasSize(1);
	}

	@Test
	void tokenize_mixedContent() {
		String text = "Javaプログラミング123";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("jav", "ava", "vaぷ", "aぷろ", "ぷろぐ", "ろぐら", "ぐらみ", "らみん", "みんぐ", "んぐ1", "ぐ12")
			.hasSize(11);
	}

	@Test
	void tokenize_numbersOnlyFiltered() {
		String text = "123456";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).isEmpty();
	}

	@Test
	void tokenize_emptyString() {
		String text = "";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).isEmpty();
	}

	@Test
	void tokenize_spacesOnlyString() {
		String text = "   ";
		Set<String> result = tokenizer.tokenize(text);
		// Whitespace-only trigrams are excluded by isValidNgram
		assertThat(result).isEmpty();
	}

	@Test
	void tokenize_shortString() {
		String text = "ab";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).isEmpty();
	}

	@Test
	void tokenize_exactlyThreeCharacters() {
		String text = "abc";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("abc").hasSize(1);
	}

	@Test
	void tokenize_withSpacesAndPunctuation() {
		String text = "Hello, World! こんにちは、世界！";
		Set<String> result = tokenizer.tokenize(text);
		// Trigrams include punctuation and spaces
		assertThat(result).contains("hel", "ell", "llo", "lo,", "o, ", ", w", " wo", "wor", "orl", "rld", "ld!", "d! ",
				"! こ", " こん", "こんに", "んにち", "にちは", "ちは、", "は、世", "、世界", "世界!");
	}

	@Test
	void tokenize_japaneseWithNumbers() {
		String text = "第1章プログラミング";
		Set<String> result = tokenizer.tokenize(text);
		assertThat(result).contains("第1章", "1章ぷ", "章ぷろ", "ぷろぐ", "ろぐら", "ぐらみ", "らみん", "みんぐ");
	}

}