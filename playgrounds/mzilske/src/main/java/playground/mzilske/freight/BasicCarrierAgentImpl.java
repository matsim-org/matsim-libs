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
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.api.Offer;

public abstract class BasicCarrierAgentImpl implements CarrierAgent{

	private static Logger logger = Logger.getLogger(CarrierAgentImpl.class);
	
	protected Carrier carrier;
	
	protected Collection<Id> driverIds = new ArrayList<Id>();

	protected int nextId = 0;

	protected PlanAlgorithm router;
	
	protected Map<Id, CarrierDriverAgent> carrierDriverAgents = new HashMap<Id, CarrierDriverAgent>();
	
	protected Map<Id, ScheduledTour> driverTourMap = new HashMap<Id, ScheduledTour>();

	protected CarrierAgentTracker tracker;
	
	protected CarrierDriverAgentFactory driverAgentFactory;

	protected Id id;
	
	public BasicCarrierAgentImpl(CarrierAgentTracker carrierAgentTracker, Carrier carrier, PlanAlgorithm router, CarrierDriverAgentFactory driverAgentFactory) {
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

	public abstract void scoreSelectedPlan();
	

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

	public abstract CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery);

	public abstract void reset();

	public abstract void calculateCosts();
	
	public abstract void tellLink(Id personId, Id linkId);
		

	@Override
	public void notifyPickup(Id driverId, Shipment shipment, double time) {
		tracker.notifyPickup(carrier.getId(), driverId, shipment, time);
	}

	@Override
	public void notifyDelivery(Id driverId, Shipment shipment, double time) {
		tracker.notifyDelivery(carrier.getId(), driverId, shipment, time);
	}

	public abstract void informOfferRejected(Offer offer);
	
	public abstract void informOfferAccepted(Contract contract);

}
