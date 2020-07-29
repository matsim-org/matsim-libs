package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.EventsManager;

public class FeedforwardRebalancingStrategy implements RebalancingStrategy, PersonDepartureEventHandler {
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final FeedforwardRebalancingParams params;
	private final Network network;
	private final MinCostRelocationCalculator minCostRelocationCalculator;

	private final Map<Double, Map<String, MutableInt>> zoneNetDepartureMap = new HashMap<>();
	private final Map<Double, List<Triple<String, String, Integer>>> rebalancePlanCore = new HashMap<>();

	// temporary parameter (to be moved to the parameter file) //TODO
	private final int timeBinSize = 900; // size of time bin in second
	private final String mode = "drt";
	private final int simulationEndTime = 30; // simulation ending time in hour

	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, Network network,
			FeedforwardRebalancingParams params, MinCostRelocationCalculator minCostRelocationCalculator,
			EventsManager events) {
		this.network = network;
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.params = params;
		this.minCostRelocationCalculator = minCostRelocationCalculator;
		events.addHandler(this);
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// assign rebalnace vehicles based on the rebalance plan
		double timeBin = Math.floor(time / timeBinSize);
		if (rebalancePlanCore.containsKey(timeBin)) {
			// Generate relocations based on the "rebalancePlanCore"

		}
		return null;
	}

	@Override
	public void reset(int iteration) {
		// For the first iteration, there will be no rebalnace plan (a dummy plan, with
		// all entry equals 0, will be generated)
		// From the second iteration, rebalance plan will be generated based on the data
		// in last iteration
		if (iteration > 0) {
			calculateRebalancePlan(zoneNetDepartureMap, true);
		} else {
			calculateRebalancePlan(zoneNetDepartureMap, false);
		}
		prepareZoneNetDepartureMap();
	}

	private void calculateRebalancePlan(Map<Double, Map<String, MutableInt>> zoneNetDepartureMap,
			boolean calculateOrNot) {
		rebalancePlanCore.clear();
		if (calculateOrNot) {
			System.out.println("Start calculating rebalnace plan now");
			for (double timeBin : zoneNetDepartureMap.keySet()) {
				List<Pair<String, Integer>> supply = new ArrayList<>();
				List<Pair<String, Integer>> demand = new ArrayList<>();
				// TODO calculate supply demand here:

				List<Triple<String, String, Integer>> interZonalRelocations = new TransportProblem<>(
						this::calcStraightLineDistance).solve(supply, demand);
			}

		} else {
			System.out
					.println("Attention: No rebalance plan is pre-calculated (this is normal for the first iteration)");
		}
	}
	
	//TODO To get the destination link, we also need other event handler:
	// e.g. we use request submission event --> from link + to link and store this info in a temporary pool
	// and passenger request scheduled event to confirm this trip and add it to the database
	// or use passenger request rejected event to delete this trip from the pool. 
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode() == mode) {
			double timeBin = Math.floor(event.getTime() / timeBinSize);
			String zoneId = zonalSystem.getZoneForLinkId(event.getLinkId());
			zoneNetDepartureMap.get(timeBin).get(zoneId).increment();
		}
	}

	private void prepareZoneNetDepartureMap() {
		zoneNetDepartureMap.clear();
		for (int i = 0; i < (3600 / timeBinSize) * simulationEndTime; i++) {
			Map<String, MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			zoneNetDepartureMap.put((double) i, zonesPerSlot);
		}
	}

	private int calcStraightLineDistance(String zone1, String zone2) {
		return (int) DistanceUtils.calculateDistance(zonalSystem.getZoneCentroid(zone1),
				zonalSystem.getZoneCentroid(zone2));
	}
}
