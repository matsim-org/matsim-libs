package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.tracking.SimulationTracker;


public interface LogisticsSolution {

	public Id<LogisticsSolution> getId();
	
	public void setLSP(LSP lsp);
	
	public LSP getLSP();
	
	public Collection<LogisticsSolutionElement> getSolutionElements();
	
	public Collection<LSPShipment> getShipments();
	
	public void assignShipment(LSPShipment shipment);
	
	public Collection<Info> getInfos();
	
    public Collection <EventHandler> getEventHandlers();
        
    public void addSimulationTracker(SimulationTracker tracker);
    
    public Collection<SimulationTracker> getSimulationTrackers();
    
    public void setEventsManager(EventsManager eventsManager);
}
