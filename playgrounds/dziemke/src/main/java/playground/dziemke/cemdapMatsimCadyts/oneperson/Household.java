package playground.dziemke.cemdapMatsimCadyts.oneperson;


public class Household {

	private int householdId;
	private int numberOfAdults = 1;
	private int totalNumberOfHouseholdVehicles = 1;
	private int homeTSZLocation;
	private int numberOfChildren = 0;
	private int householdStructure = 1;
			
	
	public Household(int householdId, int homeTSZLocation) {
		this.householdId = householdId;
		this.homeTSZLocation = homeTSZLocation;
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

}