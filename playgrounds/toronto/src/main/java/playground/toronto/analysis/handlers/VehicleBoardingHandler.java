package playground.toronto.analysis.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

public class VehicleBoardingHandler implements PersonEntersVehicleEventHandler {

	private Map<Id, Set<Double>> vehicleBoardings;
	
	public VehicleBoardingHandler(){
		this.vehicleBoardings = new HashMap<Id, Set<Double>>();
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleBoardings = new HashMap<Id, Set<Double>>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vehId = event.getVehicleId();
		if (!this.vehicleBoardings.containsKey(vehId)) this.vehicleBoardings.put(vehId, new HashSet<Double>());
		this.vehicleBoardings.get(vehId).add(event.getTime());
	}

	public Set<Double> getBaordingsForVehicle(Id vehicleId){
		return this.vehicleBoardings.get(vehicleId);
	}
	
}
