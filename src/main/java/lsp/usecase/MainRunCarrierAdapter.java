package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.LogisticsSolutionElement;
import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class MainRunCarrierAdapter implements LSPCarrierResource {

	private Id<LSPResource>id;
	private Carrier carrier;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private ArrayList<LogisticsSolutionElement> clientElements;
	private MainRunCarrierScheduler mainRunScheduler;
	private Network network;
	private Collection<EventHandler> eventHandlers;
	private Collection<LSPSimulationTracker> trackers;
	private Collection<LSPInfo> infos;
	private EventsManager eventsManager;


	MainRunCarrierAdapter(UsecaseUtils.MainRunCarrierAdapterBuilder builder){
			this.id = builder.getId();
			this.carrier = builder.getCarrier();
			this.fromLinkId = builder.getFromLinkId();
			this.toLinkId = builder.getToLinkId();
			this.clientElements = builder.getClientElements();
			this.mainRunScheduler = builder.getMainRunScheduler();
			this.network = builder.getNetwork();
			this.eventHandlers = new ArrayList<EventHandler>();
			this.infos = new ArrayList<LSPInfo>();
			this.trackers = new ArrayList<LSPSimulationTracker>();
		}
	
	
	@Override
	public Id<LSPResource> getId() {
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
