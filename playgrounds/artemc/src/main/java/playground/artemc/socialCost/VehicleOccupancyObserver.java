package playground.artemc.socialCost;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;


public class VehicleOccupancyObserver implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler {

	private HashMap<Id,Integer> vehicleOccupancy;


	private static final Logger log = Logger.getLogger(VehicleOccupancyObserver.class);

	public VehicleOccupancyObserver() {

		this.vehicleOccupancy = new HashMap<Id, Integer>();
		log.info("Initialization of the VehicleOccupancyObserver...");
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		if(vehicleOccupancy.containsKey(event.getVehicleId())){
			int personsOnBoard = vehicleOccupancy.get(event.getVehicleId()) + 1;
			vehicleOccupancy.put(event.getVehicleId(), personsOnBoard);
		}
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(vehicleOccupancy.containsKey(event.getVehicleId())){
			int personsOnBoard = vehicleOccupancy.get(event.getVehicleId()) - 1;
			vehicleOccupancy.put(event.getVehicleId(), personsOnBoard);
			if(personsOnBoard<0)
				log.warn("The vehicle "+event.getVehicleId()+" has negative number of passengers inside!");
		}
	}

	public void handleEvent(TransitDriverStartsEvent event) {
		vehicleOccupancy.put(event.getVehicleId(),0);
	}

	@Override
	public void reset(int iteration) {
		for(Id id:vehicleOccupancy.keySet()){
			if(vehicleOccupancy.get(id)>0)
				log.warn("The vehicle "+id.toString()+" still has passengers in it at the end of the iteration!");
		}

		vehicleOccupancy.clear();
	}

	public HashMap<Id, Integer> getVehicleOccupancy() {
		return vehicleOccupancy;
	}
}
