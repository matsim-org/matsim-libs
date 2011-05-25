package playground.wrashid.parkingChoice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.wrashid.parkingChoice.events.ParkingArrivalEvent;
import playground.wrashid.parkingChoice.events.ParkingDepartureEvent;
import playground.wrashid.parkingChoice.handler.ParkingArrivalEventHandler;
import playground.wrashid.parkingChoice.handler.ParkingDepartureEventHandler;
import playground.wrashid.parkingChoice.infrastructure.Parking;

public class ParkingSimulation implements AgentDepartureEventHandler, ActivityStartEventHandler {
	// key: personId
	HashSet<Id> lastTransportModeWasCar;
	// key: personId
	HashSet<Id> carIsParked;
	
	LinkedList<ParkingArrivalEventHandler> parkingArrivalEventHandlers;
	LinkedList<ParkingDepartureEventHandler> parkingDepartureEventHandlers;
	private ParkingManager parkingManager;
	
	public ParkingSimulation(ParkingManager parkingManager){
		this.parkingManager=parkingManager;
	}
	
	public void addParkingArrivalEventHandler(ParkingArrivalEventHandler parkingArrivalEventHandlers){
		this.parkingArrivalEventHandlers.add(parkingArrivalEventHandlers);
	}
	
	public void addParkingDepartureEventHandler(ParkingDepartureEventHandler parkingDepartureEventHandlers){
		this.parkingDepartureEventHandlers.add(parkingDepartureEventHandlers);
	}
	
	@Override
	public void reset(int iteration) {
		lastTransportModeWasCar=new HashSet<Id>();
		carIsParked=new HashSet<Id>();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (lastTransportModeWasCar.contains(event.getPersonId())){
			carIsParked.add(event.getPersonId());
			lastTransportModeWasCar.remove(event.getPersonId());
			
			Parking selectedParking=parkingManager.getParkingWithShortestWalkingDistance(getTargetFacility(event).getCoord());
			
			
			// TODO: this selection should happen according to best parking available for the
			// given activity location (not only according to the best walking distance).

			parkingManager.parkVehicle(event.getPersonId(), selectedParking);
			
			
			// TODO: assign the car to that parking internall (update parking count)
			
			// TODO: update score here or below?
			// should this be done in the handler????
			// at least log it for later scoring => add handler
			
			
			
			for (ParkingArrivalEventHandler parkingArrivalEH: parkingArrivalEventHandlers){
				parkingArrivalEH.handleEvent(new ParkingArrivalEvent(event, selectedParking));
			}
		}

	}

	private ActivityFacility getTargetFacility(ActivityStartEvent event) {
		return parkingManager.getControler().getFacilities().getFacilities().get(event.getFacilityId());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (TransportMode.car.equalsIgnoreCase(event.getLegMode())){
			lastTransportModeWasCar.add(event.getPersonId());
			carIsParked.remove(event.getPersonId());
			
			Parking lastUsedParking=parkingManager.getCurrentParkingLocation(event.getPersonId());;
			parkingManager.unParkVehicle(event.getPersonId(), lastUsedParking);
			
			for (ParkingDepartureEventHandler parkingDepartureEH: parkingDepartureEventHandlers){
				parkingDepartureEH.handleEvent(new ParkingDepartureEvent(event, lastUsedParking));
			}
		}
	}
}
