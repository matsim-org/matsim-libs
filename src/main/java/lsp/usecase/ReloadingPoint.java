package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class ReloadingPoint implements LSPResource {

	private Id<LSPResource> id;
	private Id<Link> locationLinkId;
	private ReloadingPointScheduler reloadingScheduler;
	private ArrayList <LogisticsSolutionElement> clientElements;
	private ArrayList<EventHandler> eventHandlers;
	private Collection<LSPInfo> infos;
	private Collection<LSPSimulationTracker> trackers;
	private ReloadingPointTourEndEventHandler eventHandler;
	private EventsManager eventsManager;

	ReloadingPoint(UsecaseUtils.ReloadingPointBuilder builder){
		this.id = builder.getId();
		this.locationLinkId = builder.getLocationLinkId();
		this.reloadingScheduler = builder.getReloadingScheduler();
		reloadingScheduler.setReloadingPoint(this);
		ReloadingPointTourEndEventHandler eventHandler = new ReloadingPointTourEndEventHandler(this);
		reloadingScheduler.setEventHandler(eventHandler);
		this.clientElements = builder.getClientElements();
		this.eventHandlers = new ArrayList<EventHandler>();
		this.infos = new ArrayList<LSPInfo>();
		this.trackers = new ArrayList<LSPSimulationTracker>();
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
	public Id<LSPResource> getId() {
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
		this.eventsManager = eventsManager;
	}
}
