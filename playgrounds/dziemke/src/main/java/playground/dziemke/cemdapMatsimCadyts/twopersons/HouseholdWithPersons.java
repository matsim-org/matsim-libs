package playground.dziemke.cemdapMatsimCadyts.twopersons;

import java.util.HashMap;
import java.util.Map;


public class HouseholdWithPersons {

	private int householdId;
	private int numberOfAdults;
	private int totalNumberOfHouseholdVehicles = 1;
	private int homeTSZLocation;
	private int numberOfChildren = 0;
	private int householdStructure;
	
	// new
	private Map <Integer, PersonInHousehold> persons = new HashMap <Integer, PersonInHousehold>();
			
	
	public HouseholdWithPersons(int householdId, int homeTSZLocation, int numberOfAdults, int householdStructure, Map <Integer, PersonInHousehold> persons) {
		this.householdId = householdId;
		this.homeTSZLocation = homeTSZLocation;
		this.numberOfAdults = numberOfAdults;
		this.householdStructure = householdStructure;
		this.persons = persons;
	}

	public int getHouseholdId() {
		return this.householdId;
	}

	public void setHouseholdId(int householdId) {
		this.householdId = householdId;
	}

	public int getNumberOfAdults() {
		return this.numberOfAdults;
	}

	public void setNumberOfAdults(int numberOfAdults) {
		this.numberOfAdults = numberOfAdults;
	}
	
	public int getTotalNumberOfHouseholdVehicles() {
		return this.totalNumberOfHouseholdVehicles;
	}

	public void setTotalNumberOfHouseholdVehicles(int totalNumberOfHouseholdVehicles) {
		this.totalNumberOfHouseholdVehicles = totalNumberOfHouseholdVehicles;
	}
	
	public int getHomeTSZLocation() {
		return this.homeTSZLocation;
	}

	public void setHomeTSZLocation(int homeTSZLocation) {
		this.homeTSZLocation = homeTSZLocation;
	}
	
	public int getNumberOfChildren() {
		return this.numberOfChildren;
	}

	public void setNumberOfChildren(int numberOfChildren) {
		this.numberOfChildren = numberOfChildren;
	}
	
	public int getHouseholdStructure() {
		return this.householdStructure;
	}

	public void setHouseholdStructure(int householdStructure) {
		this.householdStructure = householdStructure;
	}
	
	//new
	public Map <Integer, PersonInHousehold> getPersons() {
		return this.persons;
	}

	public void setHouseholdStructure(Map <Integer, PersonInHousehold> persons) {
		this.persons = persons;
	}

}