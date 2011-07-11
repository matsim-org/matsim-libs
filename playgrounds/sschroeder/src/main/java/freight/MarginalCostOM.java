package freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.TourBuilder;
import vrp.algorithms.ruinAndRecreate.api.ServiceProvider;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder;
import vrp.algorithms.ruinAndRecreate.basics.TourActivityStatusUpdaterImpl;
import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder.TourResult;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.algorithms.ruinAndRecreate.constraints.TWAndCapacityConstraint;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.Tour;
import vrp.basics.VrpUtils;
import city2000w.VrpBuilder;

public class MarginalCostOM implements OfferMaker{
	
	public static class ServiceProviderImpl implements ServiceProvider {

		private BestTourBuilder tourBuilder = new BestTourBuilder();
		
		private TourActivityStatusUpdaterImpl updater;
		
		private Tour tour;
		
		public ServiceProviderImpl(Tour tour, int vehicleCapacity) {
			super();
			this.tour = tour;
			Costs costs = new CrowFlyDistance();
			updater = new TourActivityStatusUpdaterImpl(costs);
			tourBuilder = new BestTourBuilder();
			tourBuilder.setConstraints(new TWAndCapacityConstraint(vehicleCapacity));
			tourBuilder.setCosts(costs);
			tourBuilder.setTourActivityStatusUpdater(updater);
		}
		
		public double getCostsOfCurrentTour(){
			updater.update(tour);
			return updater.getTourCost();
		}

		@Override
		public vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer requestService(vrp.algorithms.ruinAndRecreate.basics.Shipment shipment) {
			TourResult tourTrippel = tourBuilder.buildTour(tour, shipment);
			if(tourTrippel != null){
				vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer offer = new vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer(this, tourTrippel.marginalCosts);
				return offer;
			}
			else{
				return null;
			}
		}

		@Override
		public void offerGranted(
				vrp.algorithms.ruinAndRecreate.basics.Shipment shipment) {
			throw new UnsupportedOperationException();
			
		}

		@Override
		public void offerRejected(
				vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer offer) {
			throw new UnsupportedOperationException();
			
		}
		
	}
	
	private CarrierImpl carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private Collection<ServiceProvider> serviceProviders;
	
	VRPTransformation vrpTransformation;
	
	VRP vrp = null;
	
	public MarginalCostOM(CarrierImpl carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
		serviceProviders = new ArrayList<ServiceProvider>();
		vrpTransformation = new VRPTransformation(this.locations);
		vrpTransformation.addAndCreateCustomer("depot", carrierVehicle.getLocation(), 0, 0.0, 24*3600, 0.0);
		createServiceProvidersFromExistingPlan();
	}

	private void createServiceProvidersFromExistingPlan() {
		if(carrier.getSelectedPlan() == null){
			return;
		}
		for(Contract c : carrier.getContracts()){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		for(ScheduledTour t : carrier.getSelectedPlan().getScheduledTours()){
			vrp.basics.Tour tour = makeTour(t.getTour(),vrpTransformation);
			ServiceProvider serviceProvider = new ServiceProviderImpl(tour, carrierVehicle.getCapacity());
			serviceProviders.add(serviceProvider);
		}
	}

	@Override
	public Offer requestOffer(Id linkId, Id linkId2, int shipmentSize,Double memorizedPrice) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Offer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		Shipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTransformation.addShipment(requestedShipment);
		Offer bestOffer = new Offer();
		bestOffer.setCarrierId(carrier.getId());
		vrp.algorithms.ruinAndRecreate.basics.Shipment shipment = VrpUtils.createShipment(vrpTransformation.getFromCustomer(requestedShipment), vrpTransformation.getToCustomer(requestedShipment));
		if(!serviceProviders.isEmpty()){
			vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer cheapestOffer = null;
			for(ServiceProvider sP : serviceProviders){
				vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer o = sP.requestService(shipment);
				if(o != null){
					if(cheapestOffer == null){
						cheapestOffer = o;
					}
					else{
						if(o.getPrice() < cheapestOffer.getPrice()){
							cheapestOffer = o;
						}
					}
				}
			}
			if(cheapestOffer != null){
				bestOffer.setPrice(cheapestOffer.getPrice());
			}
		}
		ServiceProviderImpl sP = new ServiceProviderImpl(VrpUtils.createRoundTour(vrpTransformation.getCustomer(makeId("depot")), shipment.getFrom(), shipment.getTo()), carrierVehicle.getCapacity());
		vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer o = new vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer(sP,sP.getCostsOfCurrentTour());
		Offer offer = new Offer();
		offer.setCarrierId(carrier.getId());
		offer.setPrice(sP.getCostsOfCurrentTour());
		if(bestOffer.getPrice() < offer.getPrice()){
			return bestOffer;
		}
		else{
			return offer;
		}
	}
	
	private Tour makeTour(playground.mzilske.freight.Tour tour, VRPTransformation vrpTrafo) {
		Tour vrpTour = new Tour();
		Customer depotCustomer = vrpTrafo.getCustomer(makeId("depot"));
		vrpTour.getActivities().add(VrpUtils.createTourActivity(depotCustomer));
		for(TourElement tE : tour.getTourElements()){
			if(tE instanceof Pickup){
				Customer c = vrpTrafo.getFromCustomer(tE.getShipment());
				vrpTour.getActivities().add(VrpUtils.createTourActivity(c));
			}
			if(tE instanceof Delivery){
				Customer c = vrpTrafo.getToCustomer(tE.getShipment());
				vrpTour.getActivities().add(VrpUtils.createTourActivity(c));
			}
		}
		vrpTour.getActivities().add(VrpUtils.createTourActivity(depotCustomer));
		return vrpTour;
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

}
