package am.ik.blog.tokenizer;

import java.util.Set;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface Tokenizer {

	Set<String> tokenize(@Nullable String text);

}
