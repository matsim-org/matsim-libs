package freight.offermaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierUtils;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import vrp.algorithms.ruinAndRecreate.api.ServiceProvider;
import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder;
import vrp.algorithms.ruinAndRecreate.basics.TourActivityStatusUpdaterImpl;
import vrp.algorithms.ruinAndRecreate.constraints.TWAndCapacityConstraint;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.Tour;
import vrp.basics.VrpUtils;
import freight.vrp.Locations;
import freight.vrp.VRPTransformation;

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
			return tour.costs.generalizedCosts;
		}

		@Override
		public vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer requestService(vrp.algorithms.ruinAndRecreate.basics.Shipment shipment, double bestKnownPrice) {
			Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, bestKnownPrice);
			if(newTour != null){
				double marginalCosts = newTour.costs.generalizedCosts - tour.costs.generalizedCosts;
				vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer offer = new vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer(this, marginalCosts);
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
	
	private static Logger logger = Logger.getLogger(MarginalCostOM.class);
	
	private Carrier carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private Collection<ServiceProviderImpl> serviceProviders;
	
	private Collection<ServiceProviderImpl> morningService;
	
	private Collection<ServiceProviderImpl> afternoonService;
	
	private VRPTransformation vrpTransformation;
	
	private VRP vrp = null;
	
	private Map<ServiceProvider,Tour> tours = new HashMap<ServiceProvider, Tour>();
	
	public MarginalCostOM(Carrier carrier, Locations locations) {
		super();
		this.carrier = carrier;
		carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		this.locations = locations;
		serviceProviders = new ArrayList<ServiceProviderImpl>();
		morningService = new ArrayList<ServiceProviderImpl>();
		afternoonService = new ArrayList<ServiceProviderImpl>();
		init();
	}

	private void createServiceProvidersFromExistingPlan() {
		if(carrier.getSelectedPlan() == null){
			return;
		}
		for(CarrierContract c : carrier.getContracts()){
			CarrierShipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		for(ScheduledTour t : carrier.getSelectedPlan().getScheduledTours()){
			vrp.basics.Tour tour = makeTour(t.getTour(),vrpTransformation);
			ServiceProviderImpl serviceProvider = new ServiceProviderImpl(tour, carrierVehicle.getCapacity());
			tours.put(serviceProvider, tour);
			if(isMorning(t)){
				morningService.add(serviceProvider);
			}
			else{
				afternoonService.add(serviceProvider);
			}
			serviceProviders.add(serviceProvider);
		}
	}
	

	private boolean isMorning(ScheduledTour t) {
		if(t.getDeparture() < 3600.0){
			return true;
		}
		return false;
	}

	@Override
	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize,Double memorizedPrice) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CarrierOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice) {
		if(memorizedPrice != null){
			CarrierOffer offer = new CarrierOffer();
			offer.setId(carrier.getId());
			offer.setPrice(memorizedPrice);
			return offer;
		}
		CarrierShipment requestedShipment = CarrierUtils.createShipment(from, to, size, startPickup, endPickup, startDelivery, endDelivery);
		vrpTransformation.addShipment(requestedShipment);
		CarrierOffer bestOffer = null;
		ServiceProviderImpl bestServiceProvider = null;
		vrp.algorithms.ruinAndRecreate.basics.Shipment shipment = VrpUtils.createShipment(vrpTransformation.getFromCustomer(requestedShipment), vrpTransformation.getToCustomer(requestedShipment));
		if(!serviceProviders.isEmpty()){
			vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer cheapestOffer = null;
			Tour bestTour = null;
			if(isMorning(startPickup)){
				for(ServiceProviderImpl sP : morningService){
					double totalCostsBefore = sP.getCostsOfCurrentTour();
					vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer o = sP.requestService(shipment,Double.MAX_VALUE);
					double totalCostsAfter = sP.getCostsOfCurrentTour();
					if(o != null){
						if(cheapestOffer == null){
							cheapestOffer = o;
							bestServiceProvider = sP;
						}
						else{
							if(o.getPrice() < cheapestOffer.getPrice()){
								cheapestOffer = o;
								bestServiceProvider = sP;
							}
						}
					}
				}
				if(cheapestOffer != null){
					bestOffer = new CarrierOffer();
					bestOffer.setPrice(cheapestOffer.getPrice());
					bestOffer.setId(carrier.getId());
				}
			}
			else{
				for(ServiceProviderImpl sP : afternoonService){
					vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer o = sP.requestService(shipment,Double.MAX_VALUE);
					if(o != null){
						if(cheapestOffer == null){
							cheapestOffer = o;
							bestServiceProvider = sP;
						}
						else{
							if(o.getPrice() < cheapestOffer.getPrice()){
								cheapestOffer = o;
								bestServiceProvider = sP;
							}
						}
					}
				}
				if(cheapestOffer != null){
					bestOffer = new CarrierOffer();
					bestOffer.setPrice(cheapestOffer.getPrice());
					bestOffer.setId(carrier.getId());
				}
			}	
		}
		Tour roundTour = VrpUtils.createRoundTour(vrpTransformation.getCustomer(makeId("depot")), shipment.getFrom(), shipment.getTo());
		ServiceProviderImpl sP = new ServiceProviderImpl(roundTour, carrierVehicle.getCapacity());
		vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer o = new vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer(sP,sP.getCostsOfCurrentTour());
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(sP.getCostsOfCurrentTour());
		if(bestOffer != null && bestOffer.getPrice() < offer.getPrice()){
			logger.info(carrier.getId() + " inserts " + requestedShipment + " into " + tours.get(bestServiceProvider) + "; costs=" + bestOffer.getPrice());
			return bestOffer;
		}
		else{
			logger.info(carrier.getId() + " inserts " + requestedShipment + " into " + roundTour + "; costs=" + offer.getPrice());
			return offer;
		}
	}
	

	private boolean isMorning(Double startPickup) {
		if(startPickup == 0.0){
			return true;
		}
		return false;
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

	@Override
	public void init() {
		vrpTransformation = new VRPTransformation(this.locations);
		vrpTransformation.addAndCreateCustomer("depot", carrierVehicle.getLocation(), 0, 0.0, 24*3600, 0.0);
		createServiceProvidersFromExistingPlan();
	}

	@Override
	public void reset() {
		vrpTransformation.clear();
		serviceProviders.clear();
	}

}
