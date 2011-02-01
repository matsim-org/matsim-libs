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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.Tour.TourElement;

public class CarrierAgent {
	
	public class CostTableKey {

		private Id from;
		private Id to;
		private int size;

		public CostTableKey(Id from, Id to, int size) {
			this.from = from;
			this.to = to;
			this.size = size;
		}

		@Override
		public boolean equals(Object obj) {
			CostTableKey other = (CostTableKey) obj;
			return from.equals(other.from) && to.equals(other.to) && size == other.size;
		}

		@Override
		public int hashCode() {
			return from.hashCode() + to.hashCode() + size;
		}

	}

	private static final double EPSILON = 0.0001;

	private CarrierImpl carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private PlanAlgorithm router;
	
	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	private CostAllocator costAllocator = null;

	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	private CarrierAgentTracker tracker;

	private Map<CostTableKey, Double> costTable = new HashMap<CostTableKey, Double>();
	
	private static Logger logger = Logger.getLogger(CarrierAgent.class);

	private CarrierCostFunction costFunction;
	
	class CarrierDriverAgent {
		
		private int activityCounter = 0;
		
		private Id driverId;
		
		double distance = 0.0;

		private CarrierVehicle carrierVehicle;

		CarrierDriverAgent(Id driverId, CarrierVehicle carrierVehicle) {
			this.setCarrierVehicle(carrierVehicle);
			this.driverId = driverId;
		}

		public void activityOccurs(String activityType, double time) {
			Tour tour = driverTourMap.get(driverId).getTour();
			if (FreightConstants.PICKUP.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				tracker.notifyPickup(tourElement.getShipment(), time);
				activityCounter++;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				TourElement tourElement = tour.getTourElements().get(activityCounter);
				tracker.notifyDelivery(tourElement.getShipment(), time);
				activityCounter++;
			}
		}

		public void tellDistance(double distance) {
			this.distance += distance;
			logger.info(driverId + " sOfDistance=" + this.distance/1000 + "km");
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
		
	}

	public CarrierAgent(CarrierAgentTracker carrierAgentTracker, CarrierImpl carrier, PlanAlgorithm router) {
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.router = router;
	}

	public List<Plan> createFreightDriverPlans() {
		clear();
		List<Plan> plans = new ArrayList<Plan>();
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Plan plan = new PlanImpl();
			Activity startActivity = new ActivityImpl(FreightConstants.START, scheduledTour.getVehicle().getLocation());
			logger.info("startActivity: " + startActivity);
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			Leg startLeg = new LegImpl(TransportMode.car);
			plan.addLeg(startLeg);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				Activity tourElementActivity = new ActivityImpl(tourElement.getActivityType(), tourElement.getLocation());
				logger.info("tourActivity: " + tourElementActivity);
				((ActivityImpl) tourElementActivity).setEndTime(tourElement.getTimeWindow().getStart());
				plan.addActivity(tourElementActivity);
				Leg leg = new LegImpl(TransportMode.car);
				plan.addLeg(leg);
			}
			Activity endActivity = new ActivityImpl(FreightConstants.END, scheduledTour.getVehicle().getLocation());
			logger.info("EndActivity: " + endActivity);
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

	public void setCostAllocator(CostAllocator costAllocator) {
		this.costAllocator = costAllocator;
	}

	public Collection<Id> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	public void activityOccurs(Id personId, String activityType, double time) {
		carrierDriverAgents.get(personId).activityOccurs(activityType, time);
	}
	
	public void tellDistance(Id personId, double distance) {
		carrierDriverAgents.get(personId).tellDistance(distance);
	}

	public void scoreSelectedPlan() {
		double cost = calculateCost();
		carrier.getSelectedPlan().setScore(cost * (-1));
	}
	
	private double calculateCost() {
		costFunction.init(carrier);
		double cost = 0.0;
		for (Id driverId : getDriverIds()) {
			CarrierDriverAgent driver = carrierDriverAgents.get(driverId);
			cost += costFunction.calculateCost(driver.getCarrierVehicle(), driver.getDistance());
		}
		return cost;
	}

	public List<Tuple<Shipment, Double>> calculateCostsPerShipment() {
		List<Tuple<Shipment,Double>> listOfCostPerShipment = new ArrayList<Tuple<Shipment,Double>>();
		for(Id driverId : driverIds){
			ScheduledTour tour = driverTourMap.get(driverId);
			logger.debug("allocating cost of driver "+driverId+", carrier "+carrier.getId());
			costFunction.init(carrier);
			CarrierDriverAgent carrierDriverAgent = carrierDriverAgents.get(driverId);
			double cost = costFunction.calculateCost(carrierDriverAgent.getCarrierVehicle(), carrierDriverAgent.getDistance());
			List<Tuple<Shipment,Double>> listOfCostPerShipmentPerDriver = costAllocator.allocateCost(tour.getTour().getShipments(), cost);
			listOfCostPerShipment.addAll(listOfCostPerShipmentPerDriver);
		}
		if (! listOfCostPerShipment.isEmpty()) {
			assertSum(listOfCostPerShipment, calculateCost());
		}
		for (Tuple<Shipment,Double> t : listOfCostPerShipment) {
			memorizeCost(t.getFirst().getFrom(), t.getFirst().getTo(), t.getFirst().getSize(), t.getSecond());
		}
		return listOfCostPerShipment;
	}

	private void memorizeCost(Id from, Id to, int size, Double cost) {
		CostTableKey key = new CostTableKey(from, to, size);
		if (!costTable.containsKey(key) || costTable.get(key) > cost) {
			costTable.put(key, cost);
		}
	}

	private void assertSum(List<Tuple<Shipment, Double>> listOfCostPerShipment, double calculateCostOfSelectedPlan) {
		double sum = 0.0;
		for (Tuple<Shipment, Double> t : listOfCostPerShipment) {
			sum += t.getSecond();
		}
		if ( Math.abs(calculateCostOfSelectedPlan - sum) > EPSILON) {
			throw new RuntimeException ("For the moment, we want the total cost to be the sum of the costs per shipment.");
		}
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

	public Offer makeOffer(Id linkId, Id linkId2, int shipmentSize) {
		Offer offer = new Offer();
		offer.setCarrierId(carrier.getId());
		Double memorizedPrice = costTable.get(new CostTableKey(linkId, linkId2, shipmentSize));
		double price;
		if (memorizedPrice != null) {
			price = memorizedPrice;
			if (Math.random() < 0.05) {
				double tenPercent = price * 0.1;
				double verrauscht = Math.random() * tenPercent;
				price = price + verrauscht * ( Math.random() < 0.5 ? -1 : 1);
			}
			System.out.println("Ich bin " + carrier.getId()+". Biete an: " + linkId + " nach " +linkId2 + " für " + price);
		} else {
			price = 10000.0 * Math.random();
			System.out.println("Biete einen zufälligen Preis an, und zwar " + price);
		}
		offer.setPrice(price);
		offer.setDuration(120.0);
		return offer;
	}

	void setCostFunction(CarrierCostFunction costFunction) {
		this.costFunction = costFunction;
	}

}
