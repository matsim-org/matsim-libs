package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.EventsManager;

public class FeedforwardSignalHandler implements PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {
	private final DrtZonalSystem zonalSystem;

	private final Map<Double, Map<String, MutableInt>> zoneNetDepartureMap = new HashMap<>();
	private final static Map<Double, List<Triple<String, String, Integer>>> REBALANCE_PLAN_CORE = new HashMap<>();
	private final Map<Id<Person>, Triple<Double, String, String>> potentialDRTTripsMap = new HashMap<>();
	
	private final int timeBinSize;
	
	// temporary parameter (to be gotten from the parameter file) //TODO
	private final int simulationEndTime = 30; // simulation ending time in hour

	/** Constructor */
	public FeedforwardSignalHandler(DrtZonalSystem zonalSystem, FeedforwardRebalancingParams params,
			EventsManager events) {
		this.zonalSystem = zonalSystem;
		timeBinSize = params.getTimeBinSize();
		prepareZoneNetDepartureMap();
		events.addHandler(this);
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		// If a request is rejected, remove the request info from the temporary storage
		// place
		Id<Person> personId = event.getPersonId();
		potentialDRTTripsMap.remove(personId);
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		// When the request is scheduled (i.e. accepted), add this travel information to
		// the database;
		// Then remove the travel information from the potential trips Map
		Id<Person> personId = event.getPersonId();
		double timeBin = potentialDRTTripsMap.get(personId).getLeft();
		String departureZoneId = potentialDRTTripsMap.get(personId).getMiddle();
		String arrivalZoneId = potentialDRTTripsMap.get(personId).getRight();

		zoneNetDepartureMap.get(timeBin).get(departureZoneId).increment();
		zoneNetDepartureMap.get(timeBin).get(arrivalZoneId).decrement();
		potentialDRTTripsMap.remove(personId);
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		// Here, we get a potential DRT trip. We will first note it down in the
		// temporary data base (Potential DRT Trips Map)
		Id<Person> personId = event.getPersonId();
		double timeBin = Math.floor(event.getTime() / timeBinSize);
		String departureZoneId = zonalSystem.getZoneForLinkId(event.getFromLinkId());
		String arrivalZoneId = zonalSystem.getZoneForLinkId(event.getToLinkId());
		potentialDRTTripsMap.put(personId, Triple.of(timeBin, departureZoneId, arrivalZoneId));
	}

	@Override
	public void reset(int iteration) {
		System.out.println("resetting: iteration number = " + Integer.toString(iteration));
		if (iteration > 0) { // TODO if reset is not called at iteration 0, then we can remove this check
			calculateRebalancePlan(true);
		} else {
			calculateRebalancePlan(false);
		}
		prepareZoneNetDepartureMap();
	}

	private void prepareZoneNetDepartureMap() {
		System.out.println("Now preparing the Departure recorder");
		for (int i = 0; i < (3600 / timeBinSize) * simulationEndTime; i++) {
			Map<String, MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			zoneNetDepartureMap.put((double) i, zonesPerSlot);
		}
	}

	private void calculateRebalancePlan(boolean calculateOrNot) {
		REBALANCE_PLAN_CORE.clear();
		int progressCounter = 0;
		if (calculateOrNot) {
			System.out.println("Start calculating rebalnace plan now");
			for (double timeBin : zoneNetDepartureMap.keySet()) {
				List<Pair<String, Integer>> supply = new ArrayList<>();
				List<Pair<String, Integer>> demand = new ArrayList<>();
				for (String zone : zoneNetDepartureMap.get(timeBin).keySet()) {
					int netDeparture = zoneNetDepartureMap.get(timeBin).get(zone).intValue();
					if (netDeparture > 0) {
						demand.add(Pair.of(zone, netDeparture));
					} else if (netDeparture < 0) {
						supply.add(Pair.of(zone, -netDeparture));
					}
				}
				List<Triple<String, String, Integer>> interZonalRelocations = new TransportProblem<>(
						this::calcStraightLineDistance).solve(supply, demand);
				REBALANCE_PLAN_CORE.put(timeBin, interZonalRelocations);
				progressCounter += 1;
				System.out.println("Calculating: "
						+ Double.toString(progressCounter * timeBinSize / simulationEndTime / 36) + "% complete");
			}
			System.out.println("Rebalance plan calculation is now complete! ");
		} 
	}

	private int calcStraightLineDistance(String zone1, String zone2) {
		return (int) DistanceUtils.calculateDistance(zonalSystem.getZoneCentroid(zone1),
				zonalSystem.getZoneCentroid(zone2));
	}

	public static Map<Double, List<Triple<String, String, Integer>>> getRebalancePlanCore() {
		return REBALANCE_PLAN_CORE;
	}

}
