package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;

import lsp.events.FreightLinkEnterEvent;
import lsp.events.FreightLinkLeaveEvent;
import lsp.eventhandlers.FreightLinkLeaveEventHandler;
import lsp.events.FreightVehicleLeavesTrafficEvent;
import lsp.eventhandlers.FreightVehicleLeavesTrafficEventHandler;
import lsp.eventhandlers.FreightLinkEnterEventHandler;


/*package-private*/ class DistanceAndTimeHandler implements FreightLinkEnterEventHandler, FreightVehicleLeavesTrafficEventHandler, FreightLinkLeaveEventHandler {

	private Collection<FreightLinkEnterEvent> events;
	private double distanceCosts;
	private double timeCosts;
	private Network network;
	
	public DistanceAndTimeHandler(Network network) {
		this.network = network;
		this.events = new ArrayList<>();
	}
	
	
	@Override
	public void handleEvent(FreightLinkEnterEvent event) {
		events.add(event);
		
	}

	@Override
	public void reset(int iteration) {
		events.clear();
	}


	@Override
	public void handleEvent(FreightVehicleLeavesTrafficEvent leaveEvent) {
		for(FreightLinkEnterEvent enterEvent : events) {
			if((enterEvent.getLinkId() == leaveEvent.getLinkId()) && (enterEvent.getVehicleId() == leaveEvent.getVehicleId()) && 
			   (enterEvent.getCarrierId() == leaveEvent.getCarrierId())   &&  (enterEvent.getDriverId() == leaveEvent.getDriverId())) {
				double linkDuration = leaveEvent.getTime() - enterEvent.getTime();
				timeCosts = timeCosts + (linkDuration * enterEvent.getCarrierVehicle().getVehicleType().getCostInformation().getPerTimeUnit());
				double linkLength = network.getLinks().get(enterEvent.getLinkId()).getLength();
				distanceCosts = distanceCosts + (linkLength * enterEvent.getCarrierVehicle().getVehicleType().getCostInformation().getPerDistanceUnit());
				events.remove(enterEvent);
				break;
			}		
		}
	}


	@Override
	public void handleEvent(FreightLinkLeaveEvent leaveEvent) {
		for(FreightLinkEnterEvent enterEvent : events) {
			if((enterEvent.getLinkId() == leaveEvent.getLinkId()) && (enterEvent.getVehicleId() == leaveEvent.getVehicleId()) &&
			   (enterEvent.getCarrierId() == leaveEvent.getCarrierId())   &&  (enterEvent.getDriverId() == leaveEvent.getDriverId())) {
				double linkDuration = leaveEvent.getTime() - enterEvent.getTime();
				timeCosts = timeCosts + (linkDuration * enterEvent.getCarrierVehicle().getVehicleType().getCostInformation().getPerTimeUnit());
				double linkLength = network.getLinks().get(enterEvent.getLinkId()).getLength();
				distanceCosts = distanceCosts + (linkLength * enterEvent.getCarrierVehicle().getVehicleType().getCostInformation().getPerDistanceUnit());
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
