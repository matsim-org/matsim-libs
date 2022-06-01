package lsp;

import lsp.controler.LSPSimulationTracker;

import java.util.Collection;

public interface HasSimulationTrackers{

	void addSimulationTracker( LSPSimulationTracker tracker );

	Collection<LSPSimulationTracker> getSimulationTrackers();

}
