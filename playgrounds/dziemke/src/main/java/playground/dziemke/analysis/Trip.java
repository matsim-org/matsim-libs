package playground.dziemke.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;

import playground.dziemke.cemdapMatsimCadyts.Zone;


public class Trip {
	private Id<Household> householdId;
	private Id<Person> personId;
	private Id<Trip> tripId;
	private String activityEndActType;
	private Id<Link> departureLinkId;
	private Id<Zone> departureZoneId;
	private double departureTime;
	private String departureLegMode;
	private List<Id<Link>> links = new LinkedList<Id<Link>>();
	private int useHouseholdCar;
	private int useOtherCar;
	private int useHouseholdCarPool;
	private int useOtherCarPool;
	private int mode;
	private double distanceBeeline;
	private double distanceRoutedFastest;
	private double distanceRoutedShortest;
	private double speed;
	private double duration;
	private Id<Link> arrivalLinkId;
	private Id<Zone> arrivalZoneId;
	private double arrivalTime;
	private String arrivalLegMode;
	private String activityStartActType;
	private boolean tripComplete = false;
	
	private double weight;
	
	
	// constructor
	public Trip() {
	}

	
	// get and set methods
	public Id<Household> getHouseholdId() {
		return this.householdId;
	}

	public void setHouseholdId(Id<Household> householdId) {
		this.householdId = householdId;
	}
	
	public Id<Person> getPersonId() {
		return this.personId;
	}

	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}
	
	public Id<Trip> getTripId() {
		return this.tripId;
	}

	public void setTripId(Id<Trip> tripId) {
		this.tripId = tripId;
	}
	
	
	public String getActivityEndActType() {
		return this.activityEndActType;
	}
	
	public void setActivityEndActType(String activityEndActType) {
		this.activityEndActType = activityEndActType;
	}	

	public Id<Link> getDepartureLinkId() {
		return this.departureLinkId;
	}

	public void setDepartureLinkId(Id<Link> departureLinkId) {
		this.departureLinkId = departureLinkId;
	}
	
	public Id<Zone> getDepartureZoneId() {
		return this.departureZoneId;
	}

	public void setDepartureZoneId(Id<Zone> departureZoneId) {
		this.departureZoneId = departureZoneId;
	}
	
	public double getDepartureTime() {
		return this.departureTime;
	}

	public void setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
	}
		
	public String getDepartureLegMode() {
		return this.departureLegMode;
	}

	public void setDepartureLegMode(String departureLegMode) {
		this.departureLegMode = departureLegMode;
	}
	
	public List<Id<Link>> getLinks() {
		return this.links;
	}
	
	public void setLinks(List<Id<Link>> links) {
		this.links = links;
	}
	
	public int getUseHouseholdCar() {
		return this.useHouseholdCar;
	}

	public void setUseHouseholdCar(int useHouseholdCar) {
		this.useHouseholdCar = useHouseholdCar;
	}
	
	public int getUseOtherCar() {
		return this.useOtherCar;
	}

	public void setUseOtherCar(int useOtherCar) {
		this.useOtherCar = useOtherCar;
	}
	
	public int getUseHouseholdCarPool() {
		return this.useHouseholdCarPool;
	}

	public void setUseHouseholdCarPool(int useHouseholdCarPool) {
		this.useHouseholdCarPool = useHouseholdCarPool;
	}
	
	public int getUseOtherCarPool() {
		return this.useOtherCarPool;
	}

	public void setUseOtherCarPool(int useOtherCarPool) {
		this.useOtherCarPool = useOtherCarPool;
	}
	
	public int getMode() {
		return this.mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public double getDistanceBeeline() {
		return this.distanceBeeline;
	}

	public void setDistanceBeeline(double distanceBeeline) {
		this.distanceBeeline = distanceBeeline;
	}
	
	public double getDistanceRoutedFastest() {
		return this.distanceRoutedFastest;
	}

	public void setDistanceRoutedFastest(double distanceRoutedFastest) {
		this.distanceRoutedFastest = distanceRoutedFastest;
	}
	
	public double getDistanceRoutedShortest() {
		return this.distanceRoutedShortest;
	}

	public void setDistanceRoutedShortest(double distanceRoutedShortest) {
		this.distanceRoutedShortest = distanceRoutedShortest;
	}
	
	public double getSpeed() {
		return this.speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public double getDuration() {
		return this.duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public Id<Link> getArrivalLinkId() {
		return this.arrivalLinkId;
	}

	public void setArrivalLinkId(Id<Link> arrivalLinkId) {
		this.arrivalLinkId = arrivalLinkId;
	}
	
	public Id<Zone> getArrivalZoneId() {
		return this.arrivalZoneId;
	}

	public void setArrivalZoneId(Id<Zone> arrivalZoneId) {
		this.arrivalZoneId = arrivalZoneId;
	}
		
	public double getArrivalTime() {
		return this.arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	
	public String getArrivalLegMode() {
		return this.arrivalLegMode;
	}	
	
	public void setArrivalLegMode(String arrivalLegMode) {
		this.arrivalLegMode = arrivalLegMode;
	}
	
	public String getActivityStartActType() {
		return this.activityStartActType;
	}
	
	public void setActivityStartActType(String activityStartActType) {
		this.activityStartActType = activityStartActType;
	}	
	
	public boolean getTripComplete() {
		return this.tripComplete;
	}

	public void setTripComplete(boolean tripComplete) {
		this.tripComplete = tripComplete;
	}
	
	public double getWeight() {
		return this.weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}