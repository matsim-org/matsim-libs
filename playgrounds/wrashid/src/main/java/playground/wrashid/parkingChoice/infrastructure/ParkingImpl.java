package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.infrastructure.api.PriceScheme;

// TODO: make it also mandatory in the constructor to set the capacity
public class ParkingImpl implements Comparable<ParkingImpl>,Parking {
	public void setParkingId(Id parkingId) {
		this.parkingId = parkingId;
	}

	public void setMaxCapacity(double maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public void setCurrentOccupancy(int currentOccupancy) {
		this.currentOccupancy = currentOccupancy;
	}

	public void setPrice(Double price) {
		Price = price;
	}

	public void setAccessTime(Double accessTime) {
		this.accessTime = accessTime;
	}

	public void setSearchTime(Double searchTime) {
		this.searchTime = searchTime;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	Id parkingId = null;
	double maxCapacity =0;
	int currentOccupancy = 0;
	Double Price = null;
	Double accessTime = null;
	Double searchTime = null;
	Coord coord = null;
	double score = 0;
	private String parkingType;

	public void resetParkingOccupancy(){
		currentOccupancy=0;
	}
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getCapacity() {
		return maxCapacity;
	}

	public int getCurrentOccupancy() {
		return currentOccupancy;
	}

	public Double getPrice() {
		return Price;
	}

	public Double getAccessTime() {
		return accessTime;
	}

	public Double getSearchTime() {
		return searchTime;
	}

	public boolean hasFreeCapacity() {
		return maxCapacity > currentOccupancy;
	}

	public void parkVehicle() {
		currentOccupancy++;
	}

	public void removeVehicle() {
		currentOccupancy--;
	}

	public Coord getCoord() {
		return coord;
	}

	public ParkingImpl(Coord coord) {
		super();
		this.coord = coord;
	}

	public double getWalkingDistance(Coord targetCoordinate) {
		return GeneralLib.getDistance(coord, targetCoordinate);
	}

	@Override
	public int compareTo(ParkingImpl otherParking) {
		// the ordering is done inverse to the ususal ordering, because in a 
		// priority queue the first element is the smallest one, but we need the biggest one
		if (score > otherParking.getScore()) {
			return -1;
		} else if (score < otherParking.getScore()) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public Id getId() {
		// TODO Auto-generated method stub
		return this.parkingId;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return parkingType;
	}

	

	@Override
	public PriceScheme getPriceScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCapacity(double capacity) {
		maxCapacity=capacity;
	}

	@Override
	public void setType(String parkingType) {
		this.parkingType=parkingType;		
	}

	
}
