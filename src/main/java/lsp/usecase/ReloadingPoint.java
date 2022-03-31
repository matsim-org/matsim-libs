package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.LSPInfo;
import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import lsp.controler.LSPSimulationTracker;

/**
 * {@link LSPResource} bei der die geplanten Tätigkeiten NICHT am Verkehr teilnehmen.
 *
 * Thus, these activities are entered directly in the Schedule of the LSPShipments that pass through the ReloadingPoint.
 *
 * An entry is added to the schedule of the shipments that is an instance of
 * {@link lsp.shipment.ScheduledShipmentHandle}. There, the name of the Resource
 * and the client element are entered, so that the way that the {@link lsp.shipment.LSPShipment}
 * takes is specified. In addition, the planned start and end time of the handling
 * (i.e. crossdocking) of the shipment is entered. In the example, crossdocking
 * starts as soon as the considered LSPShipment arrives at the {@link ReloadingPoint}
 * and ends after a fixed and a size dependent amount of time.
 */
/*package-private*/ class ReloadingPoint implements LSPResource {

	private final Id<LSPResource> id;
	private final Id<Link> locationLinkId;
	private final ReloadingPointScheduler reloadingScheduler;
	private final ArrayList <LogisticsSolutionElement> clientElements;
	private final ArrayList<EventHandler> eventHandlers;
	private final Collection<LSPInfo> infos;
	private final Collection<LSPSimulationTracker> trackers;
	private ReloadingPointTourEndEventHandler eventHandler;

	ReloadingPoint(UsecaseUtils.ReloadingPointBuilder builder){
		this.id = builder.getId();
		this.locationLinkId = builder.getLocationLinkId();
		this.reloadingScheduler = builder.getReloadingScheduler();
		reloadingScheduler.setReloadingPoint(this);
		ReloadingPointTourEndEventHandler eventHandler = new ReloadingPointTourEndEventHandler(this);
		reloadingScheduler.setEventHandler(eventHandler);
		this.clientElements = builder.getClientElements();
		this.eventHandlers = new ArrayList<>();
		this.infos = new ArrayList<>();
		this.trackers = new ArrayList<>();
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
	}
}
