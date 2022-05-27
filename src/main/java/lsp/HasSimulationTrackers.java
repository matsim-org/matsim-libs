package lsp;

import lsp.controler.LSPSimulationTracker;

import java.util.Collection;

/**
 * @deprecated -- try to do without
 */
// I would say that the simulation trackers are the decorators that convert the data objects into behavioral objects.  In core matsim, we instead
// create behavioral objects, which contain the data objects.  E.g. MobsimAgent, DriverAgent, CarrierAgent, etc.  kai, may'22
public interface HasSimulationTrackers{

	/**
	 * @deprecated -- try to do without
	 */
	void addSimulationTracker( LSPSimulationTracker tracker );

	/**
	 * @deprecated -- try to do without
	 */
	Collection<LSPSimulationTracker> getSimulationTrackers();

}
