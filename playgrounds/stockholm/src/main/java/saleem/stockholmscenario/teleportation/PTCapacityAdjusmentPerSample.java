package saleem.stockholmscenario.teleportation;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class PTCapacityAdjusmentPerSample {
	public void adjustStoarageAndFlowCapacity(Scenario scenario, double samplesize){

		// Changing vehicle and road capacity according to sample size
		Vehicles vehicles = scenario.getTransitVehicles();
		CollectionUtil<VehicleType> cutil = new CollectionUtil<VehicleType>();
		ArrayList<VehicleType> vehcilestypes = cutil.toArrayList(vehicles.getVehicleTypes().values().iterator());
		Iterator<VehicleType> vehtypes = vehcilestypes.iterator();
		while(vehtypes.hasNext()){//Set flow and storage capacities according to sample size
			VehicleType vt = (VehicleType)vehtypes.next();
			VehicleCapacity cap = vt.getCapacity();
			cap.setSeats((int)Math.ceil(cap.getSeats()*samplesize));
			cap.setStandingRoom((int)Math.ceil(cap.getStandingRoom()*samplesize));
			vt.setCapacity(cap);
			vt.setPcuEquivalents(vt.getPcuEquivalents()*samplesize);
			System.out.println("Sample Size is: " + samplesize);
		}
	}
}
