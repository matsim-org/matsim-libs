package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.HashSet;
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
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

/**
 * @author Chengqi Lu This strategy is created based on the Feedforward Fluidic
 *         rebalancing algorithm in AMoDeus. The algorithm send rebalancing
 *         vehicles based on the DRT demand flow of previous iteration. This
 *         strategy is comparable to the MinCostFlowRebalancing Strategy with
 *         Previous Iteration Zonal DRT Demand Aggregator. But, instead of
 *         setting a rebalance target for each zone, the concept of flow is
 *         used. Important: At least 2 iterations are needed in order to make
 *         this strategy function properly.
 */
public class FeedforwardRebalancingStrategy implements RebalancingStrategy {
	private static final Logger log = Logger.getLogger(FeedforwardRebalancingStrategy.class);

	private final DrtZonalSystem zonalSystem;
	private final RebalancingParams generalParams;
	private final VehicleInfoCollector vehicleInfoCollector;

	private final int timeBinSize;
	private final double rebalanceInterval;
	private final double scale;
	private final Random rnd = new Random(1234);
	private final int feedforwardSignalLead;

	private final boolean feedbackSwitch;
	private final int minNumVehiclesPerZone;

	private final DrtZoneTargetLinkSelector drtZoneTargetLinkSelector;
	private final FastHeuristicRelocationCalculator fastHeuristicRelocationCalculator;

	private final Map<Double, List<Flow<DrtZone, DrtZone>>> feedforwardSignal;

	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, RebalancingParams generalParams,
			FeedforwardRebalancingStrategyParams strategySpecificParams,
			FeedforwardSignalHandler feedforwardSignalHandler, DrtZoneTargetLinkSelector drtZoneTargetLinkSelector,
			FastHeuristicRelocationCalculator fastHeuristicRelocationCalculator) {
		this.zonalSystem = zonalSystem;
		this.generalParams = generalParams;
		this.drtZoneTargetLinkSelector = drtZoneTargetLinkSelector;
		this.fastHeuristicRelocationCalculator = fastHeuristicRelocationCalculator;
		timeBinSize = strategySpecificParams.getTimeBinSize();

		rebalanceInterval = generalParams.getInterval();
		vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);

		scale = strategySpecificParams.getFeedforwardSignalStrength() * rebalanceInterval / timeBinSize;
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

		// Feedback part
		List<DvrpVehicle> truelyRebalancableVehicles = new ArrayList<>();

		if (feedbackSwitch) {
			List<DrtZoneVehicleSurplus> vehicleSurplusList = new ArrayList<>();
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
					.groupRebalancableVehicles(rebalancableVehicles, time, generalParams.getMinServiceTime());
			Map<DrtZone, List<DvrpVehicle>> soonRebalancableVehiclesPerZone = vehicleInfoCollector
					.groupSoonIdleVehicles(time, generalParams.getMaxTimeBeforeIdle(),
							generalParams.getMinServiceTime());

			for (DrtZone zone : zonalSystem.getZones().values()) {
				int surplus = rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size()
						+ soonRebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size()
						- minNumVehiclesPerZone;
				vehicleSurplusList.add(new DrtZoneVehicleSurplus(zone, surplus));
			}
			
			relocationList.addAll(
					fastHeuristicRelocationCalculator.calcRelocations(vehicleSurplusList, rebalancableVehiclesPerZone));
			truelyRebalancableVehicles.addAll(fastHeuristicRelocationCalculator.getTruelyRebalancableVehicles());

		} else {
			truelyRebalancableVehicles.addAll(rebalancableVehicles.collect(Collectors.toList()));
		}

		// Feedforward part
		// assign rebalnace vehicles based on the rebalance plan
		if (feedforwardSignal.containsKey(timeBin)) {
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
					.groupRebalancableVehicles(truelyRebalancableVehicles.stream(), time,
							generalParams.getMinServiceTime());
			// Generate relocations based on the "rebalancePlanCore"
			for (Flow<DrtZone, DrtZone> rebalanceInfo : feedforwardSignal.get(timeBin)) {
				DrtZone departureZone = rebalanceInfo.origin;
				DrtZone arrivalZone = rebalanceInfo.destination;
				int vehicleToSend = (int) Math.floor(scale * rebalanceInfo.amount + rnd.nextDouble());
				// Note: we use probability to solve the problem of non-integer value of
				// vehileToSend after scaling.
				int numVehiclesInZone = 0;
				if (rebalancableVehiclesPerZone.get(departureZone) != null) {
					numVehiclesInZone = rebalancableVehiclesPerZone.get(departureZone).size();
				}

				if (vehicleToSend > numVehiclesInZone) {
					vehicleToSend = numVehiclesInZone;
				}

				if (vehicleToSend > 0) {
					for (int i = 0; i < vehicleToSend; i++) {
						Link destinationLink = drtZoneTargetLinkSelector.selectTargetLink(arrivalZone);
						relocationList.add(
								new Relocation(rebalancableVehiclesPerZone.get(departureZone).get(0), destinationLink));
						rebalancableVehiclesPerZone.get(departureZone).remove(0);
					}
				}
			}
		}
		return relocationList;
	}

}
