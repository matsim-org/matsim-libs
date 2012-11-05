package org.matsim.contrib.freight.vrp;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.utils.matsim2vrp.MatsimPersonAdapter;
import org.matsim.contrib.freight.vrp.utils.matsim2vrp.MatsimVehicleAdapter;
import org.matsim.contrib.freight.vrp.utils.matsim2vrp.VRPVehicleAdapter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.Counter;

public class TransportCostCalculator implements VehicleRoutingCosts, TourCost {

	static class TransportDataKey {
		private final String from;
		private final String to;
		private final double time;
		private final String vehicleType;

		public TransportDataKey(String from, String to, double time,
				String vehicleType) {
			super();
			this.from = from;
			this.to = to;
			this.time = time;
			this.vehicleType = vehicleType;
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}

		public double getTime() {
			return time;
		}

		public String getVehicleType() {
			return vehicleType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			long temp;
			temp = Double.doubleToLongBits(time);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			result = prime * result
					+ ((vehicleType == null) ? 0 : vehicleType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransportDataKey other = (TransportDataKey) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (Double.doubleToLongBits(time) != Double
					.doubleToLongBits(other.time))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			if (vehicleType == null) {
				if (other.vehicleType != null)
					return false;
			} else if (!vehicleType.equals(other.vehicleType))
				return false;
			return true;
		}

	}

	static class TransportData {
		double transportCosts;
		double transportTime;

		public TransportData(double transportCosts, double transportTime) {
			super();
			this.transportCosts = transportCosts;
			this.transportTime = transportTime;
		}

	}

	private static Logger logger = Logger
			.getLogger(TransportCostCalculator.class);

	private LeastCostPathCalculator router;

	private Network network;

	private Map<TransportDataKey, TransportData> costCache = new HashMap<TransportDataKey, TransportData>();

	private int timeSliceWidth;

	private Counter ttMemorizedCounter;

	private Counter ttRequestedCounter;

	public TransportCostCalculator(LeastCostPathCalculator pathCalculator,Network network, int timeSliceWidth) {
		super();
		this.router = pathCalculator;
		this.network = network;
		this.timeSliceWidth = timeSliceWidth;
		this.ttMemorizedCounter = new Counter("numTravelTimes memorized ");
		this.ttRequestedCounter = new Counter("numTravelTimes requested ");
	}

	@Override
	public double getTourCost(TourImpl tour, Driver driver, Vehicle vehicle) {
		return vehicle.getType().vehicleCostParams.fix
				+ tour.tourData.transportCosts;
	}

	@Override
	public double getTransportCost(String fromId, String toId, double time,
			Driver driver, Vehicle vehicle) {
		if (fromId.equals(toId)) {
			return 0.0;
		}
		Id fromLinkId = new IdImpl(fromId);
		Id toLinkId = new IdImpl(toId);
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		if (vehicle == null) {
			Path path = router.calcLeastCostPath(fromLink.getToNode(),
					toLink.getFromNode(), time, null, null);
			return path.travelCost;
		}
		int timeSlice = getTimeSlice(time);
		String typeId = vehicle.getType().typeId;
		TransportDataKey transportDataKey = makeKey(fromId, toId, timeSlice,
				typeId);
		if (costCache.containsKey(transportDataKey)) {
			return costCache.get(transportDataKey).transportCosts;
		}

		VRPVehicleAdapter vrpVehicleAdapter = (VRPVehicleAdapter) vehicle;
		MatsimVehicleAdapter matsimVehicle = new MatsimVehicleAdapter(
				vrpVehicleAdapter);
		MatsimPersonAdapter matsimPerson = new MatsimPersonAdapter(driver);
		Path path = router.calcLeastCostPath(fromLink.getToNode(),
				toLink.getFromNode(), time, matsimPerson, matsimVehicle);
		memorize(transportDataKey, path);
		return path.travelCost;
	}

	@Override
	public double getTransportTime(String fromId, String toId, double time,
			Driver driver, Vehicle vehicle) {
		if (fromId.equals(toId)) {
			return 0.0;
		}
		// ttRequestedCounter.incCounter();
		int timeSlice = getTimeSlice(time);
		TransportDataKey transportDataKey = makeKey(fromId, toId, timeSlice,
				vehicle.getType().typeId);
		if (costCache.containsKey(transportDataKey)) {
			return costCache.get(transportDataKey).transportTime;
		}
		Id fromLinkId = new IdImpl(fromId);
		Id toLinkId = new IdImpl(toId);
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		VRPVehicleAdapter vrpVehicleAdapter = (VRPVehicleAdapter) vehicle;
		MatsimVehicleAdapter matsimVehicle = new MatsimVehicleAdapter(
				vrpVehicleAdapter);
		MatsimPersonAdapter matsimPerson = new MatsimPersonAdapter(driver);
		Path path = router.calcLeastCostPath(fromLink.getToNode(),
				toLink.getFromNode(), time, matsimPerson, matsimVehicle);
		memorize(transportDataKey, path);
		return path.travelTime;
	}

	private int getTimeSlice(double time) {
		int timeSlice = (int) time / timeSliceWidth;
		return timeSlice;
	}

	@Override
	public double getBackwardTransportCost(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle) {
		if (fromId.equals(toId)) {
			return 0.0;
		}
		double freeFlowTime = getTransportTime(fromId, toId, 0.0, driver,
				vehicle);
		return getTransportCost(fromId, toId, arrivalTime - freeFlowTime,
				driver, vehicle);
	}

	@Override
	public double getBackwardTransportTime(String fromId, String toId,
			double arrivalTime, Driver driver, Vehicle vehicle) {
		if (fromId.equals(toId)) {
			return 0.0;
		}
		double freeFlowTime = getTransportTime(fromId, toId, 0.0, driver,
				vehicle);
		return getTransportTime(fromId, toId, arrivalTime - freeFlowTime,
				driver, vehicle);
	}

	private void memorize(TransportDataKey costKey, Path path) {
		costCache.put(costKey, new TransportData(path.travelCost,
				path.travelTime));
		// this.ttMemorizedCounter.incCounter();
	}

	private TransportDataKey makeKey(String fromId, String toId, long time,
			String vehicleType) {
		return new TransportDataKey(fromId, toId, time, vehicleType);
	}

}
