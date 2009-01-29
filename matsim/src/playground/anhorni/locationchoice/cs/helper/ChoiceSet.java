package playground.anhorni.locationchoice.cs.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.MatsimRandom;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.cs.GenerateChoiceSets;


public class ChoiceSet {

	private Id id = null;
	double travelTimeBudget = -1.0;
	Trip trip;
	ArrayList<ZHFacility> facilities = new ArrayList<ZHFacility>();
	ArrayList<Double> travelTimes2Facilities = new ArrayList<Double>();
		
	public ChoiceSet(Id id, Trip trip) {
		this.id = id;
		this.trip = trip;		
	}
	
	private final static Logger log = Logger.getLogger(ChoiceSet.class);
	
	public double getTravelTimeBudget() {
		if (this.travelTimeBudget == -1.0) {
			double beforeShopping = trip.getShoppingAct().getStartTime() - trip.getBeforeShoppingAct().getEndTime();
			double afterShopping = trip.getAfterShoppingAct().getStartTime() - trip.getShoppingAct().getEndTime();
			return (beforeShopping + afterShopping);
		}
		else {
			return this.travelTimeBudget;
		}
	}
	
	public Coord getReferencePoint() {
		Coord referencePoint = null;
		Coord coordStart = this.trip.getBeforeShoppingAct().getCoord();
		Coord coordEnd = this.trip.getAfterShoppingAct().getCoord();
		
		if (coordStart.calcDistance(coordEnd)> GenerateChoiceSets.epsilon) {
			double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
			double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;			
			referencePoint = new CoordImpl(midPointX, midPointY);
		}
		else {
			// return to start location
			referencePoint = coordStart;
		}
		return referencePoint;
	}
	
	public ArrayList<ZHFacility> getFacilities() {
		return facilities;
	}


	public void addFacility(ZHFacility facility, double traveltime) {
		if (!this.facilities.contains(facility)) {
			this.facilities.add(facility);
			this.travelTimes2Facilities.add(traveltime);			
		}		
	}
	public void addFacilities(ArrayList<ZHFacility> facilities, ArrayList<Double> traveltimes) {
		
		int index = 0;
		Iterator<ZHFacility> it = facilities.iterator();
		while (it.hasNext()) {	
			ZHFacility facility = it.next();
			this.facilities.add(facility);
			if (traveltimes != null) {
				this.travelTimes2Facilities.add(traveltimes.get(index));
			}
			else {
				this.travelTimes2Facilities.add(0.0);
			}
			index++;
		}
	}
	public void addFacilities(ArrayList<ZHFacility> facilities, double traveltime) {		
		ArrayList<Double> traveltimes = new ArrayList<Double>();
		for (int i = 0; i < facilities.size(); i++) {
			traveltimes.add(traveltime);			
		}
		this.addFacilities(facilities, traveltimes);
	}
	
	public void removeFacility(int index) {
		this.travelTimes2Facilities.trimToSize();
		if (index >= 0 && index < this.facilities.size()) {
			this.facilities.remove(index);
			if (!travelTimes2Facilities.isEmpty()) {
				this.travelTimes2Facilities.remove(index);
			}
		}
		else {
		}
	}
	
	public boolean removeFacility(Id id) {
		Iterator<ZHFacility> it = this.facilities.iterator();
		int counter = 0;
		while (it.hasNext()) {		
			if (it.next().getId().compareTo(id) == 0) {
				removeFacility(counter);
				return true;
			}
			counter++;
		}	
		return false;
	}
	public boolean removeFacilityRandomly() {		
		this.facilities.trimToSize();
		if (this.facilities.size() > 1) {
			int index = MatsimRandom.random.nextInt(this.facilities.size());
			this.removeFacility(index);
			return true;
		}
		return false;
	}
	
	public int choiceSetSize() {
		return this.facilities.size();
	}
	
