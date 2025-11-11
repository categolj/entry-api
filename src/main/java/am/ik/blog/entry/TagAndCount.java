package am.ik.blog.entry;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

public record TagAndCount(@JsonUnwrapped Tag tag, int count) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().tag(this.tag).count(this.count);
	}

	public static class Builder {

		@Nullable private Tag tag;

		private int count;

		private Builder() {
		}

		public Builder tag(Tag tag) {
			this.tag = tag;
			return this;
		}

		public Builder count(int count) {
			this.count = count;
			return this;
		}

		public TagAndCount build() {
			Assert.notNull(tag, "tag must not be null");
			return new TagAndCount(tag, count);
		}

	}
}
