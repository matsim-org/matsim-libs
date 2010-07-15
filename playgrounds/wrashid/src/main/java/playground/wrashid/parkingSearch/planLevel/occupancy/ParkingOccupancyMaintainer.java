package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingOccupancyMaintainer {

	// id: facilityId
	// value: occupancy (integer)
	IntegerValueHashMap<Id> currentParkingOccupancy = new IntegerValueHashMap<Id>();

	// id: personId
	// value: time
	HashMap<Id, Double> endTimeOfFirstParking = new HashMap<Id, Double>();
	DoubleValueHashMap<Id> startTimeOfCurrentParking = new DoubleValueHashMap<Id>();

	// id: personId
	// value: facilityId
	HashMap<Id, Id> currentParkingFacilityId = new HashMap<Id, Id>();

	// id: facilityId
	// value: ParkingOccupancyBins
	HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins = new HashMap<Id, ParkingOccupancyBins>();

	// id: facilityId
	// value: ParkingCapacityFullLogger
	HashMap<Id, ParkingCapacityFullLogger> parkingCapacityFullTimes = new HashMap<Id, ParkingCapacityFullLogger>();

	public HashMap<Id, ParkingCapacityFullLogger> getParkingCapacityFullTimes() {
		return parkingCapacityFullTimes;
	}

	public HashMap<Id, ParkingOccupancyBins> getParkingOccupancyBins() {
		return parkingOccupancyBins;
	}

	

	private Controler controler;

	public ParkingOccupancyMaintainer(Controler controler) {
		this.controler = controler;
		
		//initialize currentParkingOccupancy based on plans
	}
	
	public void performInitializationsAfterLoadingControlerData(){
		for (Person person:controler.getPopulation().getPersons().values()){
			Id firstParkingFacilityId=ParkingGeneralLib.getFirstParkingFacilityId(person.getSelectedPlan());
			if (firstParkingFacilityId!=null){
				currentParkingOccupancy.increment(firstParkingFacilityId);
			}	
		}
	}
	

	public void logArrivalAtParking(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		Id parkingFacilityId = event.getFacilityId();
		double time=GeneralLib.projectTimeWithin24Hours(event.getTime());
		startTimeOfCurrentParking.put(personId, event.getTime());

		// this code has been replaced by introduction of parkingCapacityFullTimes
		// delete code, if still here till 15. July 2010.
		/*
		if (currentParkingOccupancy.get(parkingFacilityId) >= ParkingRoot.getParkingCapacity().getParkingCapacity(
				parkingFacilityId)) {
			capacityViolation.add(event);
		}
		*/

		currentParkingOccupancy.increment(parkingFacilityId);	
		
		currentParkingFacilityId.put(personId, parkingFacilityId);
				
		
		// log if parking got full 
		if (currentParkingOccupancy.get(parkingFacilityId)==ParkingRoot.getParkingCapacity().getParkingCapacity(parkingFacilityId)){
			if (!parkingCapacityFullTimes.containsKey(parkingFacilityId)){
				parkingCapacityFullTimes.put(parkingFacilityId, new ParkingCapacityFullLogger());
			}
			
			parkingCapacityFullTimes.get(parkingFacilityId).logParkingFull(time);
		}
	}

	public void logDepartureFromParking(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		Id parkingFacilityId = event.getFacilityId();
		double time=GeneralLib.projectTimeWithin24Hours(event.getTime());
		
		currentParkingOccupancy.decrement(event.getFacilityId());
		
		if (!endTimeOfFirstParking.containsKey(personId)) {
			// handle departure from first parking

			endTimeOfFirstParking.put(personId, event.getTime());
			
			
			
			
			
		} else {
			// handle departure

			currentParkingOccupancy.decrement(event.getFacilityId());

			// update bins

			getOccupancyBins(event.getFacilityId()).inrementParkingOccupancy(startTimeOfCurrentParking.get(personId),
					event.getTime());
			
			
			
			
		}
		
		// log if parking got from full to not-full state
		double currentParkingOcc=currentParkingOccupancy.get(parkingFacilityId);
		double parkingCapacity=ParkingRoot.getParkingCapacity().getParkingCapacity(parkingFacilityId);
		if (currentParkingOcc==parkingCapacity){
			if (!parkingCapacityFullTimes.containsKey(parkingFacilityId)){
				parkingCapacityFullTimes.put(parkingFacilityId, new ParkingCapacityFullLogger());
			}
			
			parkingCapacityFullTimes.get(parkingFacilityId).logParkingNotFull(time);
		}
		
	}

	private ParkingOccupancyBins getOccupancyBins(Id facilityId) {
		if (!parkingOccupancyBins.containsKey(facilityId)) {
			parkingOccupancyBins.put(facilityId, new ParkingOccupancyBins());
		}

		return parkingOccupancyBins.get(facilityId);
	}

	/**
	 * The last parking arrival/ First parking departure need to be merged, so
	 * that the statistics can be used properly.
	 * 
	 * It is assured, that calling this method more than once does not work.
	 */
	public void closeAllLastParkings() {
		// update all bins and 

		Iterator iter = endTimeOfFirstParking.keySet().iterator();

		while (iter.hasNext()) {
			Id personId = (Id) iter.next();
			Id parkingFacilityId = currentParkingFacilityId.get(personId);

			getOccupancyBins(parkingFacilityId).inrementParkingOccupancy(startTimeOfCurrentParking.get(personId),
					endTimeOfFirstParking.get(personId));
			
			
			
			
		}
		
		// close parkingCapacityFullTimes: close the first/last parking
		for (ParkingCapacityFullLogger pcfl: parkingCapacityFullTimes.values()){
			pcfl.closeLastParking();
		}
		
		
		
	}

}
