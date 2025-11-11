package am.ik.blog.entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public record Author(String name, @Nullable Instant date) {

	@JsonIgnore
	public String rfc1123DateTime() {
		if (this.date == null) {
			return "";
		}
		return RFC_1123_DATE_TIME.format(this.date.atOffset(ZoneOffset.UTC));
	}

	public Author withDate(Instant date) {
		return new Author(this.name, date);
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().name(this.name).date(this.date);
	}

	public static class Builder {

		@Nullable private String name;

		@Nullable private Instant date;

		private Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder date(@Nullable Instant date) {
			this.date = date;
			return this;
		}

		public Author build() {
			Assert.hasText(this.name, "name must not be empty");
			return new Author(name, date);
		}

	}
}
