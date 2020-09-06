package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

public class FeedforwardSignalHandler implements PassengerRequestScheduledEventHandler, DrtRequestSubmittedEventHandler,
		PassengerRequestRejectedEventHandler {
	private static final Logger log = Logger.getLogger(FeedforwardSignalHandler.class);
	private final DrtZonalSystem zonalSystem;

	// 1. TODO make int ZonalDemandEstimator (String mode) 
	private final Map<Double, Map<DrtZone, MutableInt>> zoneNetDepartureMap = new HashMap<>();
	private final Map<Id<Person>, Triple<Double, DrtZone, DrtZone>> potentialDRTTripsMap = new HashMap<>();

	private final Map<Double, List<Triple<DrtZone, DrtZone, Integer>>> feedforwardSignal = new HashMap<>();

	private final int timeBinSize;

	// temporary parameter (to be gotten from the parameter file) //TODO
	private final int simulationEndTime = 30; // simulation ending time in hour

	/**
	 * Constructor
	 */
	public FeedforwardSignalHandler(DrtZonalSystem zonalSystem,
			FeedforwardRebalancingStrategyParams strategySpecificParams) {
		this.zonalSystem = zonalSystem;
		timeBinSize = strategySpecificParams.getTimeBinSize();
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
		DrtZone departureZone = potentialDRTTripsMap.get(personId).getMiddle();
		DrtZone arrivalZone = potentialDRTTripsMap.get(personId).getRight();

		zoneNetDepartureMap.get(timeBin).get(departureZone).increment();
		zoneNetDepartureMap.get(timeBin).get(arrivalZone).decrement();
		potentialDRTTripsMap.remove(personId);
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		// Here, we get a potential DRT trip. We will first note it down in the
		// temporary data base (Potential DRT Trips Map)
		Id<Person> personId = event.getPersonId();
		double timeBin = Math.floor(event.getTime() / timeBinSize);
		DrtZone departureZoneId = zonalSystem.getZoneForLinkId(event.getFromLinkId());
		DrtZone arrivalZoneId = zonalSystem.getZoneForLinkId(event.getToLinkId());
		potentialDRTTripsMap.put(personId, Triple.of(timeBin, departureZoneId, arrivalZoneId));
	}

	@Override
	public void reset(int iteration) {
		if (iteration > 0) {
			calculateFeedforwardSignal(true);
		} else {
			calculateFeedforwardSignal(false);
		}
		prepareZoneNetDepartureMap();
	}

	private void prepareZoneNetDepartureMap() {
		for (int i = 0; i < (3600 / timeBinSize) * simulationEndTime; i++) {
			Map<DrtZone, MutableInt> zonesPerSlot = new HashMap<>();
			for (DrtZone zone : zonalSystem.getZones().values()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			zoneNetDepartureMap.put((double) i, zonesPerSlot);
		}
	}

	private void calculateFeedforwardSignal(boolean calculateOrNot) {
		feedforwardSignal.clear();
		int progressCounter = 0;
		if (calculateOrNot) {
			log.info("Start calculating rebalnace plan now");
			for (double timeBin : zoneNetDepartureMap.keySet()) {
				List<Pair<DrtZone, Integer>> supply = new ArrayList<>();
				List<Pair<DrtZone, Integer>> demand = new ArrayList<>();
				for (DrtZone zone : zoneNetDepartureMap.get(timeBin).keySet()) {
					int netDeparture = zoneNetDepartureMap.get(timeBin).get(zone).intValue();
					if (netDeparture > 0) {
						demand.add(Pair.of(zone, netDeparture));
					} else if (netDeparture < 0) {
						supply.add(Pair.of(zone, -netDeparture));
					}
				}
				List<Triple<DrtZone, DrtZone, Integer>> interZonalRelocations = new TransportProblem<>(
						this::calcStraightLineDistance).solve(supply, demand);
				feedforwardSignal.put(timeBin, interZonalRelocations);
				progressCounter += 1;
				log.info("Calculating: " + Double.toString(progressCounter * timeBinSize / simulationEndTime / 36)
						+ "% complete");
			}
			log.info("Rebalance plan calculation is now complete! ");
		}
	}

	private int calcStraightLineDistance(DrtZone zone1, DrtZone zone2) {
		return (int) DistanceUtils.calculateDistance(MGC.point2Coord(zone1.getGeometry().getCentroid()),
				MGC.point2Coord(zone2.getGeometry().getCentroid()));
	}

	public Map<Double, List<Triple<DrtZone, DrtZone, Integer>>> getFeedforwardSignal() {
		return feedforwardSignal;
	}

}
