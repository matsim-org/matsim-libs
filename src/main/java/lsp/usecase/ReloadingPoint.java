package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierService.Builder;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;
import lsp.tracking.SimulationTracker;

public class ReloadingPoint implements Resource {

	private Id<Resource> id;
	private Id<Link> locationLinkId;
	private ReloadingPointScheduler reloadingScheduler;
	private ArrayList <LogisticsSolutionElement> clientElements;
	private ArrayList<EventHandler> eventHandlers;
	private Collection<Info> infos;
	private Collection<SimulationTracker> trackers;
	private ReloadingPointEventHandler eventHandler;
	private EventsManager eventsManager;
	
	public static class Builder {
		
		private Id<Resource> id;
		private Id<Link> locationLinkId;
		private ReloadingPointScheduler reloadingScheduler;
		private ArrayList <LogisticsSolutionElement> clientElements;
		
		public static Builder newInstance(Id<Resource> id, Id<Link> locationLinkId){
			return new Builder(id,locationLinkId);
		}
		
		private Builder(Id<Resource> id, Id<Link> locationLinkId){
			this.id = id;
			this.clientElements = new ArrayList <LogisticsSolutionElement>();
			this.locationLinkId = locationLinkId;
		}
		
		public Builder setReloadingScheduler(ReloadingPointScheduler reloadingHandler){
			this.reloadingScheduler = reloadingHandler; 
			return this;
		}
		
		public ReloadingPoint build(){
			return new ReloadingPoint(this);
		}
		
	}
	
	private ReloadingPoint(ReloadingPoint.Builder builder){
		this.id = builder.id;
		this.locationLinkId = builder.locationLinkId;
		this.reloadingScheduler = builder.reloadingScheduler;
		reloadingScheduler.setReloadingPoint(this);
		ReloadingPointEventHandler eventHandler = new ReloadingPointEventHandler(this);
		reloadingScheduler.setEventHandler(eventHandler);
		this.clientElements = builder.clientElements;
		this.eventHandlers = new ArrayList<EventHandler>();
		this.infos = new ArrayList<Info>();
		this.trackers = new ArrayList<SimulationTracker>();
		eventHandlers.add(eventHandler);
	}
	
	@Override
	public Id<Link> getStartLinkId() {
		return locationLinkId;
	}

	@Override
	public Class<? extends ReloadingPoint> getClassOfResource() {
		return this.getClass();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return locationLinkId;
	}

	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public Id<Resource> getId() {
		return id;
	}

	@Override
	public void schedule(int bufferTime) {
		reloadingScheduler.scheduleShipments(this, bufferTime);	
	}

	public double getCapacityNeedFixed(){
		return reloadingScheduler.getCapacityNeedFixed();
	}

	public double getCapacityNeedLinear(){
		return reloadingScheduler.getCapacityNeedLinear();
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
