package playground.wrashid.parkingChoice.priceoptimization.infrastracture;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;


public class OptimizableParking extends PublicParking {
	
	private double costPerHour = 0.5;

	public OptimizableParking(Id<PC2Parking> id, int capacity, Coord coord, 
			ParkingCostModel parkingCostModel, String groupName, double costPerHour){
		super(id, capacity, coord, parkingCostModel, groupName);
		this.costPerHour = costPerHour;
		
	}	
	
	@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		
		//TODO: calculate the cost		
		
		return costPerHour;
	}	

	public double getCostPerHour() {
		return costPerHour;
	}

	public void setCostPerHour(double costPerHour) {
		this.costPerHour = costPerHour;
	}

}
