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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.FreightActivity;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;

class CarrierAgent implements ActivityStartEventHandler, ActivityEndEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler{

	static class CarrierDriverAgent {

		private static Logger logger = Logger.getLogger(CarrierDriverAgent.class);
		
		private LegImpl currentLeg;
		
		private Activity currentActivity;
		
		private List<Id> currentRoute;
		
		private final Id driverId;

		private final CarrierAgent carrierAgent;

		private final ScheduledTour scheduledTour;
		
		private ScoringFunction scoringFunction;
		
		private int activityCounter = 1;

		private Map<Integer, CarrierShipment> activity2shipment;

		CarrierDriverAgent(CarrierAgent carrierAgent, Id driverId, ScheduledTour tour, ScoringFunction scoringFunction) {
			this.driverId = driverId;
			this.scheduledTour = tour;
			this.carrierAgent = carrierAgent;
			this.scoringFunction = scoringFunction;
			activity2shipment = new HashMap<Integer, CarrierShipment>();
		}
		
		public void handleEvent(AgentArrivalEvent event) {
	        currentLeg.setArrivalTime(event.getTime());
	        double travelTime = currentLeg.getArrivalTime() - currentLeg.getDepartureTime();
	        currentLeg.setTravelTime(travelTime);
//	        assert currentRoute.size() >= 1;
	        if (currentRoute.size() > 1) {
	            NetworkRoute networkRoute = RouteUtils.createNetworkRoute(currentRoute, null);
	            networkRoute.setTravelTime(travelTime);
	            networkRoute.setVehicleId(getVehicle().getVehicleId());
	            currentLeg.setRoute(networkRoute);
	            currentRoute = null;
	        } else {
	        	Id startLink;
	        	if(currentRoute.size() != 0){
	        		startLink = currentRoute.get(0);
	        	}
	        	else{
	        		startLink = event.getLinkId();
	        	}
	            GenericRoute genericRoute = new GenericRouteImpl(startLink, event.getLinkId());    
	            genericRoute.setDistance(0.0);
	            currentLeg.setRoute(genericRoute);
	        }
//	        logger.info(driverId + " arrives, time " + Time.writeTime(event.getTime()));
	        scoringFunction.handleLeg(currentLeg);
	    }
		
		public void handleEvent(AgentDepartureEvent event) {
	        LegImpl leg = new LegImpl(event.getLegMode());
	        leg.setDepartureTime(event.getTime());
	        currentLeg = leg;
	        currentRoute = new ArrayList<Id>();
//	        logger.info(driverId + " departs, time " + Time.writeTime(event.getTime()));
		}

		public void handleEvent(LinkEnterEvent event) {
//			logger.info(driverId + " enters link " + event.getLinkId() + " at time " + Time.writeTime(event.getTime()));
			currentRoute.add(event.getLinkId());
		}
		
		public void handleEvent(LinkLeaveEvent event) {
//			logger.info(driverId + " left link " + event.getLinkId() + " at time " + Time.writeTime(event.getTime()));
		}

		public void handleEvent(ActivityEndEvent event) {
			if (currentActivity == null) {
				ActivityImpl firstActivity = new ActivityImpl(event.getActType(), event.getLinkId());
				firstActivity.setFacilityId(event.getFacilityId());
//				TourActivity tourActivity = getTourActivity();
//				FreightActivity firstFreightActivity = new FreightActivity(firstActivity, tourActivity.getTimeWindow());
				currentActivity = firstActivity;
			}
//			logger.info(driverId + " ends " + currentActivity.getType() + " time " + Time.writeTime(event.getTime()));
			currentActivity.setEndTime(event.getTime());
			scoringFunction.handleActivity(currentActivity);
			activityFinished(event.getActType(), event.getTime()); 
		}

		private TourActivity getTourActivity() {
			return (TourActivity) this.scheduledTour.getTour().getTourElements().get(activityCounter);
		}

		public void handleEvent(ActivityStartEvent event) {
			 ActivityImpl activity = new ActivityImpl(event.getActType(), event.getLinkId()); 
			 activity.setFacilityId(event.getFacilityId());
			 activity.setStartTime(event.getTime());
//			 logger.info(driverId + " starts " + activity.getType() + " time " + Time.writeTime(event.getTime()));
			 if(event.getActType().equals(FreightConstants.END)){
				 activity.setEndTime(Time.UNDEFINED_TIME);
				 scoringFunction.handleActivity(activity);
			 }
			 else{
				 FreightActivity freightActivity = new FreightActivity(activity, getTourActivity().getTimeWindow());
				 currentActivity = freightActivity; 
			 }
		 }

		private void activityFinished(String activityType, double time) {
			Tour tour = this.scheduledTour.getTour();
			if (FreightConstants.PICKUP.equals(activityType)) {
				Pickup tourElement = (Pickup) tour.getTourElements().get(activityCounter);
				carrierAgent.notifyPickup(driverId, tourElement.getShipment(),time);
//				logger.info("pickup occured");
				activityCounter += 2;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				Delivery tourElement = (Delivery) tour.getTourElements().get(activityCounter);
				carrierAgent.notifyDelivery(driverId,tourElement.getShipment(), time);
				activityCounter += 2;
			}
		}

