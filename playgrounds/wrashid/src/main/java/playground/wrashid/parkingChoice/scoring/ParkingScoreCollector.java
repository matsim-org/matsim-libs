package playground.wrashid.parkingChoice.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.list.Lists;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.parkingChoice.events.ParkingArrivalEvent;
import playground.wrashid.parkingChoice.events.ParkingDepartureEvent;
import playground.wrashid.parkingChoice.handler.ParkingArrivalEventHandler;
import playground.wrashid.parkingChoice.handler.ParkingDepartureEventHandler;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

import java.util.HashMap;
import java.util.Set;

//TODO: I could just collect the walking distances/time which needs to be deduced from score here.
//TODO: => make use of this wisly, as this is invoked a lot of times...
//TODO: in a similar handler I could log all the data during the simulation
//TODO: the parking occupancy is an approximation, which could be made more accurate.

public class ParkingScoreCollector implements ParkingArrivalEventHandler, ParkingDepartureEventHandler {

	public LinkedListValueHashMap<Id<Person>, ParkingInfo> parkingLog;
	
	private HashMap<Id, Double> firstParkingDepartureTime;

	private HashMap<Id<Person>, Double> currentArrivalTime;

	private HashMap<Id<Person>, PParking> currentParking;

	private DoubleValueHashMap<Id> sumOfParkingDurations;

	public HashMap<Id<PParking>, ParkingOccupancyBins> parkingOccupancies = new HashMap<Id<PParking>, ParkingOccupancyBins>();

	private boolean finishHandlingCalled;

	private LinkedListValueHashMap<Id<Person>, Double> walkingTimesAtParkingArrival;
	private LinkedListValueHashMap<Id<Person>, Double> walkingTimesAtParkingDeparture;

	private final Controler controler;

	public ParkingScoreCollector(Controler controler) {
		this.controler = controler;
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		currentParking=new HashMap<>();
		parkingLog=new LinkedListValueHashMap<>();
		firstParkingDepartureTime = new HashMap<>();
		currentArrivalTime = new HashMap<>();
		walkingTimesAtParkingArrival = new LinkedListValueHashMap<>();
		walkingTimesAtParkingDeparture = new LinkedListValueHashMap<>();
		sumOfParkingDurations = new DoubleValueHashMap<>();
		finishHandlingCalled = false;
		parkingOccupancies.clear();
	}

	public Set<Id<Person>> getPersonIdsWhoUsedCar() {
		if (!finishHandlingCalled) {
			DebugLib.stopSystemAndReportInconsistency("finish handling must be called before calling this method");
		}

		return currentArrivalTime.keySet();
	}

	@Override
	public void handleEvent(ParkingArrivalEvent event) {
		Id<Person> personId = event.getActStartEvent().getPersonId();
		currentArrivalTime.put(personId, event.getActStartEvent().getTime());

		currentParking.put(event.getActStartEvent().getPersonId(), event.getParking());

		updateWalkingTime(event, personId);
	}

