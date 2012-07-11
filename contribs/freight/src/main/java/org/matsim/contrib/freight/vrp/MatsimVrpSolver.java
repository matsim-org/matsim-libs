package org.matsim.contrib.freight.vrp;

/**
 * This is a solver-hull triggering the vehicle routing algorithms. This hull only handles shipments, i.e. pickup and delivery activities 
 * (and no other acts). 
 * It 
 * (a) translates matsim-input, e.g. carrier-shipments and vehicles, to what the routing algorithm requires, 
 * (b) triggers the routing algorithm itself and 
 * (c) re-translates the results from the routing-algorithm to what is needed by the matsim-carrier, e.g. carrier-tours.
 * 
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;


class MatsimVrpSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(MatsimVrpSolver.class);
	
	private VehicleRoutingCosts costs;
	
	private RuinAndRecreateFactory rrFactory;

	private CarrierPlan initialPlan;
	
	private boolean withoutIniSolution = false;

	private Matsim2VrpMap matsim2vrp;

	MatsimVrpSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, VehicleRoutingCosts costs) {
		super();
		this.costs = costs;
		withoutIniSolution = true;
		this.matsim2vrp = new Matsim2VrpMap(shipments, vehicles);
	}
	
	MatsimVrpSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, VehicleRoutingCosts costs, CarrierPlan iniPlan) {
		super();
		this.costs = costs;
		this.initialPlan = iniPlan;
		this.matsim2vrp = new Matsim2VrpMap(shipments, vehicles);
	}

	public void setRuinAndRecreateFactory(RuinAndRecreateFactory ruinAndRecreateFactory) {
		this.rrFactory = ruinAndRecreateFactory;
	}


	/**
	 * Solves the vehicle routing problem resulting from specified by the carrier's resources and shipment-contracts. 
	 * And returns a collections of tours.
	 */

	public Collection<ScheduledTour> solve() {
		verify();
		if(matsim2vrp.getShipments().isEmpty()){
			return Collections.emptyList();
		}
		VehicleRoutingProblem vrp = setupProblem();
		logger.debug("problem: ");
		logger.debug("#jobs: " + vrp.getJobs().size());
		logger.debug("#print jobs");
		logger.debug(printJobs(vrp));
		RuinAndRecreate ruinAndRecreate = makeAlgorithm(vrp);
		ruinAndRecreate.run();
		logger.debug("");
		logger.debug(printTours(getTours(ruinAndRecreate.getSolution())));
		Collection<ScheduledTour> tours = makeScheduledVehicleTours(ruinAndRecreate.getSolution());
		return tours;
	}

	private Collection<Tour> getTours(RuinAndRecreateSolution solution) {
		List<Tour> tours = new ArrayList<Tour>();
		for(TourAgent a : solution.getTourAgents()){
			tours.add(a.getTour());
		}
		return tours;
	}

	private String printTours(Collection<Tour> solution) {
		String tourString = "";
		for(Tour t : solution){
			tourString += t + "\n";
		}
		return tourString;
	}

	private String printJobs(VehicleRoutingProblem vrp) {
		String jobs = "";
		for(Job j : vrp.getJobs().values()){
			Shipment s = (Shipment)j;
			jobs+= s + "\n";
		}
		return jobs;
	}

	private VehicleRoutingProblem setupProblem() {
		VehicleRoutingProblem vrp = new VrpFactory(matsim2vrp, costs).createVrp();
		return vrp;
	}

	private void verify() {
		if(rrFactory == null){
			throw new IllegalStateException("ruinAndRecreateFactory is null but must be set");
		}
		if(matsim2vrp.getVehicles().isEmpty()){
			throw new IllegalStateException("cannot route vehicles without vehicles");
		}
	}

	/*
	 * translates vrp-solution (being vrp-tours) to matsim-carrier-tours
	 */
	private Collection<ScheduledTour> makeScheduledVehicleTours(RuinAndRecreateSolution rrSolution) {
		return Matsim2VrpUtils.createTours(rrSolution, matsim2vrp);
	}

	private RuinAndRecreate makeAlgorithm(VehicleRoutingProblem vrp) {
		RuinAndRecreate ruinAndRecreateAlgo = null;
		if(withoutIniSolution){
			ruinAndRecreateAlgo = rrFactory.createAlgorithm(vrp); 
		}
		else{
			ruinAndRecreateAlgo = rrFactory.createAlgorithm(createTourPlan(initialPlan,vrp), vrp);
		}
		return ruinAndRecreateAlgo;
	}

	private TourPlan createTourPlan(CarrierPlan initialPlan, VehicleRoutingProblem vrp) {
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for(ScheduledTour sTour : initialPlan.getScheduledTours()){
			Vehicle vehicle = matsim2vrp.getVehicle(sTour.getVehicle());
			org.matsim.contrib.freight.carrier.Tour tour = sTour.getTour();
			Tour vrpTour = getVrpTour(vrp,tour,vehicle);
			routes.add(new VehicleRoute(vrpTour,vehicle));
		}
		TourPlan tourPlan = new TourPlan(routes);
		tourPlan.setScore(initialPlan.getScore());
		return tourPlan;
	}

	private Tour getVrpTour(VehicleRoutingProblem vrp, org.matsim.contrib.freight.carrier.Tour tour, Vehicle vehicle) {
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(tour.getStartLinkId().toString(), vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		for(TourElement tE : tour.getTourElements()){
			if(tE instanceof org.matsim.contrib.freight.carrier.Tour.Pickup){
				org.matsim.contrib.freight.carrier.Tour.Pickup pickup = (org.matsim.contrib.freight.carrier.Tour.Pickup) tE;
				tourBuilder.schedulePickup(matsim2vrp.getShipment(pickup.getShipment()));
			}
			else if(tE instanceof org.matsim.contrib.freight.carrier.Tour.Delivery){
				org.matsim.contrib.freight.carrier.Tour.Delivery delivery = (org.matsim.contrib.freight.carrier.Tour.Delivery) tE;
				tourBuilder.scheduleDelivery(matsim2vrp.getShipment(delivery.getShipment()));
			}
		}
		tourBuilder.scheduleEnd(tour.getEndLinkId().toString(), 0.0, vehicle.getLatestArrival());
		return tourBuilder.build();
	}
}
