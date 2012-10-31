package org.matsim.contrib.freight.vrp;

/**
 * MatsimVrpSolver prepares carrier problem such that it can be solved by the vehicle routing algorithm, and starts the routing algorithm. It only handles shipments, i.e. pickup and delivery activities 
 * (and no other acts). 
 * It 
 * (a) translates matsim-input, e.g. carrier-shipments and vehicles, to what the routing algorithm requires, 
 * (b) triggers the routing algorithm and 
 * (c) re-translates the results from the routing-algorithm to what is needed by the matsim-carrier, e.g. carrier-tours.
 * 
 */

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolverFactory;

class MatsimVrpSolverImpl implements MatsimVrpSolver {

	private static Logger logger = Logger.getLogger(MatsimVrpSolverImpl.class);

	private VehicleRoutingCosts costs;

	private VehicleRoutingProblemSolverFactory vrpSolverFactory;

	private Matsim2VrpMap matsim2vrp;

	private Carrier carrier;
	
	private boolean useSelectedPlanAsInitialOne = false;

	MatsimVrpSolverImpl(Carrier carrier,VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
		this.carrier = carrier;
		this.matsim2vrp = new Matsim2VrpMap(carrier.getShipments(), carrier.getCarrierCapabilities().getCarrierVehicles());
	}

	public void setVrpSolverFactory(VehicleRoutingProblemSolverFactory vrpSolverFactory) {
		this.vrpSolverFactory = vrpSolverFactory;
	}

	public void useSelectedPlanAsInitialSolution(boolean value) {
		useSelectedPlanAsInitialOne = value;
	}

	/**
	 * Solves the vehicle routing problem resulting from specified by the
	 * carrier's resources and shipments. And returns a collections of
	 * tours.
	 */

	@Override
	public Collection<ScheduledTour> solve() {
		verify();
		if (matsim2vrp.getShipments().isEmpty()) {
			return Collections.emptyList();
		}
		VehicleRoutingProblem vrp = setupProblem();
		logger.debug("problem: ");
		logger.debug("#jobs: " + vrp.getJobs().size());
		logger.debug("#print jobs");
		logger.debug(printJobs(vrp));
		VehicleRoutingProblemSolver solver = vrpSolverFactory.createSolver(vrp,getInitialSolution());
		VehicleRoutingProblemSolution solution = solver.solve();
		logger.debug("");
		logger.debug(printTours(solution.getRoutes()));
		Collection<ScheduledTour> tours = makeScheduledVehicleTours(solution.getRoutes());
		route(tours);
		return tours;
	}

	private void route(Collection<ScheduledTour> tours) {
		// TODO Auto-generated method stub
		
	}

	private VehicleRoutingProblemSolution getInitialSolution() {
		if(!useSelectedPlanAsInitialOne || carrier.getSelectedPlan() == null){
			return null;
		}
		Collection<VehicleRoute> routes = Matsim2VrpUtils.createVehicleRoutes(carrier.getSelectedPlan().getScheduledTours(), matsim2vrp);
		return new VehicleRoutingProblemSolution(routes, (-1)*carrier.getSelectedPlan().getScore());
	}

	private String printTours(Collection<VehicleRoute> solution) {
		String tourString = "";
		for (VehicleRoute r : solution) {
			tourString += r.getTour() + "\n";
		}
		return tourString;
	}

	private String printJobs(VehicleRoutingProblem vrp) {
		String jobs = "";
		for (Job j : vrp.getJobs().values()) {
			Shipment s = (Shipment) j;
			jobs += s + "\n";
		}
		return jobs;
	}

	private VehicleRoutingProblem setupProblem() {
		VehicleRoutingProblem vrp = new VrpFactory(matsim2vrp, costs).createVrp();
		return vrp;
	}

	private void verify() {
		if (vrpSolverFactory == null) {
			throw new IllegalStateException("ruinAndRecreateFactory is null but must be set");
		}
		if (matsim2vrp.getVehicles().isEmpty()) {
			throw new IllegalStateException("cannot route vehicles without vehicles");
		}
	}

	/*
	 * translates vrp-solution (being vrp-tours) to matsim-carrier-tours
	 */
	private Collection<ScheduledTour> makeScheduledVehicleTours(Collection<VehicleRoute> routes) {
		return Matsim2VrpUtils.createTours(routes, matsim2vrp);
	}
}
