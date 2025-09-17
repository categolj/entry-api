package am.ik.blog.entry;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record TagAndCount(@JsonUnwrapped Tag tag, int count) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().tag(this.tag).count(this.count);
	}

	public static class Builder {

		private Tag tag;

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
			return new TagAndCount(tag, count);
		}

	}
}
