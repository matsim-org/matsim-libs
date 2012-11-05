package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolverFactory;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;

public class MatsimVrpSolverFactoryImpl implements MatsimVrpSolverFactory {

	private VehicleRoutingProblemSolverFactory vrpSolverFactory;

	private VehicleRoutingProblemType vrpType;
	
	private boolean useSelectedPlan = false;

	public MatsimVrpSolverFactoryImpl(VehicleRoutingProblemSolverFactory solverFactory,VehicleRoutingProblemType vrpType) {
		super();
		this.vrpSolverFactory = solverFactory;
		this.vrpType = vrpType;
	}

	@Override
	public MatsimVrpSolver createSolver(Carrier carrier, Network network, TourCost tourCost, VehicleRoutingCosts costs) {
		verifyVehicleRouteProblem(carrier.getShipments(), carrier.getCarrierCapabilities().getCarrierVehicles());
		MatsimVrpSolverImpl rrSolver = new MatsimVrpSolverImpl(carrier,costs);
		rrSolver.setVrpSolverFactory(vrpSolverFactory);
		rrSolver.useSelectedPlanAsInitialSolution(useSelectedPlan);
		return rrSolver;
	}

	public void useSelectedPlanAsInitialSolution(boolean value) {
		useSelectedPlan = value;
	}

	protected void verifyVehicleRouteProblem(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles) {
		if (vrpType.equals(VehicleRoutingProblemType.CVRP)
				|| vrpType.equals(VehicleRoutingProblemType.CVRPTW)
				|| vrpType.equals(VehicleRoutingProblemType.TDCVRPTW)) {
			Id location = null;
			for (CarrierVehicle v : carrierVehicles) {
				if (location == null) {
					location = v.getLocation();
				} else if (!location.toString().equals(
						v.getLocation().toString())) {
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
		} else {
			throw new IllegalStateException(
					"this problem type is not yet supported");
		}

	}

}
