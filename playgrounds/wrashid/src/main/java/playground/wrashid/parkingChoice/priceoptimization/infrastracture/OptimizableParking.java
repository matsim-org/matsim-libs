package playground.wrashid.parkingChoice.priceoptimization.infrastracture;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;


public class OptimizableParking extends PublicParking {
	
	private double costPerHourNormal = 0.5;
	private double[] costPerEachHour;
	private double costPerHourPeak = 0.5;
	private boolean highTariff = false;
	public boolean isHighTariff() {
		return highTariff;
	}

	private Id<PC2Parking> parkingId;
	public OptimizableParking(Id<PC2Parking> id, int capacity, Coord coord, 
			ParkingCostModel parkingCostModel, String groupName, double costPerHourNormal,
			double costPerHourPeak, boolean highTariff){
		super(id, capacity, coord, parkingCostModel, groupName);
		this.costPerHourNormal = costPerHourNormal;
		this.costPerHourPeak = costPerHourPeak;
		this.costPerEachHour = new double[48];
		for (int i = 0; i < 24; i++) {
			this.costPerEachHour[i] = costPerHourNormal;
			this.costPerEachHour[24 + i] = costPerHourNormal;

		}
		this.highTariff = highTariff;
		this.parkingId = id;
	}	
	
	public OptimizableParking(Id<PC2Parking> id, int capacity, Coord coord, 
			ParkingCostModel parkingCostModel, String groupName, double[] costPerEachHour,
			 boolean highTariff){
		super(id, capacity, coord, parkingCostModel, groupName);
		this.costPerEachHour = costPerEachHour;
		this.highTariff = highTariff;
		this.parkingId = id;
	}
	/*@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		if (this.costPerHourNormal == 0.0)
			return 0.0;
		
		if (parkingId.toString().contains("gp"))
			return this.costPerHourNormal * parkingDurationInSecond / (60 * 60);
		if (this.highTariff) {
			
			if (parkingDurationInSecond < 30 * 60){
				return 0.5;
			} 
			else {
				return 0.5 + Math.ceil((parkingDurationInSecond - (30 * 60)) / (30 * 60)) * this.costPerHourNormal;
			}
		}
		
		else {
			
			return Math.ceil(parkingDurationInSecond / (60 * 60)) * this.costPerHourNormal;

		}
	}*/
	
	@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		
		double departureTime = arrivalTime + parkingDurationInSecond;
		int startIndex = (int) (arrivalTime / 3600.0);
		int endIndex = (int) ((arrivalTime + parkingDurationInSecond) / 3600.0);
		double cost = 0.0;

		if (endIndex > 47)
			System.out.println(personId.toString() + " " + arrivalTime + " " + parkingDurationInSecond);
		else {
		if (parkingId.toString().contains("gp")) {
			if (startIndex != endIndex){
				cost = this.costPerEachHour[startIndex] * ((startIndex + 1) * 3600 - arrivalTime) / 3600.0;
				cost += this.costPerEachHour[endIndex] * (departureTime - endIndex * 3600) / 3600.0;
				
				for (int i = startIndex + 1; i < endIndex; i++) {
					
					cost += this.costPerEachHour[i];
				}				
			}
			else {
				
				cost = this.costPerEachHour[startIndex] * parkingDurationInSecond / 3600.0;
			}			
		}
		else {
			
			if (this.highTariff) {
				
				if (parkingDurationInSecond < 30 * 60)
					return 0.5;
				else {
					
					cost = 0.5;
					double startTime = arrivalTime + 30 * 60;
					int newStartIndex = (int) (startTime / 3600.0);
					if (newStartIndex != endIndex){
						
						cost += ((newStartIndex + 1) * 3600 - startTime) / 1800.0 * this.costPerEachHour[newStartIndex];
						
						cost += this.costPerEachHour[endIndex] * Math.ceil((departureTime - newStartIndex * 3600) / 1800.0);
						
						for (int i = newStartIndex + 1; i < endIndex; i++) {
							
							cost += this.costPerEachHour[i] * 2;
						}
						
					}
					else {
						
						cost += this.costPerEachHour[newStartIndex] * Math.ceil(parkingDurationInSecond / 1800.0);
					}						
				}				
			}	
			
			else {
				
				if (startIndex != endIndex){
					cost = this.costPerEachHour[startIndex] * ((startIndex + 1) * 3600 - arrivalTime) / 3600.0;
					cost += this.costPerEachHour[endIndex] * Math.ceil((departureTime - endIndex * 3600) / 3600.0);
					
					for (int i = startIndex + 1; i < endIndex; i++) {
						
						cost += this.costPerEachHour[i];
					}				
				}
				else {
					
					cost = this.costPerEachHour[startIndex] * Math.ceil(parkingDurationInSecond / 3600.0);
				}	

				
			}
		}	
		}
		return cost;
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
		for (int i = 0; i < 24; i++) {
			this.costPerEachHour[i] = costPerHourNormal;
			this.costPerEachHour[24 + i] = costPerHourNormal;

		}
	}
	
	public void setCostPerHour(double cost, int hour) {
		
		this.costPerEachHour[hour] = cost;
		this.costPerEachHour[hour + 24] = cost;

	}

	public double getCostPerHour(int i) {
		// TODO Auto-generated method stub
		return this.costPerEachHour[i];
	}

}
