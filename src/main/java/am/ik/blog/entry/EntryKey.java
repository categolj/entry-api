package am.ik.blog.entry;

import am.ik.csv.Csv;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record EntryKey(Long entryId, String tenantId) implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Csv csv = Csv.builder().delimiter("|").build();

	public static String DEFAULT_TENANT_ID = "_";

	public EntryKey(Long entryId, @Nullable String tenantId) {
		this.entryId = entryId;
		this.tenantId = Objects.requireNonNullElse(tenantId, DEFAULT_TENANT_ID);
	}

	public EntryKey(Long entryId) {
		this(entryId, DEFAULT_TENANT_ID);
	}

	@Override
	public String toString() {
		return (tenantId == null || DEFAULT_TENANT_ID.equals(tenantId)) ? Entry.formatId(entryId)
				: csv.joinLine(Entry.formatId(entryId), tenantId);
	}

	public static EntryKey valueOf(String value) {
		List<String> strings = csv.splitLine(value);
		try {
			if (strings.size() == 1) {
				return new EntryKey(Long.parseLong(strings.getFirst()), null);
			}
			else if (strings.size() == 2) {
				return new EntryKey(Long.parseLong(strings.get(0)), strings.get(1));
			}
			else {
				throw new IllegalArgumentException("Invalid EntryKey format: " + value);
			}
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid EntryKey format: " + value, e);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder().entryId(this.entryId).tenantId(this.tenantId);
	}

	public static class Builder {

		private Long entryId;

		private @Nullable String tenantId;

		private Builder() {
		}

		public Builder entryId(Long entryId) {
			this.entryId = entryId;
			return this;
		}

		public Builder tenantId(@Nullable String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		public EntryKey build() {
			return new EntryKey(entryId, tenantId);
		}

	}

}