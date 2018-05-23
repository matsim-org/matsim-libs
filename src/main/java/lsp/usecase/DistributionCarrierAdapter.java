package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.tracking.SimulationTracker;

public class DistributionCarrierAdapter implements CarrierResource {

	private Id<Resource>id;
	private Carrier carrier;
	private Id<Link> locationLinkId;
	private ArrayList<LogisticsSolutionElement> clientElements;
	private DistributionCarrierScheduler distributionHandler;
	private Network network;
	private Collection<EventHandler> eventHandlers;
	private Collection<Info> infos;
	private Collection<SimulationTracker> trackers;
	private EventsManager eventsManager;
	
	public static class Builder {
		
		private Id<Resource>id;
		private Carrier carrier;
		private Id<Link> locationLinkId;
		private ArrayList<LogisticsSolutionElement> clientElements;
		private DistributionCarrierScheduler distributionHandler;
		private Network network;
			
			public static Builder newInstance(Id<Resource> id, Network network){
				return new Builder(id,network);
			}
			
			private Builder(Id<Resource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList <LogisticsSolutionElement>();
				this.network = network;
			}
			
			public Builder setLocationLinkId(Id<Link> locationLinkId){
				this.locationLinkId = locationLinkId;
				return this;
			}
			
			public Builder setCarrier(Carrier carrier){
				this.carrier = carrier;
				return this;
			}
			
			
			public Builder setDistributionScheduler(DistributionCarrierScheduler distributionHandler){
				this.distributionHandler = distributionHandler; 
				return this;
			}
			
			public DistributionCarrierAdapter build(){
				return new DistributionCarrierAdapter(this);
			}
			
		}
		
		private DistributionCarrierAdapter(DistributionCarrierAdapter.Builder builder){
			this.id = builder.id;
			this.locationLinkId = builder.locationLinkId;
			this.distributionHandler = builder.distributionHandler;
			this.clientElements = builder.clientElements;
			this.carrier = builder.carrier;
			this.network = builder.network;
			this.eventHandlers = new ArrayList<EventHandler>();
			this.infos = new ArrayList<Info>();
			this.trackers = new ArrayList<SimulationTracker>();
		}
	
	@Override
	public Class<? extends Carrier> getClassOfResource() {
		return carrier.getClass();
	}

	@Override
	public Id<Link> getStartLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
		
	}

	@Override
	public Id<Link> getEndLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
	
	}

	@Override
	public Id<Resource> getId() {
		return id;
	}
	
	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public void schedule(int bufferTime) {
		distributionHandler.scheduleShipments(this, bufferTime);
		
	}

	public Network getNetwork(){
		return network;
	}
	
	public Carrier getCarrier(){
		return carrier;
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
