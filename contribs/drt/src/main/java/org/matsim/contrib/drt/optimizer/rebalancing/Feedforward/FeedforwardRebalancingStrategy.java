package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.toolbox.VehicleInfoCollector;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.core.network.NetworkUtils;

public class FeedforwardRebalancingStrategy implements RebalancingStrategy {
	private final DrtZonalSystem zonalSystem;
	private final FeedforwardRebalancingParams params;
	private final Network network;
	private final VehicleInfoCollector vehicleInfoCollector;

	private final double rebalanceInterval;
	private final double scale;
	private final Random rnd = new Random(1234);
	// temporary parameter (to be moved to the parameter file) //TODO
	private final int timeBinSize = 900; // size of time bin in second
	private final Map<Double, List<Triple<String, String, Integer>>> rebalancePlanCore;

	// TODO testing
	public FeedforwardRebalancingStrategy(DrtZonalSystem zonalSystem, Fleet fleet, Network network,
			FeedforwardRebalancingParams params) {
		this.network = network;
		this.zonalSystem = zonalSystem;
		this.params = params;

		rebalanceInterval = params.getInterval();
		vehicleInfoCollector = new VehicleInfoCollector(fleet, zonalSystem);
		scale = rebalanceInterval / timeBinSize;

		rebalancePlanCore = PreviousIterationDepartureRecorder.getRebalancePlanCore();
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		// assign rebalnace vehicles based on the rebalance plan
		System.out.println("Rebalance fleet now: Feedforward Rebalancing Strategy is used");

		double timeBin = Math.floor(time / timeBinSize);
		List<Relocation> relocationList = new ArrayList<>();

		if (rebalancePlanCore.containsKey(timeBin)) {
			Map<String, List<DvrpVehicle>> rebalancableVehiclesPerZone = vehicleInfoCollector
					.groupRebalancableVehicles(rebalancableVehicles, timeBin, params.getMinServiceTime());
			// Generate relocations based on the "rebalancePlanCore"
			for (Triple<String, String, Integer> rebalanceInfo : rebalancePlanCore.get(timeBin)) {
				String departureZoneId = rebalanceInfo.getLeft();
				String arrivalZoneId = rebalanceInfo.getMiddle();
				int vehicleToSend = (int) Math.floor(scale * rebalanceInfo.getRight() + rnd.nextDouble());
				// Note: we use probability to solve the problem of non-integer value of
				// vehileToSend after scaling.
				int numVehiclesInZone = 0;
				if (rebalancableVehiclesPerZone.get(departureZoneId) != null) {
					numVehiclesInZone = rebalancableVehiclesPerZone.get(departureZoneId).size();
				}

				if (vehicleToSend > numVehiclesInZone) {
					vehicleToSend = numVehiclesInZone;
				}
				
				if (vehicleToSend > 0) {
					for (int i = 0; i < vehicleToSend; i++) {
						// TODO change to "send to random link in a node"
						Link destinationLink = NetworkUtils.getNearestLink(network,
								zonalSystem.getZoneCentroid(arrivalZoneId));
						relocationList.add(new Relocation(rebalancableVehiclesPerZone.get(departureZoneId).get(0),
								destinationLink));
						rebalancableVehiclesPerZone.get(departureZoneId).remove(0);
					}
				}
			}
		}
		return relocationList;
	}

}
