package org.matsim.contrib.freight.mobsim;

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
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Shipment;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.vrp.basics.CarrierCostFunction;
import org.matsim.contrib.freight.vrp.basics.CarrierCostParams;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

class CarrierAgentImpl {
	
	static class CarrierDriverAgentImpl {
		
		private int activityCounter = 1;
		
		private Id driverId;
		
		private double distance = 0.0;
		
		private double totalDutyTime = 0.0;
		
		private double travelTime = 0.0;
		
		private double startTime = 0.0;
		
		private double tooLate = 0.0;
		
		private double waitingTime = 0.0;
		
		private double serviceTime = 0.0;
		
		private double lastActivityEnd = 0.0;
		
//		private double lastServiceStart = 0.0;
		
		private double volumes = 0.0;
		
		private double additionalCosts = 0.0;
		
		private ScheduledTour scheduledTour;
		
		private CarrierAgentImpl carrierAgent;
		
		CarrierDriverAgentImpl(CarrierAgentImpl carrierAgent, Id driverId, ScheduledTour tour) {
			this.driverId = driverId;
			this.scheduledTour = tour;
			this.carrierAgent = carrierAgent;
		}

		public void activityEndOccurs(String activityType, double time) {
			Tour tour = this.scheduledTour.getTour();
			lastActivityEnd = time;
			if (FreightConstants.START.equals(activityType)){
				startTime = time;
//				logger.info("driver="+driverId+" start ends, time=" + time);
			}
			if (FreightConstants.PICKUP.equals(activityType)) {
//				serviceTime += time-lastServiceStart;
//				logger.info("driver="+driverId+" picksUp ends, time=" + time);
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
//				serviceTime += time-lastServiceStart;
//				logger.info("driver="+driverId+" delivery ends, time=" + time);
			}
		}

		public void activityStartOccurs(String activityType, double time) {
			Tour tour = this.scheduledTour.getTour();
			travelTime += time - lastActivityEnd;
			if(FreightConstants.END.equals(activityType)){
				totalDutyTime = time - startTime;
//				logger.info("driver="+driverId+" end starts, time=" + time);
			}
			if (FreightConstants.PICKUP.equals(activityType)) {
//				lastServiceStart = time;
				Pickup tourElement = (Pickup) tour.getTourElements().get(activityCounter);
//				logger.info("driver="+driverId+" pickUp starts, time=" + time + ", tw=" + tourElement.getTimeWindow());
				if(time > tourElement.getTimeWindow().getEnd()){
					tooLate += time - tourElement.getTimeWindow().getEnd();
				}
				if(time < tourElement.getTimeWindow().getStart()){
					waitingTime += tourElement.getTimeWindow().getStart() - time;
//					lastServiceStart = tourElement.getTimeWindow().getStart();
				}
				carrierAgent.notifyPickup(driverId, tourElement.getShipment(), time);
//				lastActivity = tourElement;
				activityCounter+=2;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
//				lastServiceStart = time;
				Delivery tourElement = (Delivery) tour.getTourElements().get(activityCounter);
//				logger.info("driver="+driverId+" delivery starts, time=" + time + ", tw=" + tourElement.getTimeWindow());
				if(time > tourElement.getTimeWindow().getEnd()){
					tooLate += time - tourElement.getTimeWindow().getEnd();
				}
				if(time < tourElement.getTimeWindow().getStart()){
					waitingTime += tourElement.getTimeWindow().getStart() - time;
//					lastServiceStart = tourElement.getTimeWindow().getStart();
				}
				carrierAgent.notifyDelivery(driverId, tourElement.getShipment(), time);
//				lastActivity = tourElement;
				activityCounter+=2;
			}
		}

		void tellDistance(double distance) {
			this.distance += distance;
		}
		
		double getDistance(){
			return distance;
		}

//		public void tellTraveltime(double time) {
//			this.travelTime += time;
//		}

		double getTooLate() {
			return tooLate;
		}

		void tellToll(double toll) {
			this.additionalCosts  += toll;
		}

		double getAdditionalCosts(){
			return this.additionalCosts;
		}

		double getVolumes() {
			return volumes;
		}

		CarrierVehicle getVehicle() {
			return scheduledTour.getVehicle();
		}
		
		double getTransportTime(){
			return travelTime;
		}
		
		double getWaitingTime(){
			return waitingTime;
		}
		
