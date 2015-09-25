package playground.artemc.crowding;

import org.matsim.api.core.v01.Id;

/**
 * Object of this class is generated for each interaction event between public transport vehicle and facility/stop/station
 * It keeps information on arrival/departure time, bus occupancy/load factor, number persons boarding/alighting at the facility etc. 
 * 
 *  @author achakirov, grerat
 * 
 */

public class BusFacilityInteractionEvent {
	
	private final Id stationId;
	private final Id busId;
	private double busArrivalTime;
	private double busArrivalSitters;
	private double busArrivalStandees;
	
	private double busDepartureTime;
	private double busDepartureSitters;
	private double busDepartureStandees;
	private int personsBoarding;
	private int personsAlighting;
	private double totalArrivalCrowdedness;
	private double totalDepartureCrowdedness;
	private double ArrivalLoadFactor;
	private double DepartureLoadFactor;

	public BusFacilityInteractionEvent(Id station, Id bus){
		this.stationId = station;
		this.busId = bus;
	}

	public double getBusArrivalTime() {
		return busArrivalTime;
	}

	public void setBusArrivalTime(double busArrivalTime) {
		this.busArrivalTime = busArrivalTime;
	}

	public double getBusDepartureTime() {
		return busDepartureTime;
	}

	public void setBusDepartureTime(double busDepartureTime) {
		this.busDepartureTime = busDepartureTime;
	}

	public int getPersonsBoarding() {
		return personsBoarding;
	}

	public void setPersonsBoarding(int personsBoarding) {
		this.personsBoarding = personsBoarding;
	}

	public int getPersonsAlighting() {
		return personsAlighting;
	}

	public void setPersonsAlighting(int personsAlighting) {
		this.personsAlighting = personsAlighting;
	}

	public double getArrivalLoadFactor() {
		return ArrivalLoadFactor;
	}

	public void setArrivalLoadFactor(double arrivalLoadFactor) {
		ArrivalLoadFactor = arrivalLoadFactor;
	}

	public double getDepartureLoadFactor() {
		return DepartureLoadFactor;
	}

	public void setDepartureLoadFactor(double departureLoadFactor) {
		DepartureLoadFactor = departureLoadFactor;
	}

	public Id getStationId() {
		return stationId;
	}

	public Id getBusId() {
		return busId;
	}

	public double getTotalDepartureCrowdedness() {
		return totalDepartureCrowdedness;
	}

	public void setTotalDepartureCrowdedness(double totalDepartureCrowdedness) {
		this.totalDepartureCrowdedness = totalDepartureCrowdedness;
	}
	
	
	public double getTotalArrivalCrowdedness() {
		return totalArrivalCrowdedness;
	}

	public void setTotalArrivalCrowdedness(double totalArrivalCrowdedness) {
		this.totalArrivalCrowdedness = totalArrivalCrowdedness;
	}

	public double getBusDepartureStandees() {
		return busDepartureStandees;
	}

	public void setBusDepartureStandees(double busDepartureStandees) {
		this.busDepartureStandees = busDepartureStandees;
	}

	public double getBusDepartureSitters() {
		return busDepartureSitters;
	}

	public void setBusDepartureSitters(double busDepartureSitters) {
		this.busDepartureSitters = busDepartureSitters;
	}

	public double getBusArrivalStandees() {
		return busArrivalStandees;
	}

	public void setBusArrivalStandees(double busArrivalStandees) {
		this.busArrivalStandees = busArrivalStandees;
	}

	public double getBusArrivalSitters() {
		return busArrivalSitters;
	}

	public void setBusArrivalSitters(double busArrivalSitters) {
		this.busArrivalSitters = busArrivalSitters;
	}
	
}
