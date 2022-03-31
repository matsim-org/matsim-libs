package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.shipment.LSPShipment;
import lsp.controler.LSPSimulationTracker;


/**
 * A LogisticsSolution can be seen as a representative of a
 * transport chain. It consists of several chain links that implement the interface
 * {@link LogisticsSolutionElement}. The latter is more a logical than a physical entity.
 * Physical entities, in turn, are housed inside classes that implement the interface
 * {@link LSPResource}. This introduction of an intermediate layer allows physical Resources
 * to be used by several {@link LogisticsSolution}s and thus transport chains.
 */
public interface LogisticsSolution {

	Id<LogisticsSolution> getId();
	
	void setLSP(LSP lsp);
	
	LSP getLSP();
	
	Collection<LogisticsSolutionElement> getSolutionElements();
	
	Collection<LSPShipment> getShipments();
	
	void assignShipment(LSPShipment shipment);
	
	Collection<LSPInfo> getInfos();
	
    Collection <EventHandler> getEventHandlers();
        
    void addSimulationTracker(LSPSimulationTracker tracker);
    
    Collection<LSPSimulationTracker> getSimulationTrackers();
    
    void setEventsManager(EventsManager eventsManager);
}
