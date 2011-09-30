package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.CarrierTotalCostHandler.CarrierCostEvent;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.events.CostMemoryStatusEvent;
import playground.mzilske.freight.events.DriverPerformanceEvent;

public class DubioserCarrierAgentImpl extends BasicCarrierAgentImpl implements CarrierAgent{
	
	private static Logger logger = Logger.getLogger(DubioserCarrierAgentImpl.class);
	
	private Carrier carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private PlanAlgorithm router;
	
	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	private CarrierAgentTracker tracker;
	
	private CarrierDriverAgentFactory driverAgentFactory;

	private CarrierCostFunction costCalculator = new CarrierCostFunction(){

		@Override
		public void init(Carrier carrier) {
			
			
		}

		@Override
		public double calculateCost(CarrierVehicle carrierVehicle,double distance) {
			return distance;
		}

		@Override
		public double calculateCost(CarrierVehicle carrierVehicle,double distance, double time) {
			return distance;
		}
		
	};

	private Network network;

	private OfferMaker offerMaker;
	
	private CostMemory costMemory = new CostMemory(){

		@Override
		public void memorizeCost(Id from, Id to, int size, double cost) {
			
		}

		@Override
		public Double getCost(Id from, Id to, int size) {
			return null;
		}
		
	};
	
//	private CarrierCostCalculator costPerShipmentCalculator = new CarrierCostCalculator(){
//
//		@Override
//		public void run(CarrierVehicle carrierVehicle,Collection<CarrierContract> contracts, CostMemory costMemory,
//				Double totalCosts) {
//			
//		}
//		
//	};
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TollCalculator tollCalculator;

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	private Id id;
	
	public void setOfferMaker(OfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}

	public DubioserCarrierAgentImpl(CarrierAgentTracker carrierAgentTracker, Carrier carrier, PlanAlgorithm router, CarrierDriverAgentFactory driverAgentFactory) {
		super(carrierAgentTracker,carrier,router,driverAgentFactory);
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.router = router;
		this.id = carrier.getId();
		this.driverAgentFactory = driverAgentFactory;
	}
	
	public Id getId(){
		return id;
	}

	public List<Plan> createFreightDriverPlans() {
		clear();
		List<Plan> plans = new ArrayList<Plan>();
		if(carrier.getSelectedPlan() == null){
			return plans;
		}
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Plan plan = new PlanImpl();
			Activity startActivity = new ActivityImpl(FreightConstants.START, scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			Leg startLeg = new LegImpl(TransportMode.car);
			plan.addLeg(startLeg);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				Activity tourElementActivity = new ActivityImpl(tourElement.getActivityType(), tourElement.getLocation());
//				((ActivityImpl) tourElementActivity).setMaximumDuration(tourElement.getDuration());
//				((ActivityImpl) tourElementActivity).setEndTime(3600*24);
				((ActivityImpl) tourElementActivity).setEndTime(tourElement.getTimeWindow().getStart());
				plan.addActivity(tourElementActivity);
				Leg leg = new LegImpl(TransportMode.car);
				plan.addLeg(leg);
			}
			Activity endActivity = new ActivityImpl(FreightConstants.END, scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			Id driverId = createDriverId();
			Person driverPerson = createDriverPerson(driverId);
			plan.setPerson(driverPerson);
			route(plan);
			plans.add(plan);
			CarrierDriverAgent carrierDriverAgent = driverAgentFactory.createDriverAgent(this,driverId,scheduledTour);
			carrierDriverAgents.put(driverId, carrierDriverAgent);
			driverTourMap.put(driverId, scheduledTour);
		}
		return plans;
	}
	
	private void clear() {
		carrierDriverAgents.clear();
		driverTourMap.clear();
		driverIds.clear();
		nextId=0;	
	}

	public Collection<Id> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	public void activityStartOccurs(Id personId, String activityType, double time) {
		carrierDriverAgents.get(personId).activityStartOccurs(activityType, time);
	}

	public void activityEndOccurs(Id personId, String activityType, double time) {
		carrierDriverAgents.get(personId).activityEndOccurs(activityType, time);
	}
	
	public void tellDistance(Id personId, double distance) {
		carrierDriverAgents.get(personId).tellDistance(distance);
	}
	
	public void tellTraveltime(Id personId, double time){
		carrierDriverAgents.get(personId).tellTraveltime(time);
	}

	public void scoreSelectedPlan() {
		double cost = calculateCost();
		carrier.getSelectedPlan().setScore(cost * (-1));
	}
	
	private double calculateCost() {
		costCalculator.init(carrier);
		double cost = 0.0;
		for (Id driverId : getDriverIds()) {
			CarrierDriverAgent driver = carrierDriverAgents.get(driverId);
			cost += costCalculator.calculateCost(driver.getVehicle(), driver.getDistance(), driver.getTime());
		}
		return cost;
	}

	private Person createDriverPerson(Id driverId) {
		Person person = new PersonImpl(driverId);
		return person;
	}

