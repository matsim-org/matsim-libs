package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the SINGLE DEPOT DISTRIBUTION/DELIVERY vrp problem.
 */

import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesCostAndTWs;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesLocalActInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesShipmentInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.StandardRouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
import org.matsim.core.gbl.MatsimRandom;

public class DTWSolverFactory implements MatsimVrpSolverFactory {

	private Random random = MatsimRandom.getRandom();

	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public MatsimVrpSolver createSolver(Carrier carrier, Network network, TourCost tourCost, VehicleRoutingCosts costs) {
		verifyDistributionProblem(carrier.getShipments(), carrier.getCarrierCapabilities().getCarrierVehicles());
		RouteAgentFactory spFactory = new StandardRouteAgentFactory(new CalculatesShipmentInsertion(costs, new CalculatesLocalActInsertion(costs)), new CalculatesCostAndTWs(costs)); 
		MatsimVrpSolverImpl rrSolver = new MatsimVrpSolverImpl(carrier,costs);
		RuinAndRecreateStandardAlgorithmFactory ruinAndRecreateFactory = new RuinAndRecreateStandardAlgorithmFactory(spFactory);
		rrSolver.setVrpSolverFactory(ruinAndRecreateFactory);
		return rrSolver;
	}

	private void verifyDistributionProblem(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles) {
		Id location = null;
		for (CarrierVehicle v : carrierVehicles) {
			if (location == null) {
				location = v.getLocation();
			} else if (!location.toString().equals(v.getLocation().toString())) {
				throw new IllegalStateException(
						"if you use this solver "
								+ this.getClass().toString()
								+ "), all vehicles must have the same depot-location. vehicle "
								+ v.getVehicleId() + " has not.");
			}
		}
		for (CarrierShipment s : shipments) {
			if (location == null) {
				return;
			}
			if (!s.getFrom().toString().equals(location.toString())) {
				throw new IllegalStateException(
						"if you use this solver, all shipments must have the same from-location. errorShipment "
								+ s);
			}
		}

	}

}
