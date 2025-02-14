package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.kernel.NetworkKernelFunction;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;

public class ParkingOccupancyObservingSearchTimeCalculator implements ParkingSearchTimeCalculator {
	@Inject
	private ParkingSearchTimeFunction parkingSearchTimeFunction;

	@Inject
	private ParkingObserver parkingObserver;

	@Inject
	private NetworkKernelFunction kernelFunction;

	private final Map<Tuple<Id<Link>, Double>, Map<Id<Link>, Double>> kernelCache = new HashMap<>();

	//TODO
	private double distance = 100;

	@Override
	public double calculateParkingSearchTime(QVehicle vehicle, Link link) {
		Map<Id<Link>, Double> weightedLinkIds = kernelCache.computeIfAbsent(new Tuple<>(link.getId(), distance),
			k -> kernelFunction.calculateWeightedKernel(link, distance));
		Map<Id<Link>, ParkingCount> idParkingCountMap = parkingObserver.getParkingCount(weightedLinkIds);
		parkingSearchTimeFunction.calculateParkingSearchTime(idParkingCountMap);
		return 0;
	}
}
