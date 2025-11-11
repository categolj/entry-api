package am.ik.blog.tokenizer;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class TrigramTokenizer implements Tokenizer {

	@Override
	// Generate tri-grams from the input text
	public Set<String> tokenize(@Nullable String text) {
		if (text == null || text.trim().isEmpty()) {
			return Set.of();
		}
		Set<String> ngrams = new HashSet<>();
		// Normalize text: convert full-width to half-width, katakana to hiragana
		String normalized = normalize(text);
		// Generate tri-grams (3 characters)
		for (int i = 0; i < normalized.length() - 2; i++) {
			String trigram = normalized.substring(i, i + 3);
			if (isValidNgram(trigram)) {
				ngrams.add(trigram);
			}
		}
		return Set.copyOf(ngrams);
	}

	private String normalize(String text) {
		// Convert to lowercase
		text = text.toLowerCase(Locale.ENGLISH);
		// Convert full-width alphanumeric characters to half-width
		text = Normalizer.normalize(text, Normalizer.Form.NFKC);
		// Convert katakana to hiragana for search flexibility
		StringBuilder sb = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (c >= 'ァ' && c <= 'ヶ') {
				sb.append((char) (c - 'ァ' + 'ぁ'));
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private boolean isValidNgram(String ngram) {
		// Exclude n-grams that are only whitespace or only digits
		return !ngram.trim().isEmpty() && !ngram.matches("^[0-9]+$");
	}

}
