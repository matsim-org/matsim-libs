package playground.dziemke.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;


public class Trip {
	private Id householdId;
	private Id personId;
	private Id tripId;
	private String activityEndActType;
	private Id departureLinkId;
	private Id departureZoneId;
	private double departureTime;
	private String departureLegMode;
	private List<Id> links = new LinkedList<Id>();
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
	private Id arrivalLinkId;
	private Id arrivalZoneId;
	private double arrivalTime;
	private String arrivalLegMode;
	private String activityStartActType;
	private boolean tripComplete = false;
	
	private double weight;
	
	
	// constructor
	public Trip() {
	}

	
	// get and set methods
	public Id getHouseholdId() {
		return this.householdId;
	}

	public void setHouseholdId(Id householdId) {
		this.householdId = householdId;
	}
	
	public Id getPersonId() {
		return this.personId;
	}

	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	
	public Id getTripId() {
		return this.tripId;
	}

	public void setTripId(Id tripId) {
		this.tripId = tripId;
	}
	
	
	public String getActivityEndActType() {
		return this.activityEndActType;
	}
	
	public void setActivityEndActType(String activityEndActType) {
		this.activityEndActType = activityEndActType;
	}	

	public Id getDepartureLinkId() {
		return this.departureLinkId;
	}

	public void setDepartureLinkId(Id departureLinkId) {
		this.departureLinkId = departureLinkId;
	}
	
	public Id getDepartureZoneId() {
		return this.departureZoneId;
	}

	public void setDepartureZoneId(Id departureZoneId) {
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
	
	public List<Id> getLinks() {
		return this.links;
	}
	
	public void setLinks(List<Id> links) {
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
	
	public Id getArrivalLinkId() {
		return this.arrivalLinkId;
	}

	public void setArrivalLinkId(Id arrivalLinkId) {
		this.arrivalLinkId = arrivalLinkId;
	}
	
	public Id getArrivalZoneId() {
		return this.arrivalZoneId;
	}

	public void setArrivalZoneId(Id arrivalZoneId) {
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