	private void updateParkingOccupanciesParkingArrival(ParkingDepartureEvent event) {
		PParking parking = event.getParking();
		if (!parkingOccupancies.containsKey(parking.getId())) {
			parkingOccupancies.put(parking.getId(), new ParkingOccupancyBins());
		}

		if (currentArrivalTime.containsKey(event.getAgentDepartureEvent().getPersonId())) {
			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parking.getId());
			parkingOccupancyBins.inrementParkingOccupancy(currentArrivalTime.get(event.getAgentDepartureEvent().getPersonId()),
					event.getAgentDepartureEvent().getTime());
		}
	}

	private void updateWalkingTime(ParkingArrivalEvent event, Id<Person> personId) {
		Coord activityCoord = getActivityCoordinate(event.getActStartEvent().getFacilityId(), event.getActStartEvent()
				.getLinkId());
		double walkingTime = GeneralLib.getDistance(event.getParking().getCoord(), activityCoord)
				/ controler.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
		walkingTimesAtParkingArrival.put(personId, walkingTime);
	}

	private Coord getActivityCoordinate(Id<ActivityFacility> facilityId, Id<Link> linkId) {
		Coord activityCoord = null;

		if (facilityId != null) {
            activityCoord = controler.getScenario().getActivityFacilities().getFacilities().get(facilityId).getCoord();
		} else if (linkId != null) {
            activityCoord = controler.getScenario().getNetwork().getLinks().get(linkId).getCoord();
		} else {
			DebugLib.stopSystemAndReportInconsistency("facilityId and linkId of activity can't both be null!");
		}

		return activityCoord;
	}

	@Override
	public void handleEvent(ParkingDepartureEvent event) {
		Id<Person> personId = event.getAgentDepartureEvent().getPersonId();
		if (firstDepartureTimeNotInitializedYet(personId)) {
			initializeFirstDepartureTime(event, personId);
		} else {
			sumOfParkingDurations.incrementBy(personId,
					GeneralLib.getIntervalDuration(currentArrivalTime.get(personId), event.getAgentDepartureEvent().getTime()));
			
			parkingLog.put(personId, new ParkingInfo(event.getParking().getId(), currentArrivalTime.get(personId), event.getAgentDepartureEvent().getTime()));
		}

		updateParkingOccupanciesParkingArrival(event);

		// attention: the first departure event is here at first position
		updateWalkingTime(event, personId);
	}

	private void updateWalkingTime(ParkingDepartureEvent event, Id personId) {
        Coord activityCoord = controler.getScenario().getNetwork().getLinks().get(event.getAgentDepartureEvent().getLinkId()).getCoord();
		double walkingTime = GeneralLib.getDistance(event.getParking().getCoord(), activityCoord)
				/ controler.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk);
		walkingTimesAtParkingDeparture.put(personId, walkingTime);
	}

	private void initializeFirstDepartureTime(ParkingDepartureEvent event, Id personId) {
		double depTime = event.getAgentDepartureEvent().getTime();
		firstParkingDepartureTime.put(personId, depTime);
	}

	private boolean firstDepartureTimeNotInitializedYet(Id personId) {
		return !firstParkingDepartureTime.containsKey(personId);
	}

	public void finishHandling() {
		finishHandlingCalled = true;

		for (Id<Person> personId : currentArrivalTime.keySet()) {
			Double arrivalTime = currentArrivalTime.get(personId);
			Double departureTime = firstParkingDepartureTime.get(personId);
			Double parkingInterval = GeneralLib.getIntervalDuration(arrivalTime, departureTime);
			sumOfParkingDurations.incrementBy(personId, parkingInterval);
			updateParkingOccupancy(personId, arrivalTime, departureTime);
			
			parkingLog.put(personId, new ParkingInfo(currentParking.get(personId).getId(), arrivalTime.doubleValue(), departureTime));
		}

	}

	private void updateParkingOccupancy(Id<Person> personId, Double arrivalTime, Double departureTime) {
		PParking parking = currentParking.get(personId);
		if (!parkingOccupancies.containsKey(parking.getId())) {
			parkingOccupancies.put(parking.getId(), new ParkingOccupancyBins());
		}

		ParkingOccupancyBins parkingOccupancyBins = parkingOccupancies.get(parking.getId());
		parkingOccupancyBins.inrementParkingOccupancy(arrivalTime, departureTime);
	}

	public double getSumOfWalkingTimes(Id<Person> personId) {
		double walkingTimes = 0;

		walkingTimes += Lists.getSum(walkingTimesAtParkingArrival.get(personId));
		walkingTimes += Lists.getSum(walkingTimesAtParkingDeparture.get(personId));

		return walkingTimes;
	}

	public double getSumOfParkingDurations(Id<Person> personId) {
		return sumOfParkingDurations.get(personId);
	}

}
