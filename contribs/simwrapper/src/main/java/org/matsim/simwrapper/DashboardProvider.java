package org.matsim.simwrapper;

import org.matsim.core.config.Config;

import java.util.List;

/**
 * Interface for classes that provide default {@link Dashboard}s that should be added to simulation runs automatically.
 * One dashboard is shown as one tab in the simwrapper interface.
 * <p>
 * Instances of this class will be created automatically using Java's SPI functionality. To make a service available, add the fully qualified
 * name to a file named:
 * <code>
 * META-INF/services/org.matsim.simwrapper.DashboardProvider
 * </code>
 * <p>
 * Every subclass of this interface needs to have default constructor with no argument.
 */
public interface DashboardProvider {

	/**
	 * List of dashboards this provider will add to the output. Generally one {@link Dashboard} is represented as one tab.
	 */
	List<Dashboard> getDashboards(Config config, SimWrapper simWrapper);

	/**
	 * Providers with higher priority are loaded first.
	 * If a provider with lower priority tries to add the same dashboard in the same context it will be discarded.
	 */
	default double priority() {
		return 0;
	}

}
