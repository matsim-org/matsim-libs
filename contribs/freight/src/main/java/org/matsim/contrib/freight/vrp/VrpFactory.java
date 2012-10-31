package org.matsim.contrib.freight.vrp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl.Type;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

class VrpFactory {

	private static Logger logger = Logger.getLogger(VrpFactory.class);

	private VehicleRoutingCosts costs;

	private Matsim2VrpMap matsim2vrpMap;

	VrpFactory(Matsim2VrpMap matsim2vrpMap,
			VehicleRoutingCosts vehicleRoutingCosts) {
		super();
		this.costs = vehicleRoutingCosts;
		this.matsim2vrpMap = matsim2vrpMap;
	}

	/**
	 * 
	 * @return
	 */
	public VehicleRoutingProblem createVrp() {
		verify();
		logProblem();
		VehicleRoutingProblem vrp = new VehicleRoutingProblem(
				matsim2vrpMap.getShipments(), matsim2vrpMap.getVehicles(),
				costs);
		return vrp;
	}

	private void logProblem() {
		logger.info("Vehicle Routing Problem #jobs: "
				+ matsim2vrpMap.getShipments().size() + " #vehicles: "
				+ matsim2vrpMap.getVehicles().size());
		Collection<Type> types = getTypes();
		logger.info("Available VehicleTypes #types: " + types.size());
		for (Type type : types) {
			logger.info("Type id=" + type.typeId + " fixCost="
					+ type.vehicleCostParams.fix + " costPerDistUnit="
					+ type.vehicleCostParams.perDistanceUnit
					+ " costPerTimeUnit=" + type.vehicleCostParams.perTimeUnit);
		}

	}

	private Collection<Type> getTypes() {
		Map<String, Type> types = new HashMap<String, Type>();
		for (Vehicle v : matsim2vrpMap.getVehicles()) {
			types.put(v.getType().typeId, v.getType());
		}
		return types.values();
	}

	private void verify() {
		if (costs == null) {
			throw new IllegalStateException("no costs set");
		}
	}

}
