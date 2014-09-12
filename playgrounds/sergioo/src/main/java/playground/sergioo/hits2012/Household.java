package playground.sergioo.hits2012;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import playground.sergioo.hits2012.Person.Role;

public class Household {

	//Enumerations
	public static enum Column {
	
		DWELLING_TYPE(9, "J"),
		DWELLING_8_TYPE(10, "K"),
		ETHNIC(11, "L"),
		ETHNIC_OTHERS(12, "M"),
		NUM_BIKES(20, "U"),
		NUM_VEHICLES(35, "AJ"),
		FACTOR(105, "DB");
	
		//Attributes
		public int column;
		public String columnName;
	
		//Constructors
		private Column(int column, String columnName) {
			this.column = column;
			this.columnName = columnName;
		}
	
	}

	//Constants
	public static Map<String, Location> LOCATIONS = new HashMap<String, Location>();
	public static Set<String> DWELLING_TYPE = new HashSet<String>();
	public static Set<String> DWELLING_8_TYPE = new HashSet<String>();
	public static Set<String> ETHNIC = new HashSet<String>();
	public static Set<String> ETHNIC_OTHERS = new HashSet<String>();

	//Attributes
	private final String id;
	private final Map<String, Person> persons = new HashMap<String, Person>();
	private final Map<String, Person> personsNoComplete = new HashMap<String, Person>();
	private final Map<String, Person> personsNoTraveling = new HashMap<String, Person>();
	private final Map<String, Person> personsNoSurvey = new HashMap<String, Person>();
	private final Location location;
	private final String dwellingType;
	private final String dwelling8Type;
	private final String ethnic;
	private final String ethnicOthers;
	private final int numBikes;
	private final int numVehicles;
	private final double factor;
	
	//Constructors
	public Household(String id, Location location, String dwellingType,
			String dwelling8Type, String ethnic, String ethnicOthers,
			int numBikes, int numVehicles, double factor) {
		super();
		this.id = id;
		this.location = location;
		this.dwellingType = dwellingType;
		this.dwelling8Type = dwelling8Type;
		this.ethnic = ethnic;
		this.ethnicOthers = ethnicOthers;
		this.numBikes = numBikes;
		this.numVehicles = numVehicles;
		this.factor = factor;
	}

	//Methods
	public String getId() {
		return id;
	}
	public Person getPerson(String id) {
		return persons.get(id);
	}
	public void addPerson(Person person) {
		persons.put(person.getId(), person);
	}
	public Map<String, Person> getPersons() {
		return persons;
	}
	public Map<String, Person> getPersonsNoComplete() {
		return personsNoComplete;
	}
	public Map<String, Person> getPersonsNoTraveling() {
		return personsNoTraveling;
	}
	public Map<String, Person> getPersonsNoSurvey() {
		return personsNoSurvey;
	}
	public Location getLocation() {
		return location;
	}
	public String getDwellingType() {
		return dwellingType;
	}
	public String getDwelling8Type() {
		return dwelling8Type;
	}
	public String getEthnic() {
		return ethnic.equals("Others")?ethnicOthers:ethnic;
	}
	public int getNumBikes() {
		return numBikes;
	}
	public int getNumVehicles() {
		return numVehicles;
	}
	public double getFactor() {
		return factor;
	}
	public void orderPeople() {
		for(Person person:persons.values())
			if(person.getNoEligibleReason()!=null && !person.getNoEligibleReason().isEmpty())
				if(person.getNoEligibleReason().equals("Incomplete"))
					personsNoComplete.put(person.getId(), person);
				else
					personsNoSurvey.put(person.getId(), person);
			else if(person.getNoTripReason()!=null && !person.getNoTripReason().isEmpty())
				personsNoTraveling.put(person.getId(), person);
		for(String personId:personsNoComplete.keySet())
			persons.remove(personId);
		for(String personId:personsNoSurvey.keySet())
			persons.remove(personId);
		for(String personId:personsNoTraveling.keySet())
			persons.remove(personId);
	}
	public void setRoles() {
		Set<Person> personsAll = new HashSet<Person>(persons.values());
		personsAll.addAll(personsNoComplete.values());
		personsAll.addAll(personsNoSurvey.values());
		personsAll.addAll(personsNoTraveling.values());
		Person richest = null;
		for(Person person:personsAll)
			if(person.getIncomeInterval()!=null) {
				richest = person;
				break;
			}
		if(richest!=null) {
			for(Person person:personsAll)
				if(person.getIncomeInterval()!=null && person.getIncomeInterval().getCenter()>richest.getIncomeInterval().getCenter())
					richest = person;
			richest.setRole(Role.MAIN);
			Person partner = null;
			double smallestDifference = Double.MAX_VALUE;
			for(Person person:personsAll)
				if(person!=richest && person.getAgeInterval()!=null) {
					int difference = Math.abs(richest.getAgeInterval().getCenter()-person.getAgeInterval().getCenter());
					if(difference < smallestDifference) {
						smallestDifference = difference;
						partner = person;
					}
				}
			if(partner!=null)
				partner.setRole(Role.PARTNER);
			for(Person person:personsAll)
				if(person!=richest && person!=partner && person.getAgeInterval()!=null && person.getAgeInterval().getCenter()<richest.getAgeInterval().getCenter())
					person.setRole(Role.YOUNGER);
				else if(person!=richest && person!=partner && person.getAgeInterval()!=null)
					person.setRole(Role.OLDER);
		}
	}

}
