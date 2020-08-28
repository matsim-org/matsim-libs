package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

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
	private final Network network;
	private final VehicleInfoCollector vehicleInfoCollector;

	private final int timeBinSize;
	private final double rebalanceInterval;
	private final double scale;
	private final Random rnd = new Random(1234);
	private final int feedforwardSignalLead;

	private final boolean feedbackSwitch;
	private final int minNumVehiclesPerZone;

	private final Map<Double, List<Triple<DrtZone, DrtZone, Integer>>> rebalancePlanCore;

	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, Network network,
			RebalancingParams generalParams, FeedforwardRebalancingStrategyParams strategySpecificParams,
			FeedforwardSignalHandler feedforwardSignalHandler) {
		this.network = network;
		this.zonalSystem = zonalSystem;
		this.generalParams = generalParams;
		timeBinSize = strategySpecificParams.getTimeBinSize();

		rebalanceInterval = generalParams.getInterval();
		vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);

		scale = strategySpecificParams.getFeedforwardSignalStrength() * rebalanceInterval / timeBinSize;
		log.info("The feedforward signal strength is: "
				+ Double.toString(strategySpecificParams.getFeedforwardSignalStrength()));

		rebalancePlanCore = feedforwardSignalHandler.getRebalancePlanCore();
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
		Set<DvrpVehicle> truelyRebalancableVehicles = new HashSet<>();

		if (feedbackSwitch) {
			List<Link> destinationLinks = new ArrayList<>();
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
					.groupRebalancableVehicles(rebalancableVehicles, time, generalParams.getMinServiceTime());
			Map<DrtZone, List<DvrpVehicle>> soonRebalancableVehiclesPerZone = vehicleInfoCollector.groupSoonIdleVehicles(
					time, generalParams.getMaxTimeBeforeIdle(), generalParams.getMinServiceTime());
			for (DrtZone zone : zonalSystem.getZones().values()) {
				int surplus = rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size()
						+ soonRebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size()
						- minNumVehiclesPerZone;
				if (surplus > 0) {
					int numToAdd = Math.min(surplus,
							rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).size());
					for (int i = 0; i < numToAdd; i++) {
						truelyRebalancableVehicles
								.add(rebalancableVehiclesPerZone.getOrDefault(zone, new ArrayList<>()).get(i));
					}
				} else if (surplus < 0) {
					int deficit = -1 * surplus;
					for (int i = 0; i < deficit; i++) {
						Link destinationLink = NetworkUtils.getNearestLink(network, MGC.point2Coord(zone.getGeometry().getCentroid()));
						destinationLinks.add(destinationLink);
					}
				}
			}

			if (!truelyRebalancableVehicles.isEmpty()) {
				for (Link link : destinationLinks) {
					DvrpVehicle nearestVehicle = truelyRebalancableVehicles.stream().min(Comparator.comparing(
							v -> DistanceUtils.calculateSquaredDistance(Schedules.getLastLinkInSchedule(v).getCoord(),
									link.getCoord())))
							.get();
					relocationList.add(new Relocation(nearestVehicle, link));
					truelyRebalancableVehicles.remove(nearestVehicle);
					if (truelyRebalancableVehicles.isEmpty()) {
						break;
					}
				}
			}
		} else {
			truelyRebalancableVehicles.addAll(rebalancableVehicles.collect(Collectors.toList()));
			// This line is needed when feedback part is not enabled
		}

		// Feedforward part
		// assign rebalnace vehicles based on the rebalance plan
		if (rebalancePlanCore.containsKey(timeBin)) {
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector.groupRebalancableVehicles(
					truelyRebalancableVehicles.stream(), time, generalParams.getMinServiceTime());
			// Generate relocations based on the "rebalancePlanCore"
			for (Triple<DrtZone, DrtZone, Integer> rebalanceInfo : rebalancePlanCore.get(timeBin)) {
				DrtZone departureZone = rebalanceInfo.getLeft();
				DrtZone arrivalZone = rebalanceInfo.getMiddle();
				int vehicleToSend = (int) Math.floor(scale * rebalanceInfo.getRight() + rnd.nextDouble());
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
						// TODO change to "send to random link in a node"
						Link destinationLink = NetworkUtils.getNearestLink(network,
								MGC.point2Coord(arrivalZone.getGeometry().getCentroid()));
						relocationList.add(new Relocation(rebalancableVehiclesPerZone.get(departureZone).get(0),
								destinationLink));
						rebalancableVehiclesPerZone.get(departureZone).remove(0);
					}
				}
			}
		}
		return relocationList;
	}

}
