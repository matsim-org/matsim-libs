package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.resources.LSPResource;
import lsp.controler.LSPSimulationTracker;


public interface LogisticsSolutionElement {

	public Id<LogisticsSolutionElement> getId();
	
	public void setLogisticsSolution(LogisticsSolution solution);
	
	public LogisticsSolution getLogisticsSolution();
	
	public void setPreviousElement(LogisticsSolutionElement element);
	
	public void setNextElement(LogisticsSolutionElement element);
	
	public LSPResource getResource();
	
	public LogisticsSolutionElement getPreviousElement();
	
	public LogisticsSolutionElement getNextElement();
	
	public WaitingShipments getIncomingShipments();
	
	public WaitingShipments getOutgoingShipments();
	
//	public void schedulingOfResourceCompleted();

	public void addSimulationTracker( LSPSimulationTracker tracker );
    
    public Collection<LSPInfo> getInfos();
    
    public Collection<EventHandler> getEventHandlers();
    
    public Collection <LSPSimulationTracker> getSimulationTrackers();
    
    public void setEventsManager(EventsManager eventsManager);
}
