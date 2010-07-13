package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.IntegerValueHashMap;
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

	public HashMap<Id, ParkingOccupancyBins> getParkingOccupancyBins() {
		return parkingOccupancyBins;
	}

	// events, which caused occupancy violation
	// TODO: when know, what exactly needed out of the event, perhaps reduce it.
	LinkedList<ActivityStartEvent> capacityViolation = new LinkedList<ActivityStartEvent>();

	private Controler controler;

	// TODO: continue here
	/*
	 * #####################################
	 * =======================================
	 * 
	 * - register capacityViolation (based on capacity of facility). - write
	 * tests: perhaps write test starting from controler for this!!! - close all
	 * parkings
	 */

	public ParkingOccupancyMaintainer(Controler controler) {
		this.controler = controler;
	}

	public void logArrivalAtParking(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		Id parkingFacilityId = event.getFacilityId();
		startTimeOfCurrentParking.put(personId, event.getTime());

		if (currentParkingOccupancy.get(parkingFacilityId) >= ParkingRoot.getParkingCapacity().getParkingCapacity(
				parkingFacilityId)) {
			capacityViolation.add(event);
		}

		currentParkingOccupancy.increment(event.getFacilityId());

		currentParkingFacilityId.put(personId, parkingFacilityId);
	}

	public void logDepartureFromParking(ActivityEndEvent event) {
		Id personId = event.getPersonId();
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
		// update all bins

		Iterator iter = endTimeOfFirstParking.keySet().iterator();

		while (iter.hasNext()) {
			Id personId = (Id) iter.next();
			Id parkingFacilityId = currentParkingFacilityId.get(personId);

			getOccupancyBins(parkingFacilityId).inrementParkingOccupancy(startTimeOfCurrentParking.get(personId),
					endTimeOfFirstParking.get(personId));
		}
	}

}
