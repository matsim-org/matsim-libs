package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.tracking.SimulationTracker;

public class MainRunCarrierAdapter implements CarrierResource {

	private Id<Resource>id;
	private Carrier carrier;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private ArrayList<LogisticsSolutionElement> clientElements;
	private MainRunCarrierScheduler mainRunScheduler;
	private Network network;
	private Collection<EventHandler> eventHandlers;
	private Collection<SimulationTracker> trackers;
	private Collection<Info> infos;
	private EventsManager eventsManager;
	
	
	public static class Builder {
		
		private Id<Resource>id;
		private Carrier carrier;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private ArrayList<LogisticsSolutionElement> clientElements;
		private MainRunCarrierScheduler mainRunScheduler;
		private Network network;
			
			public static Builder newInstance(Id<Resource> id, Network network){
				return new Builder(id,network);
			}
			
			private Builder(Id<Resource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList <LogisticsSolutionElement>();
				this.network = network;
			}
			
			public Builder setFromLinkId(Id<Link> fromLinkId){
				this.fromLinkId = fromLinkId;
				return this;
			}
			
			public Builder setToLinkId(Id<Link> toLinkId){
				this.toLinkId = toLinkId;
				return this;
			}
			
			public Builder setCarrier(Carrier carrier){
				this.carrier = carrier;
				return this;
			}
			
			public Builder setMainRunCarrierScheduler(MainRunCarrierScheduler mainRunScheduler){
				this.mainRunScheduler = mainRunScheduler; 
				return this;
			}
			
			public MainRunCarrierAdapter build(){
				return new MainRunCarrierAdapter(this);
			}
			
		}
		
		private MainRunCarrierAdapter(MainRunCarrierAdapter.Builder builder){
			this.id = builder.id;
			this.carrier = builder.carrier;
			this.fromLinkId = builder.fromLinkId;
			this.toLinkId = builder.toLinkId;
			this.clientElements = builder.clientElements;
			this.mainRunScheduler = builder.mainRunScheduler;
			this.network = builder.network;
			this.eventHandlers = new ArrayList<EventHandler>();
			this.infos = new ArrayList<Info>();
			this.trackers = new ArrayList<SimulationTracker>();
		}
	
	
	@Override
	public Id<Resource> getId() {
		return id;
	}

	@Override
	public Id<Link> getStartLinkId() {
		return fromLinkId;
	}

	@Override
	public Class<?> getClassOfResource() {
		return carrier.getClass();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return toLinkId;
	}

	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime) {
		mainRunScheduler.scheduleShipments(this, bufferTime);
	}

	public Carrier getCarrier(){
		return carrier;
	}
	
	public Network getNetwork(){
		return network;
	}

	public Collection <EventHandler> getEventHandlers(){
		return eventHandlers;
	}

	@Override
	public Collection<Info> getInfos() {
		return infos;
	}
	
	@Override
	public void addSimulationTracker(SimulationTracker tracker) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers());
		this.infos.addAll(tracker.getInfos());
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