	private void route(Plan plan) {
		router.run(plan);
	}

	private Id createDriverId() {
		IdImpl id = new IdImpl("fracht_"+carrier.getId()+"_"+nextId);
		driverIds.add(id);
		++nextId;
		return id;
	}

	public CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery) {
		if(requestIsNoGo(linkId,linkId2)){
			return new NoOffer();
		}
		Double memorizedPrice = costMemory.getCost(linkId, linkId2, shipmentSize);
		return offerMaker.requestOffer(linkId,linkId2,shipmentSize,startPickup,endPickup,startDelivery,endDelivery,memorizedPrice);
	}

	public void setCostMemory(CostMemory costMemory) {
		this.costMemory = costMemory;
	}

	private boolean requestIsNoGo(Id linkId, Id linkId2) {
		if(carrier.getKnowledge().getNoGoLocations().contains(linkId2) || carrier.getKnowledge().getNoGoLocations().contains(linkId)){
			return true;
		}
		return false;
	}

	public void setCostFunction(CarrierCostFunction costFunction) {
		this.costCalculator = costFunction;
	}

	public void setNetwork(Network network) {
		this.network = network;
		
	}

	public void reset() {
		offerMaker.reset();
		offerMaker.init();
	}

	public void calculateCosts() {
		double cost = 0.0;
		double distance = 0.0;
		double time = 0.0;
		double volume = 0.0;
		double performance = 0.0;
		double weightedCapacityUsage = 0.0;
		double toll = 0.0;
		costCalculator.init(carrier);
		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		for(Id driverId : driverIds){
			CarrierDriverAgent carrierDriverAgent = carrierDriverAgents.get(driverId);
			distance += carrierDriverAgent.getDistance();
			time += carrierDriverAgent.getTime();
			cost += costCalculator.calculateCost(vehicle, carrierDriverAgent.getDistance(), 0.0);
			toll += carrierDriverAgent.getAdditionalCosts();
			cost += carrierDriverAgent.getAdditionalCosts();
			weightedCapacityUsage += carrierDriverAgent.getCapacityUsage()*carrierDriverAgent.getDistance();
			performance += carrierDriverAgent.getPerformace();
			volume += carrierDriverAgent.getVolumes();
			DriverPerformanceEvent driverPerformanceEvent = new DriverPerformanceEvent(driverId,carrier.getId(),carrierDriverAgent.getVehicle().getVehicleId());
			driverPerformanceEvent.capacityUsage = carrierDriverAgent.getCapacityUsage();
			driverPerformanceEvent.distance = carrierDriverAgent.getDistance();
			driverPerformanceEvent.time = carrierDriverAgent.getTime();
			driverPerformanceEvent.performance = carrierDriverAgent.getPerformace();
			driverPerformanceEvent.volumes = carrierDriverAgent.getVolumes();
			carrierAgentTracker.processEvent(driverPerformanceEvent);
		}
		double avgCapacityUsage = weightedCapacityUsage/distance;
//		costPerShipmentCalculator.run(vehicle, carrier.getContracts(), costMemory, cost);
		CarrierCostEvent costEvent = new CarrierCostEvent(carrier.getId(),distance, time, cost);
		costEvent.setCapacityUsage(avgCapacityUsage);
		costEvent.setPerformance(performance);
		costEvent.setVolume((int)volume);
		carrierAgentTracker.processEvent(costEvent);
//		carrierAgentTracker.informTotalCost(id,costEvent);
		informCostMemoryListeners();
	}

	private void informCostMemoryListeners() {
		tracker.processEvent(new CostMemoryStatusEvent(id, costMemory));
	}

	public void setCostCalculator(CarrierCostCalculator costCalculator) {
//		this.costPerShipmentCalculator = costCalculator;
	}

	public void tellLink(Id personId, Id linkId) {
		if(tollCalculator == null){
			return;
		}
		double toll = tollCalculator.getToll(linkId);
		carrierDriverAgents.get(personId).tellToll(toll);
	}

	public void setTollCalculator(TollCalculator tollCalculator) {
		this.tollCalculator = tollCalculator;
	}

	@Override
	public void notifyPickup(Id driverId, Shipment shipment, double time) {
		tracker.notifyPickup(carrier.getId(), driverId, shipment, time);
	}

	@Override
	public void notifyDelivery(Id driverId, Shipment shipment, double time) {
		tracker.notifyDelivery(carrier.getId(), driverId, shipment, time);
	}

	@Override
	public void informOfferRejected(CarrierOffer offer) {
		logger.info("my offer was rejected :(");
		
	}

	@Override
	public void informOfferAccepted(CarrierContract contract) {
		logger.info("offer was accepted :))");
		
	}

	

	@Override
	public void informTSPContractAccepted(CarrierContract contract) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void informTSPContractCanceled(CarrierContract contract) {
		// TODO Auto-generated method stub
		
	}
}
