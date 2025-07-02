package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.load.DvrpLoad;


/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public interface CapacityChangeTask extends StayTask {
	DvrpLoad getChangedCapacity();
}
