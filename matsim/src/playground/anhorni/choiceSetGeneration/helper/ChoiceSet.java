package playground.anhorni.choiceSetGeneration.helper;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Iterator;
import java.util.TreeMap;

public class ChoiceSet {

	private Id id = null;
	private double travelTimeBudget = -1.0;
	private Trip trip;	
	private TreeMap<Id, ChoiceSetFacility> choiceSetFacilities = new TreeMap<Id, ChoiceSetFacility>();
	private PersonAttributes personAttributes;
	private Id chosenFacilityId;
	//private final static Logger log = Logger.getLogger(ChoiceSet.class);
		
	public ChoiceSet(Id id, Trip trip, Id chosenFacilityId) {
		this.id = id;
		this.trip = trip;	
		this.chosenFacilityId = chosenFacilityId;
	}
	
	public boolean isRoundTrip() {
		if (CoordUtils.calcDistance(this.trip.getBeforeShoppingAct().getCoord(), this.trip.getAfterShoppingAct().getCoord()) < 0.01) {
			return true;
		}
		else return false;
	}
	
	private void calculateAdditionalTravelTime() {		
		// find cheapest:
		double minTravelTime = 999999999999999999.0;
		Iterator<ChoiceSetFacility> choiceSet_it = this.choiceSetFacilities.values().iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSetFacility choiceSetFacility = choiceSet_it.next();
			double actualTravelTime = choiceSetFacility.getTravelTimeStartShopEnd();
			if (actualTravelTime >= 0.0 && actualTravelTime < minTravelTime) {
				minTravelTime = actualTravelTime;			
			}
		}		
		// calculate difference to cheapest for all facilities in the choice set:
		choiceSet_it = this.choiceSetFacilities.values().iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSetFacility choiceSetFacility = choiceSet_it.next();
			choiceSetFacility.setAdditionalTime(choiceSetFacility.getTravelTimeStartShopEnd() - minTravelTime);
		}	
	}
	
	private void calculateAdditionalTravelDistance() {		
		// find cheapest:
		double minTravelDistance = 999999999999999999.0;
		Iterator<ChoiceSetFacility> choiceSet_it = this.choiceSetFacilities.values().iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSetFacility choiceSetFacility = choiceSet_it.next();
			double actualTravelDistance = choiceSetFacility.getTravelTimeStartShopEnd();
			if (actualTravelDistance >= 0.0 && actualTravelDistance < minTravelDistance) {
				minTravelDistance = actualTravelDistance;			
			}
		}		
		// calculate difference to cheapest for all facilities in the choice set:
		choiceSet_it = this.choiceSetFacilities.values().iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSetFacility choiceSetFacility = choiceSet_it.next();
			choiceSetFacility.setAdditionalDistance(choiceSetFacility.getTravelDistanceStartShopEnd() - minTravelDistance);
		}	
	}
	
	public void calculateAdditonalTravelEffort() {
		this.calculateAdditionalTravelTime();
		this.calculateAdditionalTravelDistance();
	}
		
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
	
	public TreeMap<Id, ChoiceSetFacility> getFacilities() {
		return this.choiceSetFacilities;
	}
	
	public void addFacility(ZHFacility facility, double traveltime, double traveldistance) {
		if (!this.choiceSetFacilities.containsKey(facility.getId())) {
			this.choiceSetFacilities.put(facility.getId(), new ChoiceSetFacility(facility, traveltime, traveldistance));
		}		
	}
	
	public int choiceSetSize() {
		return this.choiceSetFacilities.size();
	}

	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
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
	public double getTravelTimeStartShopEnd(Id facilityId) {
		return this.choiceSetFacilities.get(facilityId).getTravelTimeStartShopEnd();
	}
	public double getTravelDistanceStartShopEnd(Id facilityId) {
		return this.choiceSetFacilities.get(facilityId).getTravelDistanceStartShopEnd();
	}

	public boolean zhFacilityIsInChoiceSet(Id facilityId) {
		if (this.choiceSetFacilities.containsKey(facilityId)) return true;
		else return false;
	}

	public PersonAttributes getPersonAttributes() {
		return personAttributes;
	}

	public void setPersonAttributes(PersonAttributes personAttributes) {
		this.personAttributes = personAttributes;
	}
	
	public double calculateCrowFlyDistanceMapped(Coord mappedCoords) {
		return CoordUtils.calcDistance(this.trip.getBeforeShoppingAct().getCoord(), mappedCoords) +
			CoordUtils.calcDistance(this.trip.getAfterShoppingAct().getCoord(), mappedCoords);
	}
	
	public double calculateCrowFlyDistanceExact(Coord exactCoords) {
		return CoordUtils.calcDistance(this.trip.getBeforeShoppingAct().getCoord(), exactCoords) +
			CoordUtils.calcDistance(this.trip.getAfterShoppingAct().getCoord(), exactCoords);
	}

	public Id getChosenFacilityId() {
		return this.chosenFacilityId;
	}
	
	public ChoiceSetFacility getChosenFacility() {
		return this.choiceSetFacilities.get(this.chosenFacilityId);
	}	
}


/*
 * unused:
 * 
 * public Coord getReferencePoint() {
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
 * 
 * 
 * 
 * private double [] assignAccProbabilitiesDependendOnCrowFlyDistance(Coord other) {
		double totalDist = this.computeTotalCrowFlyDistance(other);
		double [] accProbabilities = new double[this.facilities.size()];
		boolean allAtTheSamePlace = false;
		
		double val = 0.0;
		for (int i = 0; i < this.facilities.size(); i++) {
			ZHFacility facility = this.facilities.get(i);

			if (totalDist > GenerateChoiceSets.epsilon) {
				val += facility.getMappedPosition().calcDistance(other)/totalDist;
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
 * 
 * 
 * 
 * public void removeFacility(int index) {
		this.travelTimes2Facilities.trimToSize();
		if (index >= 0 && index < this.facilities.size()) {
			this.facilities.remove(index);
			if (!travelTimes2Facilities.isEmpty()) {
				this.travelTimes2Facilities.remove(index);
			}
			if (!travelDistances2Facilities.isEmpty()) {
				this.travelDistances2Facilities.remove(index);
			}
		}
		else {
			log.info("Index out of range: " + index);
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
			totalDist += facility.getMappedPosition().calcDistance(coord);
			//log.info("distance " + facility.getCenter().calcDistance(coord));
		}
		//log.info("totalDistance " + totalDist);
		return totalDist;
	}
	
	private void sortByCrowFlyDistance(Coord coord) {
		Comparator<ZHFacility> distanceComparator = new CrowFlyDistanceComparator(coord);
		Collections.sort(this.facilities, distanceComparator);
	}
	
	
		public ZHFacility getMostDistantFacility(Coord coord) {
		Iterator<ZHFacility> it = this.facilities.iterator();
		double tempMaxDist = 0.0;
		ZHFacility mostDistantFacility = null;
		while (it.hasNext()) {
			ZHFacility facility = it.next();		
			if (facility.getMappedPosition().calcDistance(coord) > tempMaxDist) {
				mostDistantFacility = facility;
			}
		}	
		return mostDistantFacility;
	}
	
 */
