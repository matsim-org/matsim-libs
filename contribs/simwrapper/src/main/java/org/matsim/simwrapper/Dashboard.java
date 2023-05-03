package org.matsim.simwrapper;

/**
 * Function interface that allows to construct dashboards.
 */
@FunctionalInterface
public interface Dashboard {

	/**
	 * Method to configure a dashboard.
	 */
	void configure(Header header, Layout layout);

	interface Customizable extends Dashboard {

		/**
		 * Set the title of this dashboard
		 * @return same instance
		 */
		default Customizable withTitle(String title) {
			return this;
		}

	}

}
