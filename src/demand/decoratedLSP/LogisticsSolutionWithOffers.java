package demand.decoratedLSP;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferFactory;
import lsp.functions.Info;
import lsp.LogisticsSolutionImpl;
import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionImpl.Builder;
import lsp.shipment.LSPShipment;
import lsp.tracking.SimulationTracker;

public class LogisticsSolutionWithOffers implements LogisticsSolutionDecorator {

	private Id<LogisticsSolution> id;
	private LSPWithOffers lsp;
	private Collection<LogisticsSolutionElement> solutionElements; 
	private Collection<LSPShipment> shipments;
	private Collection<Info> solutionInfos;
	private Collection<EventHandler> eventHandlers;
	private Collection<SimulationTracker>trackers;
	private EventsManager eventsManager;
	private OfferFactory offerFactory;
	
	public static class Builder{
		private Id<LogisticsSolution> id;	
		private Collection<LogisticsSolutionElement> elements;
		private Collection<Info> solutionInfos;
		private Collection<EventHandler> eventHandlers;
		private Collection<SimulationTracker>trackers;
		private OfferFactory offerFactory;
		
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
		
		public Builder addOfferFactory(OfferFactory offerFactory) {
			this.offerFactory = offerFactory;
			return this;
		}
		
		public LogisticsSolutionWithOffers build(){
			return new LogisticsSolutionWithOffers(this);
		}	
	}
		
	
	private LogisticsSolutionWithOffers(LogisticsSolutionWithOffers.Builder builder){
		this.id = builder.id;
		this.solutionElements = builder.elements;
		for(LogisticsSolutionElement element : this.solutionElements) {
			element.setLogisticsSolution(this);
		}
		this.shipments = new ArrayList <LSPShipment>();
		this.solutionInfos = builder.solutionInfos;
		this.eventHandlers = builder.eventHandlers; 
		this.trackers = builder.trackers;
		this.offerFactory = builder.offerFactory;
		if(this.offerFactory != null) {
			this.offerFactory.setLogisticsSolution(this);
			this.offerFactory.setLSP(lsp);
		}
	}
	
	
	
	@Override
	public Id<LogisticsSolution> getId() {
		return id;
	}

	@Override
	public void setLSP(LSP lsp) {
		try {
			this.lsp = (LSPWithOffers) lsp;
		}
		catch(ClassCastException e) {
			System.out.println("The class " + this.toString() + " expects an LSPWithOffers and not any other implementation of LSP");
			System.exit(1);
		}
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
		getFirstElement().getIncomingShipments().addShipment(shipment.getStartTimeWindow().getStart(), shipment);
	}
	
	private LogisticsSolutionElement getFirstElement(){
		for(LogisticsSolutionElement element : solutionElements){
			if(element.getPreviousElement() == null){
				return element;
			}
			
		}
		return null;
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

	@Override
	public Offer getOffer(DemandObject object, String type) {
		return offerFactory.makeOffer(object, type);
	}

	@Override
	public void setOfferFactory(OfferFactory factory) {
		this.offerFactory = factory;
		this.offerFactory.setLogisticsSolution(this);
		this.offerFactory.setLSP(lsp);	
	}

	@Override
	public OfferFactory getOfferFactory() {
		return offerFactory;
	}
	
}
