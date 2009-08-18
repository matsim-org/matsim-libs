package playground.anhorni.Nelson;

import java.util.List;
import java.util.Vector;

public class Person {
	
	private String id;
	private List<Trip> trips = new Vector<Trip>();
	
	public Person(String id) {
		this.id = id;
	}
	
	public void addTrip(Trip trip) {
		this.trips.add(trip);
	}
	
	public List<Trip> getTrips() {
		return trips;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
}
