package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.shipment.LSPShipment;
import lsp.controler.LSPSimulationTracker;


/**
 * Was macht das hier?
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
