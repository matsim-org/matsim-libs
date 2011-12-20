package org.matsim.contrib.freight.vrp;

/**
 * RRSingleDepotVRPSolver solves standard problems with one depot, i.e. given a set of customers and shipments (depot2customer, customer2depot, 
 * enRouteCustomer2Customer) the solver solves the vehicle routing problem with a ruin-and-recreate algorithm configured in the RuinAndRecreateFactory.
 * Thus, RRSingleDepotVRPSolver is the interface between a MatSim-Carrier with its shipments as well as vehicles and the vrp-algorithm. It transforms 
 * the Carrier-Problem into a VehicleRoutingProblem, runs the vehicleRoutingEngine and transforms the results of the engine into MatSim-tours.
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
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.VehicleType;



public class ShipmentBasedSingleDepotVRPSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(ShipmentBasedSingleDepotVRPSolver.class);
	
	private MatSim2VRP matsim2vrp;
	
	private Network network;
	
	private Collection<CarrierShipment> shipments;
	
	private Id depotLocation;
	
	private VehicleType vehicleType;
	
	private SingleDepotInitialSolutionFactory iniSolutionFactory = new InitialSolution();
	
	private Constraints constraints;
	
	private Costs costs;
	
	private int nOfIterations = 20;
	
	private int nOfWarmupIterations = 4;
	
	private RuinAndRecreateFactory rrFactory;

	private Collection<CarrierVehicle> vehicles;

	public ShipmentBasedSingleDepotVRPSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, Network network) {
		super();
		this.shipments = shipments;
		makeDepot(vehicles);
		this.network = network;
		this.vehicles = vehicles;
		this.matsim2vrp = makeMatsim2VRP();
		verifyVehicleCapAndLocations();
	}

	private void verifyVehicleCapAndLocations() {
		// TODO Auto-generated method stub
		
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

	private void makeDepot(Collection<CarrierVehicle> vehicles) {
		for(CarrierVehicle v : vehicles){
			if(depotLocation == null){
				depotLocation = v.getLocation();
			}
			else{
				if(!v.getLocation().equals(depotLocation)){
					throw new IllegalStateException("multipe locations for vehicles are not supported yet");
				}
			}
		}
	}

	@Override
	public Collection<org.matsim.contrib.freight.carrier.Tour> solve() {
		verify();
		if(shipments.isEmpty()){
			return Collections.emptyList();
		}
		if(vehicleType == null){
			logger.info("cannot do vehicle routing without vehicles");
			return Collections.emptyList();
		}
		VehicleRoutingProblem vrp = setupProblem();
		RuinAndRecreate ruinAndRecreate = makeAlgorithm(vrp);
		ruinAndRecreate.run();
		Collection<org.matsim.contrib.freight.carrier.Tour> tours = makeVehicleTours(ruinAndRecreate.getSolution());
		return tours;
	}

	private VehicleRoutingProblem setupProblem() {
		MatSimSingleDepotVRPBuilder vrpBuilder = new MatSimSingleDepotVRPBuilder(depotLocation,vehicles,matsim2vrp,network);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		for(CarrierShipment s : shipments){
			vrpBuilder.addShipment(s, 0.0, 0.0);
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
		
	}

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
				if(act instanceof Delivery){
					Shipment shipment = (Shipment)((Pickup)act).getJob();
					CarrierShipment carrierShipment = matsim2vrp.getCarrierShipment(shipment);
					tourBuilder.scheduleDelivery(carrierShipment);
				}
				if(act instanceof Start){
					tourBuilder.scheduleStart(makeId(act.getLocationId()));
				}
				if(act instanceof End){
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

	private MatSim2VRP makeMatsim2VRP() {
		return new MatSim2VRP();
	}
	

	private RuinAndRecreate makeAlgorithm(VehicleRoutingProblem vrp) {
		rrFactory.setWarmUp(nOfWarmupIterations);
		rrFactory.setIterations(nOfIterations);
		RRSolution initialSolution = iniSolutionFactory.createInitialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createAlgorithm(vrp, initialSolution);
		return ruinAndRecreateAlgo;
	}
}
