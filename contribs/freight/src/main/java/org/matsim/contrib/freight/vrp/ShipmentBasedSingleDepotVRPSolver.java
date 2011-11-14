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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.api.Constraints;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.api.Customer;
import org.matsim.contrib.freight.vrp.api.Locations;
import org.matsim.contrib.freight.vrp.api.SingleDepotVRP;
import org.matsim.contrib.freight.vrp.basics.DeliveryFromDepot;
import org.matsim.contrib.freight.vrp.basics.PickupToDepot;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactory;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleType;
import org.matsim.core.basic.v01.IdImpl;



public class ShipmentBasedSingleDepotVRPSolver implements VRPSolver{
	
	private static Logger logger = Logger.getLogger(ShipmentBasedSingleDepotVRPSolver.class);
	
	private MatSim2VRPTransformation matsim2vrp;
	
	private Network network;
	
	private Collection<CarrierShipment> shipments;
	
	private Id depotLocation;
	
	private VehicleType vehicleType;
	
	private SingleDepotInitialSolutionFactory iniSolutionFactory = new SingleDepotInitialSolutionFactoryImpl();
	
	private Constraints constraints;
	
	private Costs costs;
	
	private int nOfIterations = 20;
	
	private int nOfWarmupIterations = 4;
	
	private RuinAndRecreateFactory rrFactory;

	public ShipmentBasedSingleDepotVRPSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> vehicles, Network network) {
		super();
		this.shipments = shipments;
		makeDepot(vehicles);
		makeVehicleType(vehicles);
		this.network = network;
		this.matsim2vrp = makeMatsim2VRP();
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

	private void makeVehicleType(Collection<CarrierVehicle> vehicles) {
		VehicleType minVehicleType = null;
		for(CarrierVehicle v : vehicles){
			if(vehicleType == null){
				vehicleType = new VehicleType(v.getCapacity());
				minVehicleType = vehicleType;
			}
			else{
				if(v.getCapacity() < minVehicleType.capacity){
					minVehicleType = new VehicleType(v.getCapacity());
					logger.warn("currently only one vehicleType at the depot is supported. the solver is run with the smallest vehicle");
				}
			}
		}
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
		RuinAndRecreate ruinAndRecreate = makeAlgorithm();
		ruinAndRecreate.run();
		double totDistance = 0.0;
		for(org.matsim.contrib.freight.vrp.basics.Tour t : ruinAndRecreate.getSolution()){
			logger.info(t);
			totDistance += t.costs.distance;
		}
		logger.info("total=" + totDistance);
		Collection<org.matsim.contrib.freight.carrier.Tour> tours = makeVehicleTours(ruinAndRecreate.getSolution());
		return tours;
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
			boolean tourStarted = false;
			List<CarrierShipment> pickupsAtDepot = new ArrayList<CarrierShipment>();
			List<CarrierShipment> deliveriesAtDepot = new ArrayList<CarrierShipment>();
			for(TourActivity act : tour.getActivities()){
				CarrierShipment shipment = getShipment(act.getCustomer());
				if(act instanceof DeliveryFromDepot){
					pickupsAtDepot.add(shipment);
				}
				if(act instanceof PickupToDepot){
					deliveriesAtDepot.add(shipment);
				}
			}
			for(TourActivity act : tour.getActivities()){
				CarrierShipment shipment = getShipment(act.getCustomer());
				if(act instanceof org.matsim.contrib.freight.vrp.basics.OtherDepotActivity){
					if(tourStarted){
						for(CarrierShipment s : deliveriesAtDepot){
							tourBuilder.scheduleDelivery(s);
						}
						tourBuilder.scheduleEnd(makeId(act.getCustomer().getLocation().getId()));
					}
					else{
						tourStarted = true;
						tourBuilder.scheduleStart(makeId(act.getCustomer().getLocation().getId()));
						for(CarrierShipment s : pickupsAtDepot){
							tourBuilder.schedulePickup(s);
						}
					}
				}
				else if(act instanceof org.matsim.contrib.freight.vrp.basics.DeliveryFromDepot){
					tourBuilder.scheduleDelivery(shipment);
				}
				else if(act instanceof org.matsim.contrib.freight.vrp.basics.PickupToDepot){
					tourBuilder.schedulePickup(shipment);
				}
				else if(act instanceof org.matsim.contrib.freight.vrp.basics.EnRoutePickup){
					tourBuilder.schedulePickup(shipment);
				}
				else if(act instanceof org.matsim.contrib.freight.vrp.basics.EnRouteDelivery){
					tourBuilder.scheduleDelivery(shipment);
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

	private MatSim2VRPTransformation makeMatsim2VRP() {
		return new MatSim2VRPTransformation(new Locations(){

			@Override
			public Coord getCoord(Id id) {
				if(network.getLinks().containsKey(id)){
					return network.getLinks().get(id).getCoord();
				}
				return null;
			}
			
		});
	}
	
	private CarrierShipment getShipment(Customer customer) {
		return matsim2vrp.getShipment(makeId(customer.getId()));
	}

	private RuinAndRecreate makeAlgorithm() {
		MatSimSingleDepotVRPBuilder vrpBuilder = new MatSimSingleDepotVRPBuilder(makeDepotId(),depotLocation,vehicleType,matsim2vrp);
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(constraints);
		for(CarrierShipment s : shipments){
			addShipmentToVRP(s,vrpBuilder);
		}
		SingleDepotVRP vrp = vrpBuilder.buildVRP();
		rrFactory.setWarmUp(nOfWarmupIterations);
		rrFactory.setIterations(nOfIterations);
		Collection<org.matsim.contrib.freight.vrp.basics.Tour> initialSolution = iniSolutionFactory.createInitialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createAlgorithm(vrp, initialSolution, vehicleType.capacity);
		return ruinAndRecreateAlgo;
	}

	private void addShipmentToVRP(CarrierShipment s,MatSimSingleDepotVRPBuilder vrpBuilder) {
		if(s.getFrom().equals(depotLocation)){
			vrpBuilder.addDeliveryFromDepotShipment(s);
		}
		else if(s.getTo().equals(depotLocation)){
			vrpBuilder.addPickupForDepotShipment(s);
		}
		else{
			vrpBuilder.addEnRoutePickupAndDeliveryShipment(s);
		}
	}

	private Id makeDepotId() {
		return new IdImpl("depot");
	}
}