		double getServiceTime(){
			return serviceTime;
		}

		double getDutyTime() {
			return totalDutyTime;
		}
	}

	private static Logger logger = Logger.getLogger(CarrierAgentImpl.class);
	
	private Carrier carrier;
	
	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;
	
	private Map<Id, CarrierDriverAgentImpl> carrierDriverAgents = new HashMap<Id, CarrierDriverAgentImpl>();
	
	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	private CarrierAgentTracker tracker;
	
	private Id id;
	
	CarrierAgentImpl(CarrierAgentTracker carrierAgentTracker, Carrier carrier) {
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.id = carrier.getId();
	
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
			LegImpl lastLeg = null;
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				if(tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg){
					org.matsim.contrib.freight.carrier.Tour.Leg tourLeg = (org.matsim.contrib.freight.carrier.Tour.Leg) tourElement;
					Route route = tourLeg.getRoute();
					assertRouteIsNotNull(route);
					LegImpl leg = new LegImpl(TransportMode.car);
					leg.setRoute(route);
					leg.setDepartureTime(tourLeg.getDepartureTime());
					leg.setTravelTime(tourLeg.getExpectedTransportTime());
					leg.setArrivalTime(tourLeg.getDepartureTime() + tourLeg.getExpectedTransportTime());
					plan.addLeg(leg);
					lastLeg = leg;
				}
				else if(tourElement instanceof TourActivity){
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = new ActivityImpl(act.getActivityType(), act.getLocation());
					double endTime = Math.max(lastLeg.getArrivalTime() + act.getDuration(), act.getTimeWindow().getStart() + act.getDuration());
					tourElementActivity.setEndTime(endTime);
					plan.addActivity(tourElementActivity);
				}
			}
			Activity endActivity = new ActivityImpl(FreightConstants.END, scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			Id driverId = createDriverId();
			Person driverPerson = createDriverPerson(driverId);
            driverPerson.addPlan(plan);
			plan.setPerson(driverPerson);
			plans.add(plan);
			CarrierDriverAgentImpl carrierDriverAgent = new CarrierDriverAgentImpl(this, driverId, scheduledTour); 
			carrierDriverAgents.put(driverId, carrierDriverAgent);
			driverTourMap.put(driverId, scheduledTour);
		}
		return plans;
	}
	
	private void assertRouteIsNotNull(Route route) {
		if(route == null){
			throw new IllegalStateException("missing route for carrier " + this.getId());
		}
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
	
//	public void tellTraveltime(Id personId, double time){
//		carrierDriverAgents.get(personId).tellTraveltime(time);
//	}

	private Person createDriverPerson(Id driverId) {
		Person person = new PersonImpl(driverId);
		return person;
	}

	private Id createDriverId() {
		IdImpl id = new IdImpl("fracht_"+carrier.getId()+"_"+nextId);
		driverIds.add(id);
		++nextId;
		return id;
	}

	public void notifyPickup(Id driverId, Shipment shipment, double time) {
		tracker.notifyPickup(carrier.getId(), driverId, shipment, time);
	}

	public void notifyDelivery(Id driverId, Shipment shipment, double time) {
		tracker.notifyDelivery(carrier.getId(), driverId, shipment, time);
	}

	public void calculateCosts() {
		// TODO Auto-generated method stub
		
	}

	public void tellLink(Id personId, Id linkId) {
		// TODO Auto-generated method stub
		
	}

	public void scoreSelectedPlan() {
		if(carrier.getSelectedPlan() == null){
			return;
		}
		double score = 0.0;
		double tooLate = 0.0;
		double waiting = 0.0;
		double transport = 0.0;
		double duty = 0.0;
		double dist = 0.0;
		int vehicles = 0;
		for(CarrierDriverAgentImpl driver : carrierDriverAgents.values()){
			vehicles++;
			tooLate += driver.tooLate;
			waiting += driver.waitingTime;
			transport += driver.travelTime;
			duty += driver.totalDutyTime;
			dist += driver.distance;
//			score += (-1)*new CarrierCostFunction(new MyCarrierCostParams()).getCosts(driver.getTransportTime(), 
//					driver.getDistance(), driver.getWaitingTime(), driver.getServiceTime(), driver.getTooLate());
//			score += (-1)*new CarrierCostParams().getCostPerVehicle();
		}
		carrier.getSelectedPlan().setScore(score);
	}

}
