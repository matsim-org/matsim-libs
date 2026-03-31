package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.kernel.NetworkKernelFunction;

import java.util.Map;

/**
 * This class calculates the parking search time based on the parking occupancy.
 * It plugs together the following components:
 * <ul>
 *     <li> NetworkKernelFunction: calculates the links within a certain radius of the vehicle ("kernel")
 *     <li> ParkingOccupancyObserver: observes the parking occupancy and returns the parking count per link
 *     <li> ParkingSearchTimeFunction: calculates the parking search time based on the parking occupancy
 * </ul>
 */
public class ParkingOccupancyObservingSearchTimeCalculator implements ParkingSearchTimeCalculator {
	@Inject
	private NetworkKernelFunction kernelFunction;

	@Inject
	private ParkingOccupancyObserver parkingOccupancyObserver;

	@Inject
	private ParkingSearchTimeFunction parkingSearchTimeFunction;

	@Override
	public double calculateParkingSearchTime(double now, QVehicle vehicle, Link link) {
		Map<Id<Link>, Double> weightedLinkIds = kernelFunction.calculateWeightedKernel(vehicle, link);
		Map<Id<Link>, ParkingCount> parkingCountPerLinkId = parkingOccupancyObserver.getParkingCount(now, weightedLinkIds);
		double res = parkingSearchTimeFunction.calculateParkingSearchTime(parkingCountPerLinkId);
		return res;
	}
}
