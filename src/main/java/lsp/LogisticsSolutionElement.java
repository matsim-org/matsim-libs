package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.resources.Resource;
import lsp.tracking.SimulationTracker;


public interface LogisticsSolutionElement {

	public Id<LogisticsSolutionElement> getId();
	
	public void setLogisticsSolution(LogisticsSolution solution);
	
	public LogisticsSolution getLogisticsSolution();
	
	public void setPreviousElement(LogisticsSolutionElement element);
	
	public void setNextElement(LogisticsSolutionElement element);
	
	public Resource getResource();
	
	public LogisticsSolutionElement getPreviousElement();
	
	public LogisticsSolutionElement getNextElement();
	
	public WaitingShipments getIncomingShipments();
	
	public WaitingShipments getOutgoingShipments();
	
	public void schedulingOfResourceCompleted();

	public void addSimulationTracker(SimulationTracker tracker);
    
    public Collection<Info> getInfos();
    
    public Collection<EventHandler> getEventHandlers();
    
    public Collection <SimulationTracker> getSimulationTrackers();
    
    public void setEventsManager(EventsManager eventsManager);
}
