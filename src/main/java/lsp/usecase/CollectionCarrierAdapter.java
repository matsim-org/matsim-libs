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

import lsp.functions.LSPInfo;
import lsp.LogisticsSolutionElement;
import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class CollectionCarrierAdapter implements LSPCarrierResource {

	private final Id<LSPResource>id;
	private final Carrier carrier;
	private final ArrayList<LogisticsSolutionElement> clientElements;
	private final CollectionCarrierScheduler collectionScheduler;
	private final Network network;
	private final Collection<EventHandler> eventHandlers;
	private final Collection<LSPInfo> infos;
	private final Collection<LSPSimulationTracker> trackers;

	CollectionCarrierAdapter(UsecaseUtils.CollectionCarrierAdapterBuilder builder){
		this.id = builder.id;
		Id<Link> locationLinkId = builder.locationLinkId;
		this.collectionScheduler = builder.collectionScheduler;
		this.clientElements = builder.clientElements;
		this.carrier = builder.carrier;
		this.network = builder.network;
		this.eventHandlers = new ArrayList<>();
		this.infos = new ArrayList<>();
		this.trackers = new ArrayList<>();
	}
	
	
	@Override
	public Class<? extends Carrier> getClassOfResource() {
		return carrier.getClass();
	}

	@Override
	public Id<Link> getStartLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
		
	}

	@Override
	public Id<Link> getEndLinkId() {
		Id<Link> depotLinkId = null;
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
			if(depotLinkId == null || depotLinkId == vehicle.getLocation()){
				depotLinkId = vehicle.getLocation();
			}
			
		}
		
		return depotLinkId;
	
	}
		
	
	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public Id<LSPResource> getId() {
		return id;
	}

	@Override
	public void schedule(int bufferTime) {
		collectionScheduler.scheduleShipments(this, bufferTime);	
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
	public Collection<LSPInfo> getInfos() {
		return infos;
	}

	
	@Override
	public void addSimulationTracker( LSPSimulationTracker tracker ) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers());
		this.infos.addAll(tracker.getInfos());		
	}


	@Override
	public Collection<LSPSimulationTracker> getSimulationTrackers() {
		return trackers;
	}

	@Override
	public void setEventsManager(EventsManager eventsManager) {
	}

}
