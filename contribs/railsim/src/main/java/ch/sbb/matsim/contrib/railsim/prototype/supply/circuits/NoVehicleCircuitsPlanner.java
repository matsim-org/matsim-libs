package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteType;
import ch.sbb.matsim.contrib.railsim.prototype.supply.TransitLineInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleAllocationInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleCircuitsPlanner;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleTypeInfo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation for no circuits
 * <p>
 * Creates a new vehicle for each transit line vehicle allocation. Maintains a global counter of vehicles per type.
 *
 * @author Merlin Unterfinger
 */
public class NoVehicleCircuitsPlanner implements VehicleCircuitsPlanner {

	private static final HashMap<VehicleTypeInfo, Integer> counter = new HashMap<>();

	private record DummyVehicleAllocationInfo(TransitLineInfo transitLineInfo) implements VehicleAllocationInfo {

		@Override
		public LinkedList<Double> getDepartures(RouteType routeType, RouteDirection routeDirection) {
			if (routeType == RouteType.STATION_TO_STATION) {
				return new LinkedList<>(transitLineInfo.getDepartures(routeDirection));
			}
			return null;
		}

		@Override
		public LinkedList<String> getVehicleIds(RouteType routeType, RouteDirection routeDirection) {
			if (routeType == RouteType.STATION_TO_STATION) {
				final var vehicleTypeInfo = transitLineInfo.getVehicleTypeInfo();
				return IntStream.range(0, transitLineInfo.getDepartures(routeDirection).size()).mapToObj(i -> createVehicleId(vehicleTypeInfo)).collect(Collectors.toCollection(LinkedList::new));
			}
			return null;
		}
	}

	@Override
	public Map<TransitLineInfo, VehicleAllocationInfo> plan(List<TransitLineInfo> transitLineInfos) {
		counter.clear();
		return transitLineInfos.stream().collect(Collectors.toMap(Function.identity(), DummyVehicleAllocationInfo::new));
	}

	private static String createVehicleId(VehicleTypeInfo vehicleTypeInfo) {
		return String.format("%s_%s", vehicleTypeInfo.getId(), getAndIncreaseCount(vehicleTypeInfo));
	}

	private static int getAndIncreaseCount(VehicleTypeInfo vehicleTypeInfo) {
		int count = counter.getOrDefault(vehicleTypeInfo, 0);
		counter.put(vehicleTypeInfo, count + 1);
		return count;
	}

}
