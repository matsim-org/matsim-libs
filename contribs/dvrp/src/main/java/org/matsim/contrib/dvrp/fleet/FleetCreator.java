package org.matsim.contrib.dvrp.fleet;

/**
 * This interface is used to fill the DVRP Fleet at the beginning of the
 * simulation.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface FleetCreator {
	void createFleet(Fleet fleet);
}
