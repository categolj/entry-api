package am.ik.blog.entry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record FrontMatter(String title, String summary, List<Category> categories,
		List<Tag> tags) implements Serializable {

	public static String DATE_FIELD = "date";

	public static String UPDATE_FIELD = "updated";

	public FrontMatter(String title, @Nullable String summary, List<Category> categories, List<Tag> tags) {
		this.title = title;
		this.summary = Objects.requireNonNullElse(summary, "");
		this.categories = Objects.requireNonNullElseGet(categories, List::of);
		this.tags = Objects.requireNonNullElseGet(tags, List::of);
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().title(this.title)
			.summary(this.summary)
			.categories(new ArrayList<>(this.categories))
			.tags(new ArrayList<>(this.tags));
	}

	public static class Builder {

		private String title;

		private @Nullable String summary;

		private List<Category> categories = new ArrayList<>();

		private List<Tag> tags = new ArrayList<>();

		private Builder() {
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder summary(@Nullable String summary) {
			this.summary = summary;
			return this;
		}

		public Builder categories(Category... categories) {
			this.categories = List.of(categories);
			return this;
		}

		public Builder categories(List<Category> categories) {
			this.categories = categories;
			return this;
		}

		public Builder tags(Tag... tags) {
			this.tags = List.of(tags);
			return this;
		}

		public Builder tags(List<Tag> tags) {
			this.tags = tags;
			return this;
		}

		public FrontMatter build() {
			return new FrontMatter(title, summary, List.copyOf(categories), List.copyOf(tags));
		}

	}
}