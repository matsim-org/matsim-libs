package org.matsim.simwrapper;

import org.matsim.core.config.Config;

import java.util.List;

/**
 * Interface for classes that provide default {@link Dashboard}s that should be added to simulation runs automatically.
 * <p>
 * Instances of this class will be created automatically using Java's SPI functionality. To make a service available, add the fully qualified
 * name to a file named:
 * <code>
 *     META-INF/services/org.matsim.simwrapper.DashboardProvider
 * </code>
 * <p>
 * Every subclass of this interface needs to have default constructor with no argument.
 */
public interface DashboardProvider {

	List<Dashboard> getDashboards(Config config, SimWrapper simWrapper);

}
