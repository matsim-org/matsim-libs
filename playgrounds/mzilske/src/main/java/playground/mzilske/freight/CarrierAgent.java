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

import playground.mzilske.freight.CarrierTotalCostListener.CarrierCostEvent;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;

public class CarrierAgent {
	
	private static Logger logger = Logger.getLogger(CarrierAgent.class);
	
	private CarrierImpl carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private PlanAlgorithm router;
	
	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	private CarrierAgentTracker tracker;

	private CarrierCostFunction costCalculator = new CarrierCostFunction(){

		@Override
		public void init(CarrierImpl carrier) {
			
			
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
	
	private CarrierCostCalculator costPerShipmentCalculator = new CarrierCostCalculator(){

		@Override
		public void run(CarrierVehicle carrierVehicle,Collection<Contract> contracts, CostMemory costMemory,
				Double totalCosts) {
			
		}
		
	};
	
	private CarrierAgentTracker carrierAgentTracker;
	
	private TollCalculator tollCalculator;
	
	private Collection<CarrierCostMemoryListener> costMemoryListeners = new ArrayList<CarrierCostMemoryListener>();
	
	public Collection<CarrierCostMemoryListener> getCostMemoryListeners() {
		return costMemoryListeners;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	private Id id;
	
	public void setOfferMaker(OfferMaker offerMaker) {
		this.offerMaker = offerMaker;
	}

	class CarrierDriverAgent {
		
		private int activityCounter = 0;
		
		private Id driverId;
		
		double distance = 0.0;
		
		double time = 0.0;
		
		double startTime = 0.0;
		
		private int currentLoad = 0;
		
		private double weightedLoad = 0.0;
		
		private double totalLoad = 0.0;
		
		private double distanceRecordOfLastActivity = 0.0;

		private CarrierVehicle carrierVehicle;

		private double additionalCosts = 0.0;

		CarrierDriverAgent(Id driverId, CarrierVehicle carrierVehicle) {
			this.setCarrierVehicle(carrierVehicle);
			this.driverId = driverId;
		}

		public void activityEndOccurs(String activityType, double time) {
			Tour tour = driverTourMap.get(driverId).getTour();
			if (FreightConstants.START.equals(activityType)){
				startTime = time;
			}
			if (FreightConstants.PICKUP.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				calculateLoadFactorComponent(tourElement);
				totalLoad += tourElement.getShipment().getSize();
				tracker.notifyPickup(tourElement.getShipment(), time);
				activityCounter++;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				calculateLoadFactorComponent(tourElement);
				tracker.notifyDelivery(tourElement.getShipment(), time);
				activityCounter++;
			}
		}
		
		public double getCapacityUsage(){
			return weightedLoad / (distance*carrierVehicle.getCapacity());
		}

		private void calculateLoadFactorComponent(TourElement tourElement) {
			weightedLoad += currentLoad*(distance-distanceRecordOfLastActivity);
			if(tourElement instanceof Pickup){
				currentLoad += tourElement.getShipment().getSize();
			}
			else{
				currentLoad -= tourElement.getShipment().getSize();
			}
			distanceRecordOfLastActivity = distance;
		}

		public void activityStartOccurs(String activityType, double time) {
			if(FreightConstants.END.equals(activityType)){
				time += time - startTime;
			}
			
		}

		public void tellDistance(double distance) {
			this.distance += distance;
		}
		
		public double getDistance(){
			return distance;
		}

		private void setCarrierVehicle(CarrierVehicle carrierVehicle) {
			this.carrierVehicle = carrierVehicle;
		}

		private CarrierVehicle getCarrierVehicle() {
			return carrierVehicle;
		}

		public void tellTraveltime(double time) {
			this.time += time;
		}

		double getTime() {
			return time;
		}

		public void tellToll(double toll) {
			this.additionalCosts  += toll;
		}
		
		public double getAdditionalCosts(){
			return this.additionalCosts;
		}
		
		
		
	}

	public CarrierAgent(CarrierAgentTracker carrierAgentTracker, CarrierImpl carrier, PlanAlgorithm router) {
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.router = router;
		this.id = carrier.getId();
	}
	
	Id getId(){
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
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId, scheduledTour.getVehicle());
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
			cost += costCalculator.calculateCost(driver.getCarrierVehicle(), driver.getDistance(), driver.getTime());
		}
		return cost;
	}

	public void memorizeCost(Id from, Id to, int size, Double cost) {
		costMemory.memorizeCost(from, to, size, cost);
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
			performance += carrierDriverAgent.weightedLoad;
			volume += carrierDriverAgent.totalLoad;
			DriverEvent driverEvent = new DriverEvent(driverId,carrier.getId(),carrierDriverAgent.getCarrierVehicle().getVehicleId());
			driverEvent.capacityUsage = carrierDriverAgent.getCapacityUsage();
			driverEvent.distance = carrierDriverAgent.getDistance();
			driverEvent.time = carrierDriverAgent.getTime();
			driverEvent.performance = carrierDriverAgent.weightedLoad;
			driverEvent.volumes = carrierDriverAgent.totalLoad;
			carrierAgentTracker.processEvents(driverEvent);
		}
		double avgCapacityUsage = weightedCapacityUsage/distance;
		costPerShipmentCalculator.run(vehicle, carrier.getContracts(), costMemory, cost);
		CarrierCostEvent costEvent = new CarrierCostEvent(distance, time, cost);
		costEvent.setCapacityUsage(avgCapacityUsage);
		costEvent.setPerformance(performance);
		costEvent.setVolume((int)volume);
		carrierAgentTracker.informTotalCost(id,costEvent);
		informCostMemoryListeners();
	}

	private void informCostMemoryListeners() {
		for(CarrierCostMemoryListener l : costMemoryListeners){
			l.inform(id, costMemory);
		}
		
	}

	public void setCostCalculator(CarrierCostCalculator costCalculator) {
		this.costPerShipmentCalculator = costCalculator;
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
}
