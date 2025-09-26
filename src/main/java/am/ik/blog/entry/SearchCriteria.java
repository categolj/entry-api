package am.ik.blog.entry;

import java.util.ArrayList;
import java.util.List;

public record SearchCriteria(String query, List<String> categories, String tag) {

	public static SearchCriteria NULL_CRITERIA = new SearchCriteria(null, null, null);

	public boolean isDefault() {
		return (this.query == null || this.query.isBlank()) && (this.categories == null || this.categories.isEmpty())
				&& (this.tag == null || this.tag.isBlank());
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().query(this.query).categories(new ArrayList<>(this.categories)).tag(this.tag);
	}

	public static class Builder {

		private String query;

		private List<String> categories = new ArrayList<>();

		private String tag;

		private Builder() {
		}

		public Builder query(String query) {
			this.query = query;
			return this;
		}

		public Builder categories(List<String> categories) {
			this.categories = categories;
			return this;
		}

		public Builder tag(String tag) {
			this.tag = tag;
			return this;
		}

		public SearchCriteria build() {
			return new SearchCriteria(query, List.copyOf(categories), tag);
		}

	}
}
