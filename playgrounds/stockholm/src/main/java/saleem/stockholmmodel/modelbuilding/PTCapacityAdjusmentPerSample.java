package saleem.stockholmmodel.modelbuilding;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmmodel.utils.CollectionUtil;

//Scale capacity of vehicles 
public class PTCapacityAdjusmentPerSample {
	public void adjustStoarageAndFlowCapacity(Scenario scenario, double samplesize){

		// Changing vehicle and road capacity according to sample size
		Vehicles vehicles = scenario.getTransitVehicles();
		Iterator<VehicleType> vehtypes = vehicles.getVehicleTypes().values().iterator();
		while(vehtypes.hasNext()){//Set flow and storage capacities according to sample size
			VehicleType vt = (VehicleType)vehtypes.next();
			VehicleCapacity cap = vt.getCapacity();
			cap.setSeats((int)Math.ceil(cap.getSeats()*samplesize));
			cap.setStandingRoom((int)Math.ceil(cap.getStandingRoom()*samplesize));
			vt.setCapacity(cap);
			vt.setPcuEquivalents(vt.getPcuEquivalents()*samplesize);
		}
	}
}
