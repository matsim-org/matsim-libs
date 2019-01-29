package org.matsim.contrib.parking.parkingchoice.PC2.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.facilities.ActivityFacility;

public class RentableParking extends PublicParking  {

	public RentableParking(Id<PC2Parking> id, int capacity, Coord coord, ParkingCostModel parkingCostModel,
			String groupName) {
		super(id, capacity, coord, parkingCostModel, groupName);
	}

	
	public Id<Person> getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Id<Person> ownerId) {
		this.ownerId = ownerId;
	}

public boolean isRentable(double time){
	return GeneralLib.isIn24HourInterval(startRentableTime, endRentableTime, GeneralLib.projectTimeWithin24Hours(time)) && getAvailableParkingCapacity()>0;
}	
	
	private Id<Person> ownerId;
	double startRentableTime;
	public double getStartRentableTime() {
		return startRentableTime;
	}


	public void setStartRentableTime(double startRentableTime) {
		this.startRentableTime = startRentableTime;
	}


	public double getEndRentableTime() {
		return endRentableTime;
	}


	public void setEndRentableTime(double endRentableTime) {
		this.endRentableTime = endRentableTime;
	}


	public double getRentingPricePerHourInCurrencyUnit() {
		return rentingPricePerHourInCurrencyUnit;
	}


	public void setRentingPricePerHourInCurrencyUnit(double rentingPricePerHourInCurrencyUnit) {
		this.rentingPricePerHourInCurrencyUnit = rentingPricePerHourInCurrencyUnit;
	}


	double endRentableTime;
	double rentingPricePerHourInCurrencyUnit;
	
	@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		return rentingPricePerHourInCurrencyUnit*parkingDurationInSecond/3600;
	}
	
}