		CarrierVehicle getVehicle() {
			return scheduledTour.getVehicle();
		}

		public CarrierShipment getShipment(Activity act, int planElementIndex) {
			if(activity2shipment.containsKey(planElementIndex)) return activity2shipment.get(planElementIndex);
			return null;
		}

		public void register(Integer planElementIndex,CarrierShipment shipment) {
			activity2shipment.put(planElementIndex,shipment);
		}
		
	}

	private static Logger logger = Logger.getLogger(CarrierAgent.class);

	private final Id id;

	private final Carrier carrier;

	private final CarrierAgentTracker tracker;

	private Collection<Id> driverIds = new ArrayList<Id>();

	private int nextId = 0;

	private Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();

	private Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();
	
	private ScoringFunction scoringFunction;

	CarrierAgent(CarrierAgentTracker carrierAgentTracker, Carrier carrier, CarrierScoringFunctionFactory scoringFunctionFactory) {
		this.tracker = carrierAgentTracker;
		this.carrier = carrier;
		this.id = carrier.getId();
		assert scoringFunctionFactory != null : "scoringFunctionFactory is null. this must not be.";
		this.scoringFunction = scoringFunctionFactory.createScoringFunction(carrier);
	}

	public Id getId() {
		return id;
	}

	public List<Plan> createFreightDriverPlans() {
		clear();
		List<Plan> plans = new ArrayList<Plan>();
		if (carrier.getSelectedPlan() == null) {
			return plans;
		}
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Id driverId = createDriverId(scheduledTour.getVehicle());
			Person driverPerson = createDriverPerson(driverId);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(this, driverId, scheduledTour, scoringFunction);
			int countPlanElements = 0;
			Plan plan = new PlanImpl();
			Activity startActivity = new ActivityImpl(FreightConstants.START,scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			countPlanElements++;
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
				if (tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg) {
					org.matsim.contrib.freight.carrier.Tour.Leg tourLeg = (org.matsim.contrib.freight.carrier.Tour.Leg) tourElement;
					Route route = tourLeg.getRoute();
					assert route != null : "missing route for carrier " + this.getId() + ". route must not be null";
					LegImpl leg = new LegImpl(TransportMode.car);
					leg.setRoute(route);
					leg.setDepartureTime(tourLeg.getDepartureTime());
					leg.setTravelTime(tourLeg.getExpectedTransportTime());
					leg.setArrivalTime(tourLeg.getDepartureTime() + tourLeg.getExpectedTransportTime());
					plan.addLeg(leg);
					countPlanElements++;
				} else if (tourElement instanceof TourActivity) {
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = new ActivityImpl(act.getActivityType(), act.getLocation());
					double endTime = act.getExpectedActEnd();
					tourElementActivity.setEndTime(endTime);
					plan.addActivity(tourElementActivity);
					if(act instanceof ShipmentBasedActivity){
						carrierDriverAgent.register(countPlanElements,((ShipmentBasedActivity) act).getShipment());
					}
					countPlanElements++;
				}
			}
			Activity endActivity = new ActivityImpl(FreightConstants.END,scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			countPlanElements++;
			driverPerson.addPlan(plan);
			plan.setPerson(driverPerson);
			plans.add(plan);
			carrierDriverAgents.put(driverId, carrierDriverAgent);
			driverTourMap.put(driverId, scheduledTour);
		}
		return plans;
	}

	private void clear() {
		carrierDriverAgents.clear();
		driverTourMap.clear();
		driverIds.clear();
		nextId = 0;
	}

	public Collection<Id> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}
	
	private Person createDriverPerson(Id driverId) {
		Person person = new PersonImpl(driverId);
		return person;
	}

	private Id createDriverId(CarrierVehicle carrierVehicle) {
		IdImpl id = new IdImpl("freight_" + carrier.getId() + "_veh_"
				+ carrierVehicle.getVehicleId());
		driverIds.add(id);
		++nextId;
		return id;
	}

	public void notifyPickup(Id driverId, CarrierShipment shipment, double time) {
		tracker.notifyPickedUp(carrier.getId(), driverId, shipment, time);
	}

	public void notifyDelivery(Id driverId, CarrierShipment shipment,
			double time) {
		tracker.notifyDelivered(carrier.getId(), driverId, shipment, time);
	}

	public void scoreSelectedPlan() {
		if (carrier.getSelectedPlan() == null) {
			return;
		}
		scoringFunction.finish();
		carrier.getSelectedPlan().setScore(scoringFunction.getScore());
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	
	private CarrierDriverAgent getDriver(Id driverId){
		return carrierDriverAgents.get(driverId);
	}

	public CarrierShipment getShipment(Id driverId, Activity act, int planElementIndex) {
		return getDriver(driverId).getShipment(act,planElementIndex);
	}

}
