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

}
