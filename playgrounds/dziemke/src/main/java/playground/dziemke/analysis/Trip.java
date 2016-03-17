package playground.dziemke.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
	private double departureTime_s;
	private String departureLegMode;
	private List<Id<Link>> links = new LinkedList<Id<Link>>();
	private int useHouseholdCar;
	private int useOtherCar;
	private int useHouseholdCarPool;
	private int useOtherCarPool;
	private String mode;
	private double distanceBeeline_m; // TODO
	private double distanceRoutedFastest_m;
	private double distanceRoutedShortest_m;
	private double speed_m_s;
	private double duration_s;
	private Id<Link> arrivalLinkId;
	private Id<Zone> arrivalZoneId;
	private double arrivalTime_s;
	private String arrivalLegMode;
	private String activityStartActType;
	private boolean tripComplete = false;
	
	private double weight;
	
	//
	private Double beelineDistance_m = Double.NaN;
	//
	
	
	/* Default constructor */
	public Trip() {
	}

	
	/* Get and set methods */
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
	
	public double getDepartureTime_s() {
		return this.departureTime_s;
	}

	public void setDepartureTime_s(double departureTime_s) {
		this.departureTime_s = departureTime_s;
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
	
	public String getMode() {
		return this.mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public double getDistanceBeeline_m() {
		return this.distanceBeeline_m;
	}

	public void setDistanceBeeline_m(double distanceBeeline_m) {
		this.distanceBeeline_m = distanceBeeline_m;
	}
	
	public double getDistanceRoutedFastest_m() {
		return this.distanceRoutedFastest_m;
	}

	public void setDistanceRoutedFastest_m(double distanceRoutedFastest_m) {
		this.distanceRoutedFastest_m = distanceRoutedFastest_m;
	}
	
	public double getDistanceRoutedShortest_m() {
		return this.distanceRoutedShortest_m;
	}

	public void setDistanceRoutedShortest_m(double distanceRoutedShortest_m) {
		this.distanceRoutedShortest_m = distanceRoutedShortest_m;
	}
	
	public double getSpeed_m_s() {
		return this.speed_m_s;
	}

	public void setSpeed_m_s(double speed_m_s) {
		this.speed_m_s = speed_m_s;
	}
	
	public double getDurationGivenBySurvey_s() {
		return this.duration_s;
	}

	public void setDurationGivenBySurvey_s(double duration_s) {
		this.duration_s = duration_s;
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
		
	public double getArrivalTime_s() {
		return this.arrivalTime_s;
	}

	public void setArrivalTime_s(double arrivalTime) {
		this.arrivalTime_s = arrivalTime;
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
	
	
	/**
	 * @param network
	 * @return beeline distance of this trip in meters
	 */
	public double getBeelineDistance(Network network) {
		if (beelineDistance_m.isNaN()) {
			calculateBeelineDistance(network);
		}
		return beelineDistance_m;
	}
	
	public double getCalculatedDuration_s(){
		return arrivalTime_s - departureTime_s;
	}


	private void calculateBeelineDistance(Network network) {
    	Link departureLink = network.getLinks().get(departureLinkId);
    	Link arrivalLink = network.getLinks().get(arrivalLinkId);

    	// TODO use coords of toNode instead of center coord of link
    	double arrivalCoordX = arrivalLink.getCoord().getX();
    	double arrivalCoordY = arrivalLink.getCoord().getY();
    	double departureCoordX = departureLink.getCoord().getX();
    	double departureCoordY = departureLink.getCoord().getY();
    	
    	double horizontalDistance_m = (Math.abs(departureCoordX - arrivalCoordX)) / 1000;
    	double verticalDistance_m = (Math.abs(departureCoordY - arrivalCoordY)) / 1000;

    	this.beelineDistance_m = Math.sqrt(horizontalDistance_m * horizontalDistance_m 
    			+ verticalDistance_m * verticalDistance_m);
	}
}