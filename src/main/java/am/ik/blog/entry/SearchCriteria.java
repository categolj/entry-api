package am.ik.blog.entry;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record SearchCriteria(@Nullable String query, @Nullable List<String> categories, @Nullable String tag) {

	public static SearchCriteria NULL_CRITERIA = new SearchCriteria(null, null, null);

	public boolean isDefault() {
		return (this.query == null || this.query.isBlank()) && (this.categories == null || this.categories.isEmpty())
				&& (this.tag == null || this.tag.isBlank());
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().query(this.query)
			.categories(this.categories == null ? new ArrayList<>() : new ArrayList<>(this.categories))
			.tag(this.tag);
	}

	public static class Builder {

		@Nullable private String query;

		private List<String> categories = new ArrayList<>();

		@Nullable private String tag;

		private Builder() {
		}

		public Builder query(@Nullable String query) {
			this.query = query;
			return this;
		}

		public Builder categories(List<String> categories) {
			this.categories = categories;
			return this;
		}

		public Builder tag(@Nullable String tag) {
			this.tag = tag;
			return this;
		}

		public SearchCriteria build() {
			return new SearchCriteria(query, categories == null ? new ArrayList<>() : List.copyOf(categories), tag);
		}

	}
}
