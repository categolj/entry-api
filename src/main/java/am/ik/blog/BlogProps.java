package am.ik.blog;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog")
public final class BlogProps {

	private TokenizerType tokenizerType = TokenizerType.KUROMOJI;

	private Init init = new Init();

	public TokenizerType getTokenizerType() {
		return tokenizerType;
	}

	public void setTokenizerType(TokenizerType tokenizerType) {
		this.tokenizerType = tokenizerType;
	}

	public Init getInit() {
		return init;
	}

	public void setInit(Init init) {
		this.init = init;
	}

	public enum TokenizerType {

		KUROMOJI, TRIGRAM

	}

	public static final class Init {

		private boolean enabled = false;

		private String tenantId;

		private Fetch fetch = new Fetch();

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getTenantId() {
			return tenantId;
		}

		public void setTenantId(String tenantId) {
			this.tenantId = tenantId;
		}

		public Fetch getFetch() {
			return fetch;
		}

		public void setFetch(Fetch fetch) {
			this.fetch = fetch;
		}

		@Override
		public String toString() {
			return "Init{" + "enabled=" + enabled + ", tenantId='" + tenantId + '\'' + ", fetch=" + fetch + '}';
		}

		public static final class Fetch {

			private int from = 0;

			private int to = 0;

			public int getFrom() {
				return from;
			}

			public void setFrom(int from) {
				this.from = from;
			}

			public int getTo() {
				return to;
			}

			public void setTo(int to) {
				this.to = to;
			}

			@Override
			public String toString() {
				return "Fetch{" + "from=" + from + ", to=" + to + '}';
			}

		}

	}

}
