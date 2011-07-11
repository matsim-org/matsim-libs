package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import vrp.basics.TourActivity;
import freight.ClarkeAndWrightSolver;
import freight.LocationsImpl;
import freight.VRPTransformation;

public class ClarkeAndWrightCarrierPlanBuilder {
	
	private static Logger logger = Logger.getLogger(ClarkeAndWrightCarrierPlanBuilder.class);
	
	private Network network;
	
	private VRPTransformation vrpTrafo;
	
	public ClarkeAndWrightCarrierPlanBuilder(Network network){
		this.network = network;
		iniTrafo();
	}
	
	private void iniTrafo() {
		LocationsImpl locations = new LocationsImpl();
		makeLocations(locations);
		vrpTrafo = new VRPTransformation(locations);
		
	}
	
	private void makeLocations(LocationsImpl locations) {
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
		new ClarkeAndWrightSolver(vrpSolution, vrpTrafo).solve(contracts, carrierCapabilities.getCarrierVehicles().iterator().next());
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
		return vrpTrafo.getShipment(customerId);
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
