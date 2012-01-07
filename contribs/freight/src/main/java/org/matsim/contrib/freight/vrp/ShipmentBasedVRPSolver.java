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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.vrp.algorithms.rr.InitialSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.basics.Constraints;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.core.basic.v01.IdImpl;


public class ShipmentBasedVRPSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(ShipmentBasedVRPSolver.class);
	
	private MatSim2VRP matsim2vrp;
	
	private Network network;
	
	private Collection<CarrierShipment> shipments;
	
	private SingleDepotInitialSolutionFactory iniSolutionFactory = new InitialSolution();
	
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

	public void setConstraints(Constraints constraints) {
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

	public void setIniSolutionFactory(SingleDepotInitialSolutionFactory iniSolutionFactory) {
		this.iniSolutionFactory = iniSolutionFactory;
	}

	/**
	 * Solves the vehicle routing problem resulting from specified by the carrier's resources and shipment-contracts. 
	 * And returns a collections of tours.
	 */
	@Override
	public Collection<org.matsim.contrib.freight.carrier.Tour> solve() {
		verify();
		if(shipments.isEmpty()){
			return Collections.emptyList();
		}
		VehicleRoutingProblem vrp = setupProblem();
		RuinAndRecreate ruinAndRecreate = makeAlgorithm(vrp);
		ruinAndRecreate.run();
		Collection<org.matsim.contrib.freight.carrier.Tour> tours = makeVehicleTours(ruinAndRecreate.getSolution());
		return tours;
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
	private Collection<org.matsim.contrib.freight.carrier.Tour> makeVehicleTours(Collection<org.matsim.contrib.freight.vrp.basics.Tour> vrpSolution) {
		Collection<org.matsim.contrib.freight.carrier.Tour> tours = new ArrayList<org.matsim.contrib.freight.carrier.Tour>();
		for(org.matsim.contrib.freight.vrp.basics.Tour tour : vrpSolution){
			TourBuilder tourBuilder = new TourBuilder();
			for(TourActivity act : tour.getActivities()){
				if(act instanceof Pickup){
					Shipment shipment = (Shipment)((Pickup)act).getJob();
					CarrierShipment carrierShipment = matsim2vrp.getCarrierShipment(shipment);
					tourBuilder.schedulePickup(carrierShipment);
				}
				else if(act instanceof Delivery){
					Shipment shipment = (Shipment)((Delivery)act).getJob();
					CarrierShipment carrierShipment = matsim2vrp.getCarrierShipment(shipment);
					tourBuilder.scheduleDelivery(carrierShipment);
				}
				else if(act instanceof Start){
					tourBuilder.scheduleStart(makeId(act.getLocationId()));
					tourBuilder.setTourStartTimeWindow(act.getEarliestArrTime(), act.getLatestArrTime());
				}
				else if(act instanceof End){
					tourBuilder.scheduleEnd(makeId(act.getLocationId()));
				}
				else {
					throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
				}
			}
			org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
			tours.add(vehicleTour);
		}
		return tours;
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
