package am.ik.blog.tokenizer;

import java.util.Set;

@FunctionalInterface
public interface Tokenizer {

	Set<String> tokenize(String text);

}
