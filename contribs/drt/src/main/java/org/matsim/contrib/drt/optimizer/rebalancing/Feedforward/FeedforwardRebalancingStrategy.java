package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingUtils;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

/**
 * This strategy is created based on the Feedforward Fluidic rebalancing
 * algorithm in AMoDeus. The algorithm send rebalancing vehicles based on the
 * DRT demand flow of previous iteration. This strategy is comparable to the
 * MinCostFlowRebalancing Strategy with Previous Iteration Zonal DRT Demand
 * Aggregator. But, instead of setting a rebalance target for each zone, the
 * concept of flow is used.
 * <p>
 * Important: At least 2 iterations are needed in order to make this strategy
 * function properly.
 *
 * @author Chengqi Lu
 */
public class FeedforwardRebalancingStrategy implements RebalancingStrategy {
	private static final Logger log = Logger.getLogger(FeedforwardRebalancingStrategy.class);

	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final RebalancingParams generalParams;

	private final int timeBinSize;
	private final double rebalanceInterval;
	private final double scaling;
	private final Random rnd = new Random(1234);
	private final int feedforwardSignalLead;

	private final boolean feedbackSwitch;
	private final int minNumVehiclesPerZone;

	private final DrtZoneTargetLinkSelector drtZoneTargetLinkSelector;
	private final FastHeuristicZonalRelocationCalculator fastHeuristicRelocationCalculator;

	private final Map<Double, List<Flow<DrtZone, DrtZone>>> feedforwardSignal;

	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, RebalancingParams generalParams,
			FeedforwardRebalancingStrategyParams strategySpecificParams,
			FeedforwardSignalHandler feedforwardSignalHandler, DrtZoneTargetLinkSelector drtZoneTargetLinkSelector,
			FastHeuristicZonalRelocationCalculator fastHeuristicRelocationCalculator) {
		this.zonalSystem = zonalSystem;
		this.generalParams = generalParams;
		this.drtZoneTargetLinkSelector = drtZoneTargetLinkSelector;
		this.fastHeuristicRelocationCalculator = fastHeuristicRelocationCalculator;
		this.fleet = fleet;
		timeBinSize = strategySpecificParams.getTimeBinSize();

		rebalanceInterval = generalParams.getInterval();

		scaling = strategySpecificParams.getFeedforwardSignalStrength() * rebalanceInterval / timeBinSize;
		log.info("The feedforward signal strength is: "
				+ Double.toString(strategySpecificParams.getFeedforwardSignalStrength()));

		feedforwardSignal = feedforwardSignalHandler.getFeedforwardSignal();
		feedforwardSignalLead = strategySpecificParams.getFeedforwardSignalLead();

		feedbackSwitch = strategySpecificParams.getFeedbackSwitch();
		minNumVehiclesPerZone = strategySpecificParams.getMinNumVehiclesPerZone();

		log.info("Rebalance strategy constructed: Feedforward Rebalancing Strategy is used");
		log.info("Feedback switch is set to " + Boolean.toString(feedbackSwitch));
		if (feedbackSwitch) {
			log.info("Minimum Number of Vehicles per zone is " + Integer.toString(minNumVehiclesPerZone));
		}
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<Relocation> relocationList = new ArrayList<>();
		double timeBin = Math.floor((time + feedforwardSignalLead) / timeBinSize);
		Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = RebalancingUtils
				.groupRebalancableVehicles(zonalSystem, generalParams, rebalancableVehicles, time);
		Map<DrtZone, List<DvrpVehicle>> actualRebalancableVehiclesPerZone = new HashMap<>();

		// Feedback part
		if (feedbackSwitch) {
			List<DrtZoneVehicleSurplus> vehicleSurplusList = new ArrayList<>();
			Map<DrtZone, List<DvrpVehicle>> soonRebalancableVehiclesPerZone = RebalancingUtils
					.groupSoonIdleVehicles(zonalSystem, generalParams, fleet, time);

			for (DrtZone zone : zonalSystem.getZones().values()) {
				int rebalancable = rebalancableVehiclesPerZone.getOrDefault(zone, List.of()).size();
				int soonIdle = soonRebalancableVehiclesPerZone.getOrDefault(zone, List.of()).size();

				int surplus = rebalancable + soonIdle - minNumVehiclesPerZone;
				vehicleSurplusList.add(new DrtZoneVehicleSurplus(zone, Math.min(surplus, rebalancable)));
			}

			relocationList.addAll(
					fastHeuristicRelocationCalculator.calcRelocations(vehicleSurplusList, rebalancableVehiclesPerZone));
			// Connection between feedback and feedforward part
			Set<DvrpVehicle> relocatedVehicles = relocationList.stream().map(relocation -> relocation.vehicle)
					.collect(toSet());
			for (DrtZone zone : rebalancableVehiclesPerZone.keySet()) {
				actualRebalancableVehiclesPerZone.put(zone, rebalancableVehiclesPerZone.get(zone).stream()
						.filter(v -> !relocatedVehicles.contains(v)).collect(Collectors.toList()));
			}
		} else {
			actualRebalancableVehiclesPerZone = rebalancableVehiclesPerZone;
		}

		// Feedforward part
		// assign rebalance vehicles based on the rebalance plan
		if (feedforwardSignal.containsKey(timeBin)) {
			// Generate relocations based on the "rebalancePlanCore"
			for (Flow<DrtZone, DrtZone> rebalanceInfo : feedforwardSignal.get(timeBin)) {
				DrtZone departureZone = rebalanceInfo.origin;
				DrtZone arrivalZone = rebalanceInfo.destination;
				int vehicleToSend = (int) Math.floor(scaling * rebalanceInfo.amount + rnd.nextDouble());
				// Note: we use probability to solve the problem of non-integer value of
				// vehileToSend after scaling.

				List<DvrpVehicle> rebalancableVehiclesInDepartureZone = actualRebalancableVehiclesPerZone
						.get(departureZone);
				int numAvailableVehiclesInZone = 0;
				if (rebalancableVehiclesInDepartureZone != null) {
					numAvailableVehiclesInZone = rebalancableVehiclesInDepartureZone.size();
				}

				if (vehicleToSend > numAvailableVehiclesInZone) {
					vehicleToSend = numAvailableVehiclesInZone;
				}

				for (int i = 0; i < vehicleToSend; i++) {
					Link destinationLink = drtZoneTargetLinkSelector.selectTargetLink(arrivalZone);
					relocationList.add(new Relocation(
							rebalancableVehiclesInDepartureZone.get(numAvailableVehiclesInZone - 1), destinationLink));
					rebalancableVehiclesInDepartureZone.remove(numAvailableVehiclesInZone - 1);
					numAvailableVehiclesInZone -= 1;
				}
			}
		}
		return relocationList;
	}
}
