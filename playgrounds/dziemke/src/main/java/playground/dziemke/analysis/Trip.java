package playground.dziemke.analysis;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;

import playground.dziemke.cemdapMatsimCadyts.Zone;


public class Trip {
	public static final Logger log = Logger.getLogger(Trip.class);

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
	private double distanceBeelineFromSurvey_m; // TODO
	private double distanceRoutedFastestFromSurvey_m;
	private double distanceRoutedShortestFromSurvey_m;
	private double speedFromSurvey_m_s;
	private double durationFromSurvey_s;
	private Id<Link> arrivalLinkId;
	private Id<Zone> arrivalZoneId;
	private double arrivalTime_s;
	private String arrivalLegMode;
	private String activityStartActType;
	private boolean tripComplete = false;
	
	private double weight;
	
	//
	private Double distanceBeelineByCalculation_m = Double.NaN;
	private Double distanceRoutedByCalculation_m = Double.NaN;
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
	
	public double getDistanceBeelineFromSurvey_m() {
		return this.distanceBeelineFromSurvey_m;
	}

	public void setDistanceBeelineFromSurvey_m(double distanceBeelineFromSurvey_m) {
		this.distanceBeelineFromSurvey_m = distanceBeelineFromSurvey_m;
	}
	
	public double getDistanceRoutedFastestFromSurvey_m() {
		return this.distanceRoutedFastestFromSurvey_m;
	}

	public void setDistanceRoutedFastestFromSurvey_m(double distanceRoutedFastestFromSurvey_m) {
		this.distanceRoutedFastestFromSurvey_m = distanceRoutedFastestFromSurvey_m;
	}
	
	public double getDistanceRoutedShortestFromSurvey_m() {
		return this.distanceRoutedShortestFromSurvey_m;
	}

	public void setDistanceRoutedShortestFromSurvey_m(double distanceRoutedShortestFromSurvey_m) {
		this.distanceRoutedShortestFromSurvey_m = distanceRoutedShortestFromSurvey_m;
	}
	
	public double getSpeedFromSurvey_m_s() {
		return this.speedFromSurvey_m_s;
	}

	public void setSpeedFromSurvey_m_s(double speedFromSurvey_m_s) {
		this.speedFromSurvey_m_s = speedFromSurvey_m_s;
	}
	
	public double getDurationFromSurvey_s() {
		return this.durationFromSurvey_s;
	}

	public void setDurationFromSurvey_s(double durationFromSurvey_s) {
		this.durationFromSurvey_s = durationFromSurvey_s;
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
	
	
	public double getDistanceBeelineByCalculation_m(Network network) {
		if (distanceBeelineByCalculation_m.isNaN()) {
			calculateBeelineDistance_m(network);
		}
		return distanceBeelineByCalculation_m;
	}
	
	public double getDurationByCalculation_s(){
		return arrivalTime_s - departureTime_s;
	}
	
	public double getDistanceRoutedByCalculation_m(Network network) {
		if (distanceRoutedByCalculation_m.isNaN()) {
			calculateRoutedDistance_m(network);
		}
		return distanceRoutedByCalculation_m;
	}
	
	 public String toString() {
		return "householdId = " + householdId
				+ " -- personId = " + personId
				+ " -- tripId = "+ tripId
				+ " -- activityEndActType = " + activityEndActType
				+ " -- departureLinkId = " + departureLinkId
//	private Id<Zone> departureZoneId;
//	private double departureTime_s;
//	private String departureLegMode;
//	private List<Id<Link>> links = new LinkedList<Id<Link>>();
//	private int useHouseholdCar;
//	private int useOtherCar;
//	private int useHouseholdCarPool;
//	private int useOtherCarPool;
//	private String mode;
//	private double distanceBeelineFromSurvey_m; // TODO
//	private double distanceRoutedFastest_m;
//	private double distanceRoutedShortestFromSurvey_m;
//	private double speedFromSurvey_m_s;
//	private double durationFromSurvey_s;
//	private Id<Link> arrivalLinkId;
//	private Id<Zone> arrivalZoneId;
//	private double arrivalTime_s;
//	private String arrivalLegMode;
//	private String activityStartActType;
//	private boolean tripComplete = false;
	+ " -- weight = " + weight;
//	private Double distanceBeelineByCalculation_m = Double.NaN;
//	private Double distanceRoutedByCalculation_m = Double.NaN;activityEndActType;	 
	 }


	private void calculateBeelineDistance_m(Network network) {
    	Link departureLink = network.getLinks().get(departureLinkId);
    	Link arrivalLink = network.getLinks().get(arrivalLinkId);

    	// TODO use coords of toNode instead of center coord of link
    	double arrivalCoordX_m = arrivalLink.getCoord().getX();
    	double arrivalCoordY_m = arrivalLink.getCoord().getY();
    	double departureCoordX_m = departureLink.getCoord().getX();
    	double departureCoordY_m = departureLink.getCoord().getY();
    	
    	// TODO use CoordUtils.calcEuclideanDistance instead
    	double horizontalDistance_m = Math.abs(departureCoordX_m - arrivalCoordX_m);
    	double verticalDistance_m = Math.abs(departureCoordY_m - arrivalCoordY_m);

    	this.distanceBeelineByCalculation_m = Math.sqrt(horizontalDistance_m * horizontalDistance_m 
    			+ verticalDistance_m * verticalDistance_m);
	}
	
	
	private void calculateRoutedDistance_m(Network network) {
		double tripDistance_m = 0.;
		if (links.isEmpty()) {
			log.warn("List of links is empty.");
		}
		for (int i = 0; i < links.size(); i++) {
			Id<Link> linkId = links.get(i);
			Link link = network.getLinks().get(linkId);
			double length_m = link.getLength();
			tripDistance_m = tripDistance_m + length_m;
		}
		this.distanceRoutedByCalculation_m = tripDistance_m;
		// TODO here, the distances from activity to link and link to activity are missing!
	}
}