package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.kernel.NetworkKernelFunction;

import java.util.Map;

public class ParkingOccupancyObservingSearchTimeCalculator implements ParkingSearchTimeCalculator {
	@Inject
	private ParkingSearchTimeFunction parkingSearchTimeFunction;

	@Inject
	private ParkingObserver parkingObserver;

	@Inject
	private NetworkKernelFunction kernelFunction;


	@Override
	public double calculateParkingSearchTime(QVehicle vehicle, Link link) {
		Map<Id<Link>, Double> weightedLinkIds = kernelFunction.calculateWeightedKernel(vehicle, link);
		Map<Id<Link>, ParkingCount> idParkingCountMap = parkingObserver.getParkingCount(weightedLinkIds);
		return parkingSearchTimeFunction.calculateParkingSearchTime(idParkingCountMap);
	}

	public interface Factory {
		ParkingOccupancyObservingSearchTimeCalculator create(double kernelDistance);
	}
}
