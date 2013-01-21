package playground.sergioo.facilitiesGenerator2012.hits;

import java.util.SortedMap;
import java.util.TreeMap;

public class PersonSchedule {
	
	//Attributes
	private String id;
	private String occupation;
	private SortedMap<Integer, Trip> trips;
	
	//Constructors
	public PersonSchedule(String id, String occupation) {
		super();
		this.id = id;
		this.occupation = occupation;
		trips = new TreeMap<Integer, Trip>();
	}
	
	//Methods
	public String getId() {
		return id;
	}
	public String getOccupation() {
		return occupation;
	}
	public SortedMap<Integer, Trip> getTrips() {
		return trips;
	}
	
}
