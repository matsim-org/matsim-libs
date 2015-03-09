package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.lib.obj.list.ListElementMarkForRemoval;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class ParkingOccupancyMaintainer {

	// id: facilityId
	// value: occupancy (integer)
	IntegerValueHashMap<Id<ActivityFacility>> currentParkingOccupancy = new IntegerValueHashMap<>();

	// id: personId
	// value: Plan
	// this last selected plan is needed during replanning
	HashMap<Id<Person>, Plan> lastSelectedPlan = new HashMap<>();

	// id: personId
	// value: time
	HashMap<Id<Person>, Double> endTimeOfFirstParking = new HashMap<>();
	DoubleValueHashMap<Id<Person>> startTimeOfCurrentParking = new DoubleValueHashMap<>();

	// id: personId
	// value: travel distance whole day
	HashMap<Id<Person>, Double> parkingRelatedWalkDistance = new HashMap<>();

	// id: personId
	// value: facilityId
	HashMap<Id<Person>, Id<ActivityFacility>> currentParkingFacilityId = new HashMap<>();

	// id: personId
	// value: ParkingArrivalLog
	HashMap<Id<Person>, ParkingArrivalDepartureLog> parkingArrivalDepartureLog = new HashMap<>();

	// id: facilityId
	// value: ParkingOccupancyBins
	HashMap<Id<ActivityFacility>, ParkingOccupancyBins> parkingOccupancyBins = new HashMap<>();

	// id: facilityId
	// value: ParkingCapacityFullLogger
	HashMap<Id<ActivityFacility>, ParkingCapacityFullLogger> parkingCapacityFullTimes = new HashMap<>();

	public HashMap<Id<Person>, Plan> getLastSelectedPlan() {
		return lastSelectedPlan;
	}

	public HashMap<Id<Person>, Double> getParkingRelatedWalkDistance() {
		return parkingRelatedWalkDistance;
	}

	public LinkedList<ActivityImpl> getActivitiesWithParkingConstraintViolations(Plan plan) {
		Id<Person> personId = plan.getPerson().getId();
		LinkedList<Id<ActivityFacility>> parkingFacilityIds = ParkingGeneralLib.getAllParkingFacilityIds(plan);
		// this list is initialized with all parking target activities and later
		// filtered
		LinkedList<ActivityImpl> targetActivitiesWithParkingCapacityViolations = ParkingGeneralLib
				.getParkingTargetActivities(plan);

		// System.out.println(personId);

		for (int i = 0; i < parkingFacilityIds.size(); i++) {
			// System.out.print(parkingFacilityIds.get(i) + " - ");
		}

		// System.out.println();

		for (int i = 0; i < parkingArrivalDepartureLog.get(personId).getParkingArrivalDepartureList().size(); i++) {
			// System.out.print(parkingArrivalLog.get(personId).getParkingArrivalInfoList().get(i).getFacilityId()
			// + " - ");
		}

		// System.out.println();

		ListElementMarkForRemoval listElementToRemove = new ListElementMarkForRemoval();

		for (int i = 0; i < parkingFacilityIds.size(); i++) {
			Id<ActivityFacility> parkingFacilityId = parkingFacilityIds.get(i);

			ParkingTimeInfo pai = parkingArrivalDepartureLog.get(personId).getParkingArrivalDepartureList().get(i);

			double parkingArrivalTime = pai.getStartTime();

			// a consistency check of the system.
			if (!pai.getParkingFacilityId().toString().equalsIgnoreCase(parkingFacilityId.toString())) {
				throw new Error("the facility Ids are inconsistent");
			}

			// if no parking violation happened at the parking, remove it from
			// the list.
			ParkingCapacityFullLogger pcfl = parkingCapacityFullTimes.get(parkingFacilityId);
			if (pcfl == null || !pcfl.isParkingFullAtTime(parkingArrivalTime)) {
				listElementToRemove.markForRemoval(targetActivitiesWithParkingCapacityViolations.get(i));
			}
		}

		listElementToRemove.apply(targetActivitiesWithParkingCapacityViolations);

		return targetActivitiesWithParkingCapacityViolations;
	}

	public HashMap<Id<ActivityFacility>, ParkingCapacityFullLogger> getParkingCapacityFullTimes() {
		return parkingCapacityFullTimes;
	}

	public HashMap<Id<ActivityFacility>, ParkingOccupancyBins> getParkingOccupancyBins() {
		return parkingOccupancyBins;
	}

	public HashMap<Id<Person>, ParkingArrivalDepartureLog> getParkingArrivalDepartureLog() {
		return parkingArrivalDepartureLog;
	}

	public void setParkingArrivalLog(HashMap<Id<Person>, ParkingArrivalDepartureLog> parkingArrivalLog) {
		this.parkingArrivalDepartureLog = parkingArrivalLog;
	}

	private Controler controler;

	public ParkingOccupancyMaintainer(Controler controler) {
		this.controler = controler;

		// initialize currentParkingOccupancy based on plans
	}

	public void performInitializationsAfterLoadingControlerData() {
        for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			Id<ActivityFacility> firstParkingFacilityId = ParkingGeneralLib.getFirstParkingFacilityId(person.getSelectedPlan());
			if (firstParkingFacilityId != null) {
				currentParkingOccupancy.increment(firstParkingFacilityId);
			}
			lastSelectedPlan.put(person.getId(), person.getSelectedPlan());
			
			// update parkingRelatedWalkDistance

            parkingRelatedWalkDistance.put(person.getId(), ParkingGeneralLib.getParkingRelatedWalkingDistanceOfWholeDayAveragePerLeg(person.getSelectedPlan(), controler.getScenario().getActivityFacilities()));
		}
	}

	public void logArrivalAtParking(ActivityStartEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<ActivityFacility> parkingFacilityId = event.getFacilityId();
		double time = GeneralLib.projectTimeWithin24Hours(event.getTime());
		startTimeOfCurrentParking.put(personId, event.getTime());
		currentParkingOccupancy.increment(parkingFacilityId);

		currentParkingFacilityId.put(personId, parkingFacilityId);

		// log if parking got full
		if (currentParkingOccupancy.get(parkingFacilityId) == ParkingRoot.getParkingCapacity().getParkingCapacity(
				parkingFacilityId)) {
			if (!parkingCapacityFullTimes.containsKey(parkingFacilityId)) {
				parkingCapacityFullTimes.put(parkingFacilityId, new ParkingCapacityFullLogger());
			}

			parkingCapacityFullTimes.get(parkingFacilityId).logParkingFull(time);
		}

	}

	private void assureParkingOccupancyIsNonNegative(Id<ActivityFacility> facilityId) {
		if (currentParkingOccupancy.get(facilityId) < 0) {
			throw new Error("parking occupancy cannot be negative");
		}
	}

	public void logDepartureFromParking(ActivityEndEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<ActivityFacility> parkingFacilityId = event.getFacilityId();
		double time = GeneralLib.projectTimeWithin24Hours(event.getTime());
		
		if (parkingFacilityId==null){
			System.out.println();
		}

		currentParkingOccupancy.decrement(event.getFacilityId());

		if (!endTimeOfFirstParking.containsKey(personId)) {
			// handle departure from first parking

			endTimeOfFirstParking.put(personId, event.getTime());

		} else {

			// update bins

			getOccupancyBins(event.getFacilityId()).inrementParkingOccupancy(startTimeOfCurrentParking.get(personId),
					event.getTime());
			
			// log arrival time at parking
			if (!parkingArrivalDepartureLog.containsKey(personId)) {
				parkingArrivalDepartureLog.put(personId, new ParkingArrivalDepartureLog());
			}
			parkingArrivalDepartureLog.get(personId).logParkingArrivalDepartureTime(parkingFacilityId, startTimeOfCurrentParking.get(personId), event.getTime());
		}

		assureParkingOccupancyIsNonNegative(event.getFacilityId());

		// log if parking got from full to not-full state
		double currentParkingOcc = currentParkingOccupancy.get(parkingFacilityId);
		double parkingCapacity = ParkingRoot.getParkingCapacity().getParkingCapacity(parkingFacilityId);
		if (currentParkingOcc == parkingCapacity - 1) {
			if (!parkingCapacityFullTimes.containsKey(parkingFacilityId)) {
				parkingCapacityFullTimes.put(parkingFacilityId, new ParkingCapacityFullLogger());
			}

			parkingCapacityFullTimes.get(parkingFacilityId).logParkingNotFull(time);
		}
		
		
		

	}

	private ParkingOccupancyBins getOccupancyBins(Id<ActivityFacility> facilityId) {
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

		Iterator<Id<Person>> iter = endTimeOfFirstParking.keySet().iterator();

		while (iter.hasNext()) {
			Id<Person> personId = iter.next();
			Id<ActivityFacility> parkingFacilityId = currentParkingFacilityId.get(personId);

			if (parkingFacilityId==null){
				System.out.println();
			}
			
			getOccupancyBins(parkingFacilityId).inrementParkingOccupancy(startTimeOfCurrentParking.get(personId),
					endTimeOfFirstParking.get(personId));

			if (!parkingArrivalDepartureLog.containsKey(personId)) {
				parkingArrivalDepartureLog.put(personId, new ParkingArrivalDepartureLog());
			}
			parkingArrivalDepartureLog.get(personId).logParkingArrivalDepartureTime(parkingFacilityId, startTimeOfCurrentParking.get(personId), endTimeOfFirstParking.get(personId));
		}

		// close parkingCapacityFullTimes: close the first/last parking
		for (ParkingCapacityFullLogger pcfl : parkingCapacityFullTimes.values()) {
			pcfl.closeLastParking();
		}

	}

}
