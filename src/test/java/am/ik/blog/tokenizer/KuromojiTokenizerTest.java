package am.ik.blog.tokenizer;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KuromojiTokenizer Tests with English Support")
class KuromojiTokenizerTest {

	private KuromojiTokenizer tokenizer;

	@BeforeEach
	void setUp() {
		tokenizer = new KuromojiTokenizer();
	}

	@Nested
	@DisplayName("English Text Tests")
	class EnglishTextTests {

		@Test
		@DisplayName("Should tokenize pure English text")
		void testPureEnglishText() {
			// Given
			String text = "Spring Boot is a popular Java framework for building web applications";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens)
				.contains("spring", "boot", "is", "popular", "java", "framework", "building", "web", "applications")
				.doesNotContain("a", "for", "the");
		}

		@Test
		@DisplayName("Should handle camelCase and PascalCase")
		void testCamelCaseHandling() {
			// Given
			String text = "JavaScript getElementById XMLHttpRequest";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("javascript", "java", "script", "getelementbyid", "element", "xmlhttprequest",
					"xml", "http", "request");
		}

		@Test
		@DisplayName("Should filter English stop words")
		void testEnglishStopWordFiltering() {
			// Given
			String text = "The quick brown fox jumps over the lazy dog";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("quick", "brown", "fox", "jumps", "over", "lazy", "dog").doesNotContain("the");
		}

	}

	@Nested
	@DisplayName("Mixed Japanese-English Tests")
	class MixedLanguageTests {

		@Test
		@DisplayName("Should handle Japanese text with English technical terms")
		void testJapaneseWithEnglishTerms() {
			// Given
			String text = "Spring BootでRESTful APIを開発する";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("spring", "boot", "restful", "api", "開発");
		}

		@Test
		@DisplayName("Should handle programming article text")
		void testProgrammingArticle() {
			// Given
			String text = "JavaのStreamAPIとLambda式を使ってコレクションを処理します";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("java", "streamapi", "stream", "api", "lambda", "使う", "コレクション", "処理");
		}

		@Test
		@DisplayName("Should handle IT company names and products")
		void testITCompanyAndProductNames() {
			// Given
			String text = "GoogleのCloud PlatformとAmazonのAWSを比較する";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("google", "cloud", "platform", "amazon", "aws", "比較");
		}

		@Test
		@DisplayName("Should handle URLs and email addresses")
		void testUrlsAndEmails() {
			// Given
			String text = "詳細はhttps://example.comまたはsupport@example.comまで";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("詳細", "https", "example", "com", "support");
		}

	}

	@Nested
	@DisplayName("Technical Documentation Tests")
	class TechnicalDocumentationTests {

		@Test
		@DisplayName("Should tokenize database-related content")
		void testDatabaseContent() {
			// Given
			String text = "PostgreSQLでCREATE INDEXを実行してBツリーインデックスを作成する";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("postgresql", "postgre", "sql", "create", "index", "実行", "ツリー", "インデックス", "作成");
		}

		@Test
		@DisplayName("Should handle version numbers and technical specifications")
		void testVersionNumbers() {
			// Given
			String text = "Java 17とSpring Boot 3.0の新機能について";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("java", "spring", "boot", "機能");
		}

		@Test
		@DisplayName("Should tokenize code snippets and method names")
		void testCodeSnippets() {
			// Given
			String text = "getUserById()メソッドでOptional<User>を返す";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("getuserbyid", "get", "user", "メソッド", "optional", "返す");
		}

	}

	@Nested
	@DisplayName("Edge Cases with English")
	class EnglishEdgeCases {

		@Test
		@DisplayName("Should handle abbreviations and acronyms")
		void testAbbreviations() {
			// Given
			String text = "API REST JSON XML HTML CSS";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("api", "rest", "json", "xml", "html", "css");
		}

		@Test
		@DisplayName("Should normalize different cases of same word")
		void testCaseNormalization() {
			// Given
			String text = "java Java JAVA JavaScriptのJava";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("java", "javascript", "script");
		}

		@Test
		@DisplayName("Should handle possessives and contractions")
		void testPossessivesAndContractions() {
			// Given
			String text = "Google's CloudとAmazon's AWSとMicrosoft's Azure";

			// When
			Set<String> tokens = tokenizer.tokenize(text);

			// Then
			assertThat(tokens).contains("google", "amazon", "microsoft", "cloud", "aws", "azure")
				.doesNotContain("google's", "amazon's", "microsoft's");
		}

	}

}