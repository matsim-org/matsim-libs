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
		
		private double time = 0.0;
		
		private double startTime = 0.0;
		
		private int currentLoad = 0;
		
		private double performance = 0.0;
		
		private double volumes = 0.0;
		
		private double distanceRecordOfLastActivity = 0.0;

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
			if (FreightConstants.START.equals(activityType)){
				startTime = time;
			}
			if (FreightConstants.PICKUP.equals(activityType)) {
				Pickup tourElement = (Pickup) tour.getTourElements().get(activityCounter);
				calculateLoadFactorComponent(tourElement);
				volumes += tourElement.getShipment().getSize();
				carrierAgent.notifyPickup(driverId, tourElement.getShipment(), time);
				activityCounter+=2;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				Delivery tourElement = (Delivery) tour.getTourElements().get(activityCounter);
				calculateLoadFactorComponent(tourElement);
				carrierAgent.notifyDelivery(driverId, tourElement.getShipment(), time);
				activityCounter+=2;
			}
		}
		

		public double getCapacityUsage(){
			return performance / (distance*scheduledTour.getVehicle().getCapacity());
		}

		private void calculateLoadFactorComponent(TourElement tourElement) {
			if(!(tourElement instanceof ShipmentBasedActivity)){
				return;
			}
			ShipmentBasedActivity act = (ShipmentBasedActivity) tourElement;
			performance += currentLoad*(distance-distanceRecordOfLastActivity);
			if(tourElement instanceof Pickup){
				currentLoad += act.getShipment().getSize();
			}
			else{
				currentLoad -= act.getShipment().getSize();
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

		public void tellTraveltime(double time) {
			this.time += time;
		}

		public void tellToll(double toll) {
			this.additionalCosts  += toll;
		}

		public double getAdditionalCosts(){
			return this.additionalCosts;
		}

		public double getVolumes() {
			return volumes;
		}

		public double getPerformace() {
			return performance;
		}

		public CarrierVehicle getVehicle() {
			return scheduledTour.getVehicle();
		}

		public double getTime() {
			return time;
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
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				if(tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg){
					Route route = ((org.matsim.contrib.freight.carrier.Tour.Leg)tourElement).getRoute();
					assertRouteIsNotNull(route);
					Leg leg = new LegImpl(TransportMode.car);
					leg.setRoute(route);
					plan.addLeg(leg);
				}
				else if(tourElement instanceof TourActivity){
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = new ActivityImpl(act.getActivityType(), act.getLocation());
//					tourElementActivity.setEndTime(tourElement.getTimeWindow().getStart());
					tourElementActivity.setMaximumDuration(act.getDuration());
					tourElementActivity.setStartTime(act.getTimeWindow().getStart());
					tourElementActivity.setEndTime(act.getTimeWindow().getEnd() + act.getDuration());
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
	
	public void tellTraveltime(Id personId, double time){
		carrierDriverAgents.get(personId).tellTraveltime(time);
	}

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
		// TODO Auto-generated method stub
		
	}

}
