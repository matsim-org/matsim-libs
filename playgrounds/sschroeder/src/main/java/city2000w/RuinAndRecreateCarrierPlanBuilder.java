package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.TourBuilder;
import vrp.algorithms.clarkeAndWright.ClarkeAndWright;
import vrp.algorithms.clarkeAndWright.ClarkeWrightCapacityConstraint;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.api.VRP;
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;

public class RuinAndRecreateCarrierPlanBuilder {
	
private static Logger logger = Logger.getLogger(RuinAndRecreateCarrierPlanBuilder.class);
	
	private Network network;
	
	private Map<Id,Shipment> customerIdToShipmentMap = new HashMap<Id,Shipment>();
	
	public RuinAndRecreateCarrierPlanBuilder(Network network){
		this.network = network;
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Collection<vrp.basics.Tour> vrpSolution = solveVRP(contracts,carrierCapabilities.getCarrierVehicles().iterator().next());
		for(CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = carrierVehicle.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			for(vrp.basics.Tour tour : vrpSolution){
				List<Pickup> pickupsAtDepot = new ArrayList<Pickup>();
				List<Delivery> deliveriesAtDepot = new ArrayList<Delivery>();
				List<TourElement> enRouteActivities = new ArrayList<Tour.TourElement>();
				for(TourActivity act : tour.getActivities()){
					Shipment shipment = getShipment(act.getCustomer().getId());
					if(act instanceof vrp.basics.Delivery){
						pickupsAtDepot.add(new Pickup(shipment));
						enRouteActivities.add(new Delivery(shipment));
						continue;
					}
					if(act instanceof vrp.basics.Pickup){
						enRouteActivities.add(new Pickup(shipment));
						deliveriesAtDepot.add(new Delivery(shipment));
						continue;
					}
					if(act instanceof vrp.basics.EnRouteDelivery){
						enRouteActivities.add(new Delivery(shipment));
					}
					if(act instanceof vrp.basics.EnRoutePickup){
						enRouteActivities.add(new Pickup(shipment));
					}
				}
				List<TourElement> tourActivities = new ArrayList<Tour.TourElement>();
				tourActivities.addAll(pickupsAtDepot);
				tourActivities.addAll(enRouteActivities);
				tourActivities.addAll(deliveriesAtDepot);
				for(TourElement e : tourActivities){
					tourBuilder.schedule(e);
				}
			}
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		return carrierPlan;
	}
		

	private Shipment getShipment(Id customerId) {
		return customerIdToShipmentMap.get(customerId);
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

	private Collection<vrp.basics.Tour> solveVRP(Collection<Contract> contracts, CarrierVehicle carrierVehicle) {
		Id depotId = findDepotId(contracts);
		VrpBuilder vrpBuilder = new VrpBuilder(depotId, network, customerIdToShipmentMap);
		vrpBuilder.setConstraints(new CapacityConstraint(carrierVehicle.getCapacity()));
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			vrpBuilder.addShipment(s);
		}
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		Collection<vrp.basics.Tour> initialSolution = VrpUtils.createTrivialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.run();
		return ruinAndRecreateAlgo.getSolution();
	}

	private Id findDepotId(Collection<Contract> contracts) {
		for(Contract c : contracts){
			return c.getShipment().getFrom();
		}
		throw new RuntimeException("no contracts or shipments");
	}
}
