package vwExamples.utils.tripAnalyzer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import analysis.drtOccupancy.DynModeTrip;

public class ParkingEvent implements Comparable<ParkingEvent> {
	Double startOfParking;
	Double endOfParking;
	Id<Person> parkedPerson;
	Coord coordinate;
	String parkingZone;

	ParkingEvent(Double startOfParking, Double endOfParking, Id<Person> parkedPerson , Coord parkCoord, String parkingZone) {
		this.startOfParking = startOfParking;
		this.endOfParking = endOfParking;
		this.parkedPerson = parkedPerson;
		this.coordinate = parkCoord;
		this.parkingZone = parkingZone;
	}

	@Override
	public int compareTo(ParkingEvent o) {
		return getStartOfParking().compareTo(o.getStartOfParking());
	}

	Double getStartOfParking() {
		return this.startOfParking;

	}
	
	Double getEndOfParking() {
		return this.endOfParking;

	}
	
	Coord getCoord() {
		return this.coordinate;

	}
	
	String getParkingZone() {
		return this.parkingZone;
	}

}
