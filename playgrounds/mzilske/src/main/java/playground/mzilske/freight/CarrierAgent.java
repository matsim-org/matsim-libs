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
	
	private CarrierImpl carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private PlanAlgorithm router;
	
	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	private CostAllocator costAllocator = null;

	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();
	
	private static Logger logger = Logger.getLogger(CarrierAgent.class);
	
	class CarrierDriverAgent {
		
		private Id driverId;
		
		double distance = 0.0;

		CarrierDriverAgent(Id driverId) {
			this.driverId = driverId;
		}

		public double getScore() {
			return -distance;
		}

		public void activityEnds(String activityType) {
			logger.info("Driver " + driverId + " has had a " + activityType);
		}

		public void tellDistance(double distance) {
			this.distance += distance;
			logger.info(driverId + " sOfDistance=" + this.distance/1000 + "km");
		}
		
		public double getCost(){
			return distance;
		}
		
	}

	public CarrierAgent(CarrierImpl carrier, PlanAlgorithm router) {
		this.carrier = carrier;
		this.router = router;
	}

	public List<Plan> createFreightDriverPlans() {
		clear();
		List<Plan> plans = new ArrayList<Plan>();
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Plan plan = new PlanImpl();
			Activity startActivity = new ActivityImpl("start", scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			Leg startLeg = new LegImpl(TransportMode.car);
			plan.addLeg(startLeg);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				Activity tourElementActivity = new ActivityImpl(tourElement.getActivityType(), tourElement.getLocation());
				((ActivityImpl) tourElementActivity).setDuration(tourElement.getDuration());
				plan.addActivity(tourElementActivity);
				Leg leg = new LegImpl(TransportMode.car);
				plan.addLeg(leg);
			}
			Activity endActivity = new ActivityImpl("end", scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			Id driverId = createDriverId();
			Person driverPerson = createDriverPerson(driverId);
			plan.setPerson(driverPerson);
			route(plan);
			plans.add(plan);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId);
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

	public void activityEnds(Id personId, String activityType) {
		carrierDriverAgents.get(personId).activityEnds(activityType);
	}
	
	public void tellDistance(Id personId, double distance) {
		carrierDriverAgents.get(personId).tellDistance(distance);
	}

	public double score() {
		int score = 0;
		for (Id driverId : getDriverIds()) {
			score += carrierDriverAgents.get(driverId).getScore();
		}
		score += carrierScore();
		return score;
	}

	public void calculateCostsOfSelectedPlan() {
		
	}

	public void scoreSelectedPlan() {
		double score = score();
		carrier.getSelectedPlan().setScore(score);
	}

	public List<Tuple<Shipment, Double>> calculateCostsOfSelectedPlanPerShipment() {
		List<Tuple<Shipment,Double>> listOfCostPerShipment = new ArrayList<Tuple<Shipment,Double>>();
		for(Id driverId : driverIds){
			ScheduledTour tour = driverTourMap.get(driverId);
			List<Tuple<Shipment,Double>> listOfCostPerShipmentPerDriver = costAllocator.allocateCost(tour.getTour().getShipments(),carrierDriverAgents.get(driverId).getCost());
			listOfCostPerShipment.addAll(listOfCostPerShipmentPerDriver);
		}
		return listOfCostPerShipment;
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

	private int carrierScore() {
		return 0;
	}

}
