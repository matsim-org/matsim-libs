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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgent;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.constraints.Constraints;
import org.matsim.core.basic.v01.IdImpl;


public class ShipmentBasedVRPSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(ShipmentBasedVRPSolver.class);
	
	private MatSim2VRP matsim2vrp;
	
	private Network network;
	
	private Collection<CarrierShipment> shipments;
	
	private InitialSolutionFactory iniSolutionFactory = new InitialSolution();
	
	private Constraints constraints;
	
	private Costs costs;
	
	private int nOfIterations = 20;
	
	private int nOfWarmupIterations = 4;
	
	private RuinAndRecreateFactory rrFactory;

	private Collection<CarrierVehicle> vehicles;

	public ShipmentBasedVRPSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, Network network) {
		super();
		this.shipments = shipments;
		this.network = network;
		this.vehicles = vehicles;
		this.matsim2vrp = new MatSim2VRP();
	}

	public void setRuinAndRecreateFactory(RuinAndRecreateFactory ruinAndRecreateFactory) {
		this.rrFactory = ruinAndRecreateFactory;
	}

	public void setGlobalConstraints(Constraints constraints) {
		this.constraints = constraints;
	}

	public void setCosts(Costs costs) {
		this.costs = costs;
	}

	public void setnOfIterations(int nOfIterations) {
		this.nOfIterations = nOfIterations;
	}

	public void setnOfWarmupIterations(int nOfWarmupIterations) {
		this.nOfWarmupIterations = nOfWarmupIterations;
	}

	public void setIniSolutionFactory(InitialSolutionFactory iniSolutionFactory) {
		this.iniSolutionFactory = iniSolutionFactory;
	}

	/**
	 * Solves the vehicle routing problem resulting from specified by the carrier's resources and shipment-contracts. 
	 * And returns a collections of tours.
	 */
	@Override
	public Collection<ScheduledTour> solve() {
		verify();
		if(shipments.isEmpty()){
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

	private Collection<Tour> getTours(RRSolution solution) {
		List<Tour> tours = new ArrayList<Tour>();
		for(RRTourAgent a : solution.getTourAgents()){
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
		MatSimVRPBuilder vrpBuilder = new MatSimVRPBuilder(vehicles,matsim2vrp,network);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		for(CarrierShipment s : shipments){
			vrpBuilder.addShipment(s);
		}
		VehicleRoutingProblem vrp = vrpBuilder.buildVRP();
		return vrp;
	}

	private void verify() {
		if(iniSolutionFactory == null){
			throw new IllegalStateException("initialSolutionFactor is null but must be set");
		}
		if(rrFactory == null){
			throw new IllegalStateException("ruinAndRecreateFactory is null but must be set");
		}
		if(vehicles.isEmpty()){
			throw new IllegalStateException("cannot route vehicles without vehicles");
		}
	}

	/*
	 * translates vrp-solution (being vrp-tours) to matsim-carrier-tours
	 */
	private Collection<ScheduledTour> makeScheduledVehicleTours(RRSolution rrSolution) {
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(RRTourAgent a : rrSolution.getTourAgents()){
			if(!a.isActive()){
				continue;
			}
			Tour tour = a.getTour();
			TourBuilder tourBuilder = new TourBuilder();
			for(TourActivity act : tour.getActivities()){
				if(act instanceof Pickup){
					Shipment shipment = (Shipment)((Pickup)act).getJob();
					CarrierShipment carrierShipment = matsim2vrp.getCarrierShipment(shipment);
					tourBuilder.addLeg(new Leg());
					tourBuilder.schedulePickup(carrierShipment);
				}
				else if(act instanceof Delivery){
					Shipment shipment = (Shipment)((Delivery)act).getJob();
					CarrierShipment carrierShipment = matsim2vrp.getCarrierShipment(shipment);
					tourBuilder.addLeg(new Leg());
					tourBuilder.scheduleDelivery(carrierShipment);
				}
				else if(act instanceof Start){
					tourBuilder.scheduleStart(makeId(act.getLocationId()), act.getEarliestArrTime(), act.getLatestArrTime());
				}
				else if(act instanceof End){
					tourBuilder.addLeg(new Leg());
					tourBuilder.scheduleEnd(makeId(act.getLocationId()));
				}
				else {
					throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
				}
			}
			org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
			ScheduledTour scheduledTour = new ScheduledTour(vehicleTour, getCarrierVehicle(a.getVehicle()), vehicleTour.getEarliestDeparture());
			scheduledTours.add(scheduledTour);
		}
		return scheduledTours;
	}

	private CarrierVehicle getCarrierVehicle(Vehicle vehicle) {
		for(CarrierVehicle v : vehicles){
			if(v.getVehicleId().toString().equals(vehicle.getId())){
				return v;
			}
		}
		throw new IllegalStateException("cannot assign vehcile. vehicle " + vehicle.getId() + " not found in the set of carriers' vehicles.");
	}

	private Id makeId(String id) {
		return new IdImpl(id);
	}

	private RuinAndRecreate makeAlgorithm(VehicleRoutingProblem vrp) {
		rrFactory.setWarmUp(nOfWarmupIterations);
		rrFactory.setIterations(nOfIterations);
		RRSolution initialSolution = iniSolutionFactory.createInitialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createAlgorithm(vrp, initialSolution);
		return ruinAndRecreateAlgo;
	}
}
