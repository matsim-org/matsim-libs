package org.matsim.simwrapper;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Functional interface extension of the dashboard interface that allows to construct comparison dashboards.
 */

//@FunctionalInterface
public interface ComparisonDashboard extends Dashboard{


	Map<ComparisonDashboard, String> PATH_STORAGE = new WeakHashMap<>();

	default String getPathToBaseCase() {
		return PATH_STORAGE.get(this);
	}

	default void setPathToBaseCase(String path) {
		PATH_STORAGE.put(this, path);
	}

	/**
	 * Wrapper around an existing dashboard that allows to customize some of the attributes.
	 */
	final class Customizable implements ComparisonDashboard {

		private final ComparisonDashboard delegate;
		private String context;

		private String title;
		private String description;
		private String pathToBaseCase;

		@Override
		public String getPathToBaseCase() {
			return pathToBaseCase;
		}		private Double priority;


		@Override
		public void setPathToBaseCase(String path) {
			this.pathToBaseCase = path;
		}
		private Customizable(ComparisonDashboard delegate) {
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

	}
}
