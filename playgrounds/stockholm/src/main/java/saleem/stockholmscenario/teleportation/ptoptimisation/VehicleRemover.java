package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class VehicleRemover {
	Scenario scenario;
	public VehicleRemover(Scenario scenario){
		this.scenario=scenario;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void removeVehicles(double sample) {//sample represents percentage of vehicles to remove, ranges from 0 to 1
		CollectionUtil cutil = new CollectionUtil();
		ArrayList<Id<Vehicle>> removed = new ArrayList<Id<Vehicle>>();
		Vehicles vehicles = scenario.getTransitVehicles();
		ArrayList<Id<Vehicle>> vehids = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
		int numvehrem = (int)Math.ceil(sample * vehids.size()); 
		for(int i=0;i<numvehrem;i++) {
			int index = (int)Math.floor(numvehrem * Math.random());//Randomly remove vehilces
			if (!removed.contains(vehids.get(index))){//Try to remove if not removed already
				removed.add(vehids.get(index));
				vehicles.removeVehicle(vehids.get(index));
			}else{
				i--;
			}
			
		} 
				int a =0;

	}
}
