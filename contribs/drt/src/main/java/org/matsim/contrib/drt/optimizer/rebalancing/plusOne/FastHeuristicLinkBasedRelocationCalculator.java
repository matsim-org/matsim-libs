package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.util.distance.DistanceUtils;

public class FastHeuristicLinkBasedRelocationCalculator implements LinkBasedRelocationCalculator {

	@Override
	public List<Relocation> calcRelocations(List<Link> targetLinks, List<? extends DvrpVehicle> rebalancableVehicles) {
		List<Relocation> relocations = new ArrayList<>();
		for (Link destinationLink : targetLinks) {
			if (!rebalancableVehicles.isEmpty()) {
				DvrpVehicle nearestVehicle = findNearestVehicle(destinationLink, rebalancableVehicles);
				relocations.add(new Relocation(nearestVehicle, destinationLink));
				rebalancableVehicles.remove(nearestVehicle);
			}
		}
		return relocations;
	}

	private DvrpVehicle findNearestVehicle(Link targetLink, List<? extends DvrpVehicle> rebalancableVehicles) {
		Coord toCoord = targetLink.getFromNode().getCoord();
		return rebalancableVehicles.stream()
				.min(Comparator.comparing(v -> DistanceUtils.calculateSquaredDistance(
						Schedules.getLastLinkInSchedule(v).getToNode().getCoord(), toCoord)))
				.get();
	}
}
