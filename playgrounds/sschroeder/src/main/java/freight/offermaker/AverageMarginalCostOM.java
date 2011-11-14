package freight.offermaker;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.vrp.MatSim2VRPTransformation;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.ServiceProvider;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.BestTourBuilder;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.TourActivityStatusUpdaterImpl;
import org.matsim.contrib.freight.vrp.algorithms.rr.constraints.TWAndCapacityConstraint;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.api.Customer;
import org.matsim.contrib.freight.vrp.api.Locations;
import org.matsim.contrib.freight.vrp.api.Offer;
import org.matsim.contrib.freight.vrp.api.VRP;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mzilske.freight.OfferMaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AverageMarginalCostOM implements OfferMaker{
	
	public static class AvgMarginalCostCalculator {
		private Collection<CarrierContract> currentContracts;
		
		private Network network;
		
		private Id depotLocation;

		public AvgMarginalCostCalculator(Collection<CarrierContract> currentContracts, Network network, Id depotLocation) {
			super();
			this.currentContracts = currentContracts;
			this.network = network;
			this.depotLocation = depotLocation;
		}
		
		public double calculateShareOfDistance(Shipment requestedShipment, double totalDistance){
			double sumOfWeights = 0.0;
			double weightOfRequestedShipment = getBeeLineDistance(depotLocation, requestedShipment) * size(requestedShipment); 
			sumOfWeights += weightOfRequestedShipment;
			for(CarrierContract c : currentContracts){
				sumOfWeights += getBeeLineDistance(depotLocation, c.getShipment()) * size(c.getShipment());
			}
			double avgMC = weightOfRequestedShipment/sumOfWeights * totalDistance;
//			assertNotHigherThan(200000,avgMC,requestedShipment,totalCosts);
			return avgMC;
		}
		
		public double calculateShareOfTime(Shipment requestedShipment, double totalTime){
			double sumOfWeights = 0.0;
			double weightOfRequestedShipment = getBeeLineTime(depotLocation, requestedShipment) * size(requestedShipment); 
			sumOfWeights += weightOfRequestedShipment;
			for(CarrierContract c : currentContracts){
				sumOfWeights += getBeeLineTime(depotLocation, c.getShipment()) * size(c.getShipment());
			}
			double avgMC = weightOfRequestedShipment/sumOfWeights * totalTime;
//			assertNotHigherThan(200000,avgMC,requestedShipment,totalCosts);
			return avgMC;
		}
		
		private double getBeeLineTime(Id depotLocation,Shipment requestedShipment) {
			double speed = 25;
			double beelineDistance = getBeeLineDistance(depotLocation, requestedShipment);
			return beelineDistance/speed;
		}

		private void assertNotHigherThan(int i, double avgMC, Shipment shipment,double totCosts) {
			if(avgMC > i){
				throw new IllegalStateException("strange.");
			}
			
		}

		private double size(Shipment shipment) {
			return Math.log(shipment.getSize());
		}

		private double getBeeLineDistance(Id depotLocation, Shipment shipment) {
			Coord locCoord = findCoord(depotLocation);
			Coord from = findCoord(shipment.getFrom());
			Coord to = findCoord(shipment.getTo());
			double distance = CoordUtils.calcDistance(locCoord, from) +
					CoordUtils.calcDistance(from, to) + CoordUtils.calcDistance(to, locCoord);
			return distance;
		}
		
		private Coord findCoord(Id location) {
			return network.getLinks().get(location).getCoord();
		}
	}
	
	public static class ServiceProviderImpl implements ServiceProvider {

		private BestTourBuilder tourBuilder = new BestTourBuilder();
		
		private TourActivityStatusUpdaterImpl updater;
		
		private Tour tour;
		
		public ServiceProviderImpl(Tour tour, int vehicleCapacity) {
			super();
			this.tour = tour;
			Costs costs = new CrowFlyCosts();
			updater = new TourActivityStatusUpdaterImpl(costs);
			tourBuilder = new BestTourBuilder();
			tourBuilder.setConstraints(new TWAndCapacityConstraint());
			tourBuilder.setCosts(costs);
			tourBuilder.setTourActivityStatusUpdater(updater);
		}
		
		public double getCostsOfCurrentTour(){
			updater.update(tour);
			return tour.costs.generalizedCosts;
		}

		@Override
		public org.matsim.contrib.freight.vrp.api.Offer requestService(org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment shipment, double bestKnownPrice) {
			Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, bestKnownPrice);
			if(newTour != null){
				double marginalCosts = newTour.costs.generalizedCosts - tour.costs.generalizedCosts;
				org.matsim.contrib.freight.vrp.api.Offer offer = new org.matsim.contrib.freight.vrp.api.Offer(this, marginalCosts);
				return offer;
			}
			else{
				return null;
			}
		}

		@Override
		public void offerGranted(
				org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment shipment) {
			throw new UnsupportedOperationException();
			
		}

		@Override
		public void offerRejected(
				org.matsim.contrib.freight.vrp.api.Offer offer) {
			throw new UnsupportedOperationException();
			
		}
		
	}
	
	private static Logger logger = Logger.getLogger(AverageMarginalCostOM.class);
	
	private Carrier carrier;
	
	private CarrierVehicle carrierVehicle;

	private Locations locations;
	
	private Collection<ServiceProviderImpl> serviceProviders;
	
	private Collection<ServiceProviderImpl> morningService;
	
	private Collection<ServiceProviderImpl> afternoonService;
	
	private MatSim2VRPTransformation vrpTransformation;
	
	private VRP vrp = null;
	
	private Map<ServiceProvider,Tour> tours = new HashMap<ServiceProvider, Tour>();
	
	private Network network;

	public void setNetwork(Network network) {
		this.network = network;
	}

	public AverageMarginalCostOM(Carrier carrier, Locations locations) {
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
//			vrpTransformation.addEnRoutePickupAndDeliveryShipment(s);
		}
		for(ScheduledTour t : carrier.getSelectedPlan().getScheduledTours()){
			org.matsim.contrib.freight.vrp.basics.Tour tour = makeTour(t.getTour(),vrpTransformation);
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
//		vrpTransformation.addEnRoutePickupAndDeliveryShipment(requestedShipment);
		CarrierOffer bestOffer = null;
		ServiceProviderImpl bestServiceProvider = null;
		org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment shipment = VrpUtils.createShipment(vrpTransformation.getFromCustomer(requestedShipment), vrpTransformation.getToCustomer(requestedShipment));
		double totalCosts = 0.0;
		if(!serviceProviders.isEmpty()){
			org.matsim.contrib.freight.vrp.api.Offer cheapestOffer = null;
			Tour bestTour = null;
			if(isMorning(startPickup)){
				for(ServiceProviderImpl sP : morningService){
					org.matsim.contrib.freight.vrp.api.Offer o = sP.requestService(shipment,Double.MAX_VALUE);
					totalCosts += sP.getCostsOfCurrentTour();
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
					totalCosts += sP.getCostsOfCurrentTour();
					org.matsim.contrib.freight.vrp.api.Offer o = sP.requestService(shipment,Double.MAX_VALUE);
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
		org.matsim.contrib.freight.vrp.api.Offer o = new org.matsim.contrib.freight.vrp.api.Offer(sP,sP.getCostsOfCurrentTour());
		CarrierOffer offer = new CarrierOffer();
		offer.setId(carrier.getId());
		offer.setPrice(sP.getCostsOfCurrentTour());
		if(bestOffer != null && bestOffer.getPrice() < offer.getPrice()){
			logger.info(carrier.getId() + " inserts " + requestedShipment + " into " + tours.get(bestServiceProvider) + "; costs=" + bestOffer.getPrice());
			totalCosts += bestOffer.getPrice();
			bestOffer.setPrice(new AvgMarginalCostCalculator(carrier.getContracts(), network, carrierVehicle.getLocation()).calculateShareOfDistance(requestedShipment, totalCosts));
			assertPriceNotHigherThan(200000,bestOffer.getPrice());
			return bestOffer;
		}
		else{
			logger.info(carrier.getId() + " inserts " + requestedShipment + " into " + roundTour + "; costs=" + offer.getPrice());
			assertPriceNotHigherThan(200000,offer.getPrice());
			return offer;
		}
	}
	

	private void assertPriceNotHigherThan(int i, Double price) {
		if(price > i){
			throw new IllegalStateException("price too hight");
		}
		
	}

	private boolean isMorning(Double startPickup) {
		if(startPickup == 0.0){
			return true;
		}
		return false;
	}

	private Tour makeTour(org.matsim.contrib.freight.carrier.Tour tour, MatSim2VRPTransformation vrpTrafo) {
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
		vrpTransformation = new MatSim2VRPTransformation(this.locations);
//		vrpTransformation.addAndCreateCustomer("depot", carrierVehicle.getLocation(), 0, 0.0, 24*3600, 0.0);
		createServiceProvidersFromExistingPlan();
	}

	@Override
	public void reset() {
//		vrpTransformation.clear();
		serviceProviders.clear();
	}

}
