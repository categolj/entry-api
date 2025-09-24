package am.ik.blog.entry;

public record Category(String name) {

	// Used for deserialization in Jackson
	public static Category valueOf(String category) {
		return new Category(category);
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().name(this.name);
	}

	public static class Builder {

		private String name;

		private Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Category build() {
			return new Category(name);
		}

	}
}
