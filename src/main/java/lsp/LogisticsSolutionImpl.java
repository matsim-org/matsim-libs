package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.shipment.LSPShipment;
import lsp.tracking.SimulationTracker;

public class LogisticsSolutionImpl implements LogisticsSolution {

	
	private Id<LogisticsSolution> id;
	private LSP lsp;
	private Collection<LogisticsSolutionElement> solutionElements; 
	private Collection<LSPShipment> shipments;
	private Collection<Info> solutionInfos;
	private Collection<EventHandler> eventHandlers;
	private Collection<SimulationTracker>trackers;
	private EventsManager eventsManager;
	
	public static class Builder{
		private Id<LogisticsSolution> id;	
		private Collection<LogisticsSolutionElement> elements;
		private Collection<Info> solutionInfos;
		private Collection<EventHandler> eventHandlers;
		private Collection<SimulationTracker>trackers;
		
		public static Builder newInstance(Id<LogisticsSolution>id){
			return new Builder(id);
		}
	
		private Builder(Id<LogisticsSolution> id){
			this.elements = new ArrayList<LogisticsSolutionElement>();
			this.solutionInfos = new ArrayList<Info>();
			this.eventHandlers = new ArrayList<EventHandler>(); 
			this.trackers = new ArrayList<SimulationTracker>();
			this.id = id;
		}
	
		public Builder addSolutionElement(LogisticsSolutionElement element){
			elements.add(element);
			return this;
		}
		
		public Builder addInfo(Info info) {
			solutionInfos.add(info);
			return this;
		}
		
		public Builder addEventHandler(EventHandler handler) {
			eventHandlers.add(handler);
			return this;
		}
		
		public Builder addTracker(SimulationTracker tracker) {
			trackers.add(tracker);
			return this;
		}
		
		public LogisticsSolutionImpl build(){
			//linkSolutionElements(elements);
			return new LogisticsSolutionImpl(this);
		}	
	
		/*private void linkSolutionElements(Collection<LogisticsSolutionElement> solutionElements){
			
			LogisticsSolutionElement previousElement = null;
			LogisticsSolutionElement currentElement = null;

			 
			for(LogisticsSolutionElement element : solutionElements){
				if((previousElement == null) && (currentElement == null)){
					previousElement = element;
				}
				else{
					currentElement = element;
					previousElement.setNextElement(currentElement);
					currentElement.setPreviousElement(previousElement);
					previousElement = currentElement;
				}
			}
		}*/
	}
		
	
	private LogisticsSolutionImpl(LogisticsSolutionImpl.Builder builder){
		this.id = builder.id;
		this.solutionElements = builder.elements;
		for(LogisticsSolutionElement element : this.solutionElements) {
			element.setLogisticsSolution(this);
		}
		this.shipments = new ArrayList <LSPShipment>();
		this.solutionInfos = builder.solutionInfos;
		this.eventHandlers = builder.eventHandlers; 
		this.trackers = builder.trackers;
	}
	
	
	@Override
	public Id<LogisticsSolution> getId() {
		return id;
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

	@Override
	public Collection<LogisticsSolutionElement> getSolutionElements() {
		return  solutionElements;
	}

	@Override
	public Collection<LSPShipment> getShipments() {
		return shipments;
	}

	@Override
	public void assignShipment(LSPShipment shipment) {
		shipments.add(shipment);	
	}
	
	@Override
	public Collection<Info> getInfos() {
		return solutionInfos;
	}


	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}


	@Override
	public void addSimulationTracker(SimulationTracker tracker) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers());
		this.solutionInfos.addAll(tracker.getInfos());
	}


	@Override
	public Collection<SimulationTracker> getSimulationTrackers() {
		return trackers;
	}
	
	@Override
	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

}	