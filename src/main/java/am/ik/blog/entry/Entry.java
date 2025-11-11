package am.ik.blog.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public record Entry(@JsonUnwrapped EntryKey entryKey, FrontMatter frontMatter,
		@Nullable @JsonInclude(JsonInclude.Include.NON_NULL) String content, Author created, Author updated) {

	public String toMarkdown() {
		return """
				---
				title: %s%s
				tags: %s
				categories: %s%s%s
				---

				%s
				""".formatted(frontMatter.title(),
				StringUtils.hasLength(frontMatter.summary()) ? "%nsummary: %s".formatted(frontMatter.summary()) : "",
				frontMatter.tags().stream().map(t -> "\"%s\"".formatted(t.name())).toList(),
				frontMatter.categories().stream().map(c -> "\"%s\"".formatted(c.name())).toList(),
				created.date() == null ? "" : "%n%s: %s".formatted(FrontMatter.DATE_FIELD, created.date()),
				updated.date() == null ? "" : "%n%s: %s".formatted(FrontMatter.UPDATE_FIELD, updated.date()), content);
	}

	public static Long parseId(String fileName) {
		return Long.parseLong(fileName.replace(".md", "").replace(".markdown", ""));
	}

	@JsonIgnore
	public String formatId() {
		return Entry.formatId(entryKey().entryId());
	}

	public static String formatId(Long entryId) {
		return "%05d".formatted(entryId);
	}

	@Nullable public Instant toCursor() {
		if (updated == null) {
			return null;
		}
		return updated.date();
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().entryKey(this.entryKey)
			.frontMatter(this.frontMatter)
			.content(this.content)
			.created(this.created)
			.updated(this.updated);
	}

	public static class Builder {

		@Nullable private EntryKey entryKey;

		@Nullable private FrontMatter frontMatter;

		@Nullable private String content;

		@Nullable private Author created;

		@Nullable private Author updated;

		private Builder() {
		}

		public Builder entryKey(EntryKey entryKey) {
			this.entryKey = entryKey;
			return this;
		}

		public Builder frontMatter(FrontMatter frontMatter) {
			this.frontMatter = frontMatter;
			return this;
		}

		public Builder content(@Nullable String content) {
			this.content = content;
			return this;
		}

		public Builder created(Author created) {
			this.created = created;
			return this;
		}

		public Builder updated(Author updated) {
			this.updated = updated;
			return this;
		}

		public Entry build() {
			Assert.notNull(entryKey, "entryKey must not be null");
			Assert.notNull(frontMatter, "frontMatter must not be null");
			Assert.notNull(created, "created must not be null");
			Assert.notNull(updated, "updated must not be null");
			return new Entry(entryKey, frontMatter, content, created, updated);
		}

	}
}
