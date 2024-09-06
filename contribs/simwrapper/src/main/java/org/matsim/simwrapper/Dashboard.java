package org.matsim.simwrapper;

/**
 * Function interface that allows to construct dashboards.
 */
@FunctionalInterface
public interface Dashboard {

	/**
	 * Wrap an existing dashboard to customize its configuration.
	 */
	static Customizable customize(Dashboard d) {
		return new Customizable(d);
	}

	/**
	 * Method to configure a dashboard.
	 */
	void configure(Header header, Layout layout);

	/**
	 * Dashboards are ordered by priority, while higher priority means they appear in the front.
	 * Default ist 0.
	 */
	default double priority() {
		return 0;
	}

	/**
	 * Default context of a dashboard. Using different context values allows to have multiple dashboards of the same type, but with different configurations.
	 */
	default String context() {
		return "";
	}

	/**
	 * Wrapper around an existing dashboard that allows to customize some of the attributes.
	 */
	final class Customizable implements Dashboard {

		private final Dashboard delegate;
		private Double priority;
		private String context;

		private String title;
		private String description;

		private Customizable(Dashboard delegate) {
			this.delegate = delegate;
		}

		@Override
		public void configure(Header header, Layout layout) {
			delegate.configure(header, layout);
			if (title != null)
				header.title = title;
			if (description != null)
				header.description = description;
		}

		@Override
		public double priority() {
			if (priority != null)
				return priority;
			return delegate.priority();
		}

		/**
		 * Overwrite priority setting.
		 */
		public Customizable priority(double priority) {
			this.priority = priority;
			return this;
		}

		@Override
		public String context() {
			if (context != null)
				return context;
			return delegate.context();
		}

		/**
		 * Overwrites the context setting.
		 */
		public Customizable context(String context) {
			this.context = context;
			return this;
		}

		/**
		 * Overwrite the default title.
		 */
		public Customizable title(String title) {
			this.title = title;
			return this;
		}

		/**
		 * Overwrite the default description.
		 */
		public Customizable description(String description) {
			this.description = description;
			return this;
		}
	}
}
