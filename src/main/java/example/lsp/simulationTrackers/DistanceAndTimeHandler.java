package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;

import org.matsim.contrib.freight.events.LSPFreightLinkEnterEvent;
import org.matsim.contrib.freight.events.LSPFreightLinkLeaveEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPLinkLeaveEventHandler;
import org.matsim.contrib.freight.events.LSPFreightVehicleLeavesTrafficEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPVehicleLeavesTrafficEventHandler;
import org.matsim.contrib.freight.events.eventhandler.LSPLinkEnterEventHandler;
import org.matsim.vehicles.Vehicle;


/*package-private*/ class DistanceAndTimeHandler implements LSPLinkEnterEventHandler, LSPVehicleLeavesTrafficEventHandler, LSPLinkLeaveEventHandler{

	private Collection<LSPFreightLinkEnterEvent> events;
	private double distanceCosts;
	private double timeCosts;
	private Network network;
	
	public DistanceAndTimeHandler(Network network) {
		this.network = network;
		this.events = new ArrayList<>();
	}
	
	
	@Override
	public void handleEvent(LSPFreightLinkEnterEvent event) {
		events.add(event);
		
	}

	@Override
	public void reset(int iteration) {
		events.clear();
	}


	@Override
	public void handleEvent(LSPFreightVehicleLeavesTrafficEvent leaveEvent) {
		for(LSPFreightLinkEnterEvent enterEvent : events) {
			if((enterEvent.getLinkId() == leaveEvent.getLinkId()) && (enterEvent.getVehicleId() == leaveEvent.getVehicleId()) && 
			   (enterEvent.getCarrierId() == leaveEvent.getCarrierId())   &&  (enterEvent.getDriverId() == leaveEvent.getDriverId())) {
				double linkDuration = leaveEvent.getTime() - enterEvent.getTime();
				timeCosts = timeCosts + (linkDuration * ((Vehicle) enterEvent.getCarrierVehicle()).getType().getCostInformation().getPerTimeUnit());
				double linkLength = network.getLinks().get(enterEvent.getLinkId()).getLength();
				distanceCosts = distanceCosts + (linkLength * ((Vehicle) enterEvent.getCarrierVehicle()).getType().getCostInformation().getPerDistanceUnit());
				events.remove(enterEvent);
				break;
			}		
		}
	}


	@Override
	public void handleEvent(LSPFreightLinkLeaveEvent leaveEvent) {
		for(LSPFreightLinkEnterEvent enterEvent : events) {
			if((enterEvent.getLinkId() == leaveEvent.getLinkId()) && (enterEvent.getVehicleId() == leaveEvent.getVehicleId()) &&
			   (enterEvent.getCarrierId() == leaveEvent.getCarrierId())   &&  (enterEvent.getDriverId() == leaveEvent.getDriverId())) {
				double linkDuration = leaveEvent.getTime() - enterEvent.getTime();
				timeCosts = timeCosts + (linkDuration * ((Vehicle) enterEvent.getCarrierVehicle()).getType().getCostInformation().getPerTimeUnit());
				double linkLength = network.getLinks().get(enterEvent.getLinkId()).getLength();
				distanceCosts = distanceCosts + (linkLength * ((Vehicle) enterEvent.getCarrierVehicle()).getType().getCostInformation().getPerDistanceUnit());
				events.remove(enterEvent);
				break;
			}
		}
	}

	public double getDistanceCosts() {
		return distanceCosts;
	}

	public double getTimeCosts() {
		return timeCosts;
	}
	
}
