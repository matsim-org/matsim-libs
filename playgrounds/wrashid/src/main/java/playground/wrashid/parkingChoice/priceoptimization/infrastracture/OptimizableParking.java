package playground.wrashid.parkingChoice.priceoptimization.infrastracture;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;


public class OptimizableParking extends PublicParking {
	
	private double costPerHourNormal = 0.5;
	private double costPerHourPeak = 0.5;
	private boolean highTariff = false;
	public OptimizableParking(Id<PC2Parking> id, int capacity, Coord coord, 
			ParkingCostModel parkingCostModel, String groupName, double costPerHourNormal,
			double costPerHourPeak, boolean highTariff){
		super(id, capacity, coord, parkingCostModel, groupName);
		this.costPerHourNormal = costPerHourNormal;
		this.costPerHourPeak = costPerHourPeak;
		this.highTariff = highTariff;
	}	
	@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		if (this.costPerHourNormal == 0.0)
			return 0.0;
		if (this.highTariff) {
			
			if (parkingDurationInSecond < 30 * 60){
				return 0.5;
			} else {
				return 0.5 + Math.ceil((parkingDurationInSecond - (30 * 60)) / (30 * 60)) * this.costPerHourNormal;
			}
		}
		
		else {
			
			return Math.ceil(parkingDurationInSecond / (60 * 60)) * this.costPerHourNormal;

		}
			

		

	}
	
	/*@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		
		//TODO: calculate the cost		
		double startMorning = 7.0 * 60 * 60;
		double endMorning = 11.0 * 60 * 60;
			
		double departureTime = arrivalTime + parkingDurationInSecond;
		double cost = 0.0;
		if (arrivalTime < startMorning) {
			
			if (departureTime < startMorning )
				cost = this.costPerHourNormal * (parkingDurationInSecond) / 3600.0;
			else if (departureTime >= startMorning && departureTime < endMorning)
				cost = this.costPerHourNormal * (startMorning - arrivalTime) / 3600.0
				+ this.costPerHourPeak * (departureTime - startMorning) / 3600.0;
			else 
				cost = this.costPerHourNormal * (startMorning - arrivalTime) / 3600.0
				+ this.costPerHourPeak * (endMorning - startMorning) / 3600.0
				+ this.costPerHourNormal * (departureTime - endMorning) / 3600.0;
			
			
		}
		else if (arrivalTime >= startMorning && arrivalTime < endMorning) {
			
			if (departureTime < endMorning )
				cost = 	this.costPerHourPeak * (departureTime - arrivalTime) / 3600.0;
			else
				cost = this.costPerHourNormal * (departureTime - endMorning) / 3600.0
				+ this.costPerHourPeak * (endMorning - arrivalTime) / 3600.0;

		}
		else
			cost = this.costPerHourNormal * (parkingDurationInSecond) / 3600.0;	 
		
		return cost;
	}	*/

	public double getCostPerHour() {
		return costPerHourNormal;
	}

	public void setCostPerHour(double costPerHour) {
		this.costPerHourNormal = costPerHour;
	}

}
