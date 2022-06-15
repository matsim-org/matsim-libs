package lsp;

import lsp.controler.LSPSimulationTracker;

import java.util.Collection;

// One could say that the simulation trackers are the decorators that convert the data objects into behavioral objects.  In core matsim, we instead
// create behavioral objects, which contain the data objects.  E.g. MobsimAgent, DriverAgent, CarrierAgent, etc.  kai, may'22
public interface HasSimulationTrackers<T>{

	void addSimulationTracker( LSPSimulationTracker<T> tracker );

	Collection<LSPSimulationTracker<T>> getSimulationTrackers();

	void clearSimulationTrackers();
}