	public int getFacilityIndexProbDependendOnTravelCost(Coord other, boolean crowFly) {
		this.facilities.trimToSize();
		
		// TODO: sort by traveltime (crowfly);
		this.sortByCrowFlyDistance(other);		
		double [] accProbabilities;
		
		// traveltimes is empty for walk
		if (crowFly || this.travelTimes2Facilities.isEmpty()) {
			accProbabilities = assignAccProbabilitiesDependendOnCrowFlyDistance(other);
		}
		else {
			accProbabilities = assignAccProbabilitiesDependendOnTravelTimes();
		}
		double val = MatsimRandom.random.nextDouble();
		double last = 0.0;
				
		for (int i = 0; i < accProbabilities.length; i++) {
			// >= and <= if first field has width = 0.0
			if (val >= last && val <= accProbabilities[i]) {
				return i;
			}
			last = accProbabilities[i];
			//log.info("acc :" + accProbabilities[i]);
		}	
		// if accProbabalities not exactly summing up to 1 -> return last index;
		return (this.facilities.size()-1);
	}
		
	public ZHFacility getMostDistantFacility(Coord coord) {
		Iterator<ZHFacility> it = this.facilities.iterator();
		double tempMaxDist = 0.0;
		ZHFacility mostDistantFacility = null;
		while (it.hasNext()) {
			ZHFacility facility = it.next();		
			if (facility.getCenter().calcDistance(coord) > tempMaxDist) {
				mostDistantFacility = facility;
			}
		}	
		return mostDistantFacility;
	}
	
		
	private double [] assignAccProbabilitiesDependendOnCrowFlyDistance(Coord other) {
		double totalDist = this.computeTotalCrowFlyDistance(other);
		double [] accProbabilities = new double[this.facilities.size()];
		boolean allAtTheSamePlace = false;
		
		double val = 0.0;
		for (int i = 0; i < this.facilities.size(); i++) {
			ZHFacility facility = this.facilities.get(i);

			if (totalDist > GenerateChoiceSets.epsilon) {
				val += facility.getCenter().calcDistance(other)/totalDist;
				accProbabilities[i] = val;
			}
			else {
				// all facilities and shopping acts at ONE point
				val += 1.0 / this.facilities.size();
				accProbabilities[i] = val;
				allAtTheSamePlace = true;
			}
		}
		//log.info("val :" + val);
		if (allAtTheSamePlace) {
			log.info("All facilities and shopping activities at the same place");
		}
		return accProbabilities;
	}
	
	private double [] assignAccProbabilitiesDependendOnTravelTimes() {
		double totalTravelTime = this.computeTotalTravelTime();
		double [] accProbabilities = new double[this.facilities.size()];
	
		double val = 0.0;
		for (int i = 0; i < this.travelTimes2Facilities.size(); i++) {
			if (totalTravelTime > 0.0) {
				val += this.travelTimes2Facilities.get(i)/totalTravelTime;
				accProbabilities[i] = val;
			}
			else {
				// all facilities and person acts at ONE point
				val += 1.0 / this.facilities.size();
				accProbabilities[i] = val;
			}
		}
		return accProbabilities;
	}
	
	private double computeTotalTravelTime() {	
		double totalTravelTime = 0.0;
		for (int i = 0; i < this.travelTimes2Facilities.size(); i++) {
			totalTravelTime += travelTimes2Facilities.get(i);
		}
		return totalTravelTime;
	}
	
	private double computeTotalCrowFlyDistance(Coord coord) {
		Iterator<ZHFacility> it = this.facilities.iterator();
		double totalDist = 0.0;
		while (it.hasNext()) {
			ZHFacility facility = it.next();
			totalDist += facility.getCenter().calcDistance(coord);
			//log.info("distance " + facility.getCenter().calcDistance(coord));
		}
		//log.info("totalDistance " + totalDist);
		return totalDist;
	}
	
	private void sortByCrowFlyDistance(Coord coord) {
		Comparator<ZHFacility> distanceComparator = new CrowFlyDistanceComparator(coord);
		Collections.sort(this.facilities, distanceComparator);
	}
	
	// -------------------------------------------------------------------------------------
	public Id getPersonId() {
		return id;
	}
	public void setPersonId(Id personId) {
		this.id = personId;
	}
	public Trip getTrip() {
		return trip;
	}
	public void setTrip(Trip trip) {
		this.trip = trip;
	}

	public void setTravelTimeBudget(double travelTimeBudget) {
		this.travelTimeBudget = travelTimeBudget;
	}
}
