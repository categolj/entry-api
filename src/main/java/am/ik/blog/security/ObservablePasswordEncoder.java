package am.ik.blog.security;

import io.micrometer.observation.annotation.Observed;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;

@Observed
public class ObservablePasswordEncoder implements PasswordEncoder {

	private final PasswordEncoder delegate;

	public ObservablePasswordEncoder(@NonNull PasswordEncoder delegate) {
		this.delegate = delegate;
	}

	@Override
	public @Nullable String encode(@Nullable CharSequence rawPassword) {
		return this.delegate.encode(rawPassword);
	}

	@Override
	public boolean matches(@Nullable CharSequence rawPassword, @Nullable String encodedPassword) {
		return this.delegate.matches(rawPassword, encodedPassword);
	}

}
