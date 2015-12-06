package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class DepartureAdder {
	Scenario scenario;
	public DepartureAdder(Scenario scenario){
		this.scenario=scenario;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addDeparture(double sample) {//sample represents percentage of vehicles to remove, ranges from 0 to 1
		CollectionUtil cutil = new CollectionUtil();
		ArrayList<Id<Vehicle>> added = new ArrayList<Id<Vehicle>>();
		Vehicles vehicles = scenario.getTransitVehicles();
		ArrayList<VehicleType> vehtypes = cutil.toArrayList(vehicles.getVehicleTypes().values().iterator());
		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
		int numvehadd = (int)Math.ceil(sample * vehids.size());//How many to add 
		for(int i=0;i<numvehadd;i++) {
			VehicleType type = vehtypes.get((int)Math.floor(vehtypes.size() * Math.random()));
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(numvehadd * Math.random()), Vehicle.class);
			Vehicle veh = new VehicleImpl(vehid, type);
		} 
	}
}
