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
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.Parking;

public class ParkingSimulation implements AgentDepartureEventHandler, ActivityStartEventHandler {
	// key: personId, value: parking
	HashMap<Id, Parking> lastParkingUsed;
	// key: personId
	HashSet<Id> lastTransportModeWasCar;
	// key: personId
	HashSet<Id> carIsParked;
	
	LinkedList<ParkingArrivalEventHandler> parkingArrivalEventHandlers;
	LinkedList<ParkingDepartureEventHandler> parkingDepartureEventHandlers;
	private ParkingManager parkingManager;
	
	public ParkingSimulation(ParkingManager parkingManager){
		this.parkingManager=parkingManager;
		this.parkingArrivalEventHandlers=new LinkedList<ParkingArrivalEventHandler>();
		this.parkingDepartureEventHandlers=new LinkedList<ParkingDepartureEventHandler>();
		this.lastParkingUsed=new HashMap<Id, Parking>();
	}
	
	public void addParkingArrivalEventHandler(ParkingArrivalEventHandler parkingArrivalEventHandlers){
		this.parkingArrivalEventHandlers.add(parkingArrivalEventHandlers);
	}
	
	public void addParkingDepartureEventHandler(ParkingDepartureEventHandler parkingDepartureEventHandlers){
		this.parkingDepartureEventHandlers.add(parkingDepartureEventHandlers);
	}
	
	@Override
	public void reset(int iteration) {
		// for starting the next day, park each vehicle at the location
		// where it parked at the evening
		parkingManager.resetAllParkingOccupancies();
		for (Parking parking:lastParkingUsed.values()){
			parking.parkVehicle();
		}
		
		lastParkingUsed=new HashMap<Id, Parking>();
		lastTransportModeWasCar=new HashSet<Id>();
		carIsParked=new HashSet<Id>();
		
		for (ParkingArrivalEventHandler parkingArrivalHandler:parkingArrivalEventHandlers){
			parkingArrivalHandler.reset(iteration);
		}
		for (ParkingDepartureEventHandler parkingDepartureHandler:parkingDepartureEventHandlers){
			parkingDepartureHandler.reset(iteration);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		if (lastTransportModeWasCar.contains(personId)){
			carIsParked.add(personId);
			lastTransportModeWasCar.remove(personId);
			
			// TODO: this selection should happen according to best parking available for the
			// given activity location (not only according to the best walking distance).
			Parking selectedParking=parkingManager.getParkingWithShortestWalkingDistance(getTargetFacility(event).getCoord(),new ActInfo(event.getFacilityId(),event.getActType()));
			parkingManager.parkVehicle(personId, selectedParking);
			lastParkingUsed.put(personId, selectedParking);
			
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
