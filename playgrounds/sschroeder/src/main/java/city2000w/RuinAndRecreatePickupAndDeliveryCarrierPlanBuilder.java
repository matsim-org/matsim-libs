package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;
import freight.VRPTransformation;

public class RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder {
	
private static Logger logger = Logger.getLogger(RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder.class);
	
	private Network network;
	
	private MarginalCostCalculator marginalCostCalculator = new MarginalCostCalculator();
	
	private VRPTransformation vrpTrafo;
	
	public void setMarginalCostCalculator(
			MarginalCostCalculator marginalCostCalculator) {
		this.marginalCostCalculator = marginalCostCalculator;
	}

	public RuinAndRecreatePickupAndDeliveryCarrierPlanBuilder(Network network){
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
				List<TourElement> enRouteActivities = new ArrayList<Tour.TourElement>();
				for(TourActivity act : tour.getActivities()){
					Shipment shipment = getShipment(act.getCustomer());
					if(act instanceof vrp.basics.EnRouteDelivery){
						enRouteActivities.add(new Delivery(shipment));
					}
					if(act instanceof vrp.basics.EnRoutePickup){
						enRouteActivities.add(new Pickup(shipment));
					}
				}
				List<TourElement> tourActivities = new ArrayList<Tour.TourElement>();
				tourActivities.addAll(enRouteActivities);
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
		

	private Shipment getShipment(Customer customer) {
		return vrpTrafo.getShipment(customer.getId());
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
		Id depotId = carrierVehicle.getLocation();
		VrpBuilder vrpBuilder = new VrpBuilder(depotId, network);
		vrpBuilder.setConstraints(new CapacityConstraint(carrierVehicle.getCapacity()));
		vrpTrafo = new VRPTransformation(network);
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			vrpTrafo.addShipment(s);
		}
		vrpBuilder.setVrpTrafo(vrpTrafo);
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		rrFactory.addRecreationListener(marginalCostCalculator);
		Collection<vrp.basics.Tour> initialSolution = VrpUtils.createTrivialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.run();
		logger.info(carrierVehicle.getVehicleId().toString());
		marginalCostCalculator.finish();
		return ruinAndRecreateAlgo.getSolution();
	}
}
