package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.resources.LSPResource;
import lsp.controler.LSPSimulationTracker;


public interface LogisticsSolutionElement {

	Id<LogisticsSolutionElement> getId();
	
	void setLogisticsSolution(LogisticsSolution solution);
	
	LogisticsSolution getLogisticsSolution();

	
	void connectWithNextElement(LogisticsSolutionElement element);
	
	LSPResource getResource();
	
	LogisticsSolutionElement getPreviousElement();
	
	LogisticsSolutionElement getNextElement();
	
	WaitingShipments getIncomingShipments();
	
	WaitingShipments getOutgoingShipments();
	
//	public void schedulingOfResourceCompleted();

	void addSimulationTracker(LSPSimulationTracker tracker);
    
    Collection<LSPInfo> getInfos();
    
    Collection<EventHandler> getEventHandlers();
    
    Collection <LSPSimulationTracker> getSimulationTrackers();
    
//    public void setEventsManager(EventsManager eventsManager);
}
