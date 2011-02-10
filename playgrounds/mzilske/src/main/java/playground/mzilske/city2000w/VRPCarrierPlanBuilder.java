package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;
import api.basic.VRP;
import api.basic.VehicleTour;
import core.algorithms.ClarkAndWright;
import core.algorithms.HomeTourOpt;
import core.algorithms.TwoMergeOpt;
import core.basic.CapacityConstraint;
import core.basic.Node;
import core.basic.VRPCrowFlyBuilder;
import core.basic.VRPSolution;
import core.basic.VehicleTourFactory;

public class VRPCarrierPlanBuilder {

	private static Logger logger = Logger.getLogger(VRPCarrierPlanBuilder.class); 
	
	private Network network;
	
	private Map<Id,Shipment> nodeIdToShipmentMap = new HashMap<Id, Shipment>();
	
	public VRPCarrierPlanBuilder(Network network) {
		super();
		this.network = network;
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		VRPSolution solution = solveVehicleRoutingProblem(contracts, carrierCapabilities.getCarrierVehicles());
		//!!!!!!!!!! homeLocation
		Collection<ScheduledTour> scheduledTours = scheduleVehicles(carrierCapabilities.getCarrierVehicles(), solution);
		
		return new CarrierPlan(scheduledTours);
	}

	private double getVehicleCapacity(Collection<CarrierVehicle> carrierVehicles) {
		return carrierVehicles.iterator().next().getCapacity();
	}

	private VRPSolution solveVehicleRoutingProblem(Collection<Contract> contracts, Collection<CarrierVehicle> vehicles) {
		double vehicleCapacity = getVehicleCapacity(vehicles);
		Coord carrierLocation = getCoord(vehicles.iterator().next().getLocation());
		VRPCrowFlyBuilder vrpBuilder = new VRPCrowFlyBuilder();
		Id depotId = findDepotId(contracts);
		vrpBuilder.createAndAddNode(depotId, network.getLinks().get(depotId).getCoord(), 0);
		vrpBuilder.setDepot(depotId);
		vrpBuilder.setConstraints(new CapacityConstraint(vehicleCapacity));
		Set<Id> idSet = new HashSet<Id>();
		int idCounter = 1;
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			Id to = s.getTo();
			Id nodeId = to;
			if(idSet.contains(nodeId)){
				nodeId = new IdImpl(to.toString() + "_" + idCounter);
				idCounter++;
			}
			idSet.add(nodeId);
			Coord toCoord = network.getLinks().get(to).getCoord();
			nodeIdToShipmentMap.put(nodeId, s);
			vrpBuilder.createAndAddNode(nodeId, toCoord, s.getSize());
		}
		VRP vrp = vrpBuilder.build();
		ClarkAndWright clarkAndWright = new ClarkAndWright(vrp, new VehicleTourFactory());
		clarkAndWright.construct();
		TwoMergeOpt twoOpt = new TwoMergeOpt(vrp);
		twoOpt.run();
		HomeTourOpt homeTourOpt = new HomeTourOpt(vrp, carrierLocation);
		homeTourOpt.run();
		return vrp.getSolution();
	}

	private Coord getCoord(Id location) {
		return network.getLinks().get(location).getCoord();
	}

	private Collection<ScheduledTour> scheduleVehicles(Collection<CarrierVehicle> carrierVehicles, VRPSolution solution) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(CarrierVehicle carrierVehicle : carrierVehicles){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = carrierVehicle.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			for(VehicleTour tour : solution.getTours()){
				List<Shipment> shipmentsOnTour = getShipments(tour);
				for(Shipment s : shipmentsOnTour){
					logger.info("schedulePickUp: " + s);
					tourBuilder.schedulePickup(s);
				}
				for(int i=1;i<tour.getNodes().size()-1;i++){
					Node nextDelivery = tour.getNodes().get(i);
					logger.info("scheduleDelivery: " + nodeIdToShipmentMap.get(nextDelivery.getId()));
					tourBuilder.scheduleDelivery(nodeIdToShipmentMap.get(nextDelivery.getId()));
				}
			}
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, 0.0);
			scheduledTours.add(scheduledTour);
		}
		return scheduledTours;
	}

	private List<Shipment> getShipments(VehicleTour tour) {
		List<Shipment> shipments = new ArrayList<Shipment>();
		for(Node n : tour.getNodes()){
			logger.info("Node: " + n);
			if(nodeIdToShipmentMap.containsKey(n.getId())){
				shipments.add(nodeIdToShipmentMap.get(n.getId()));
			}
		}
		return shipments;
	}

	private Id findDepotId(Collection<Contract> contracts) {
		if(!contracts.isEmpty()){
			return contracts.iterator().next().getShipment().getFrom();
		}
		throw new RuntimeException("no contracts or shipments");
	}


	private CarrierPlan getEmptyPlan(CarrierCapabilities carrierCapabilities) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(CarrierVehicle cv : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = cv.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, cv, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		return carrierPlan;
	}

}
