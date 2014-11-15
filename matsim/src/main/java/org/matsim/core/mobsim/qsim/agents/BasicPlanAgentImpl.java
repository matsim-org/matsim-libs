package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.NetworkAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.VehicleUsingAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

public final class BasicPlanAgentImpl implements PlanAgent, Identifiable<Person>, HasPerson, VehicleUsingAgent, NetworkAgent {

	private int currentPlanElementIndex = 0;
	private Plan plan;
	private boolean firstTimeToGetModifiablePlan = true;
	private final Scenario scenario;
	private final EventsManager events;
	private final MobsimTimer simTimer;
	private MobsimVehicle vehicle ;
	private Id<Link> currentLinkId = null;

	private transient Id<Link> cachedDestinationLinkId;
	// why is this transient?  "transient" means it is not included in automatic serialization/deserialization.  But where are we using this for this 
	// class?  kai, nov'14

	public BasicPlanAgentImpl(Plan plan2, Scenario scenario, EventsManager events, MobsimTimer simTimer) {
		this.plan = plan2 ;
		this.scenario = scenario ;
		this.events = events ;
		this.simTimer = simTimer ;
	}
	
	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.plan.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public final PlanElement getNextPlanElement() {
		if ( this.currentPlanElementIndex < this.plan.getPlanElements().size() ) {
			return this.plan.getPlanElements().get( this.currentPlanElementIndex+1 ) ;
		} else {
			return null ;
		}
	}


	/* default */ final int getCurrentPlanElementIndex() {
		return currentPlanElementIndex;
	}

	@Override
	public final Plan getCurrentPlan() {
		return plan;
	}

	private final void setPlan(Plan plan) {
		this.firstTimeToGetModifiablePlan = true ;
		this.plan = plan;
	}

	/**
	 * Returns a modifiable Plan for use by WithinDayAgentUtils in this package.
	 * This agent retains the copied plan and forgets the original one.  However, the original plan remains in the population file
	 * (and will be scored).  This is deliberate behavior!
	 */
	final Plan getModifiablePlan() {
		if (firstTimeToGetModifiablePlan) {
			firstTimeToGetModifiablePlan = false ;
			PlanImpl newPlan = new PlanImpl(this.getCurrentPlan().getPerson());
			newPlan.copyFrom(this.getCurrentPlan());
			this.setPlan(newPlan);
		}
		return this.getCurrentPlan();
	}


	void advancePlan() {
		this.currentPlanElementIndex++ ;
	}

	@Override
	public Id<Person> getId() {
		return this.plan.getPerson().getId() ;
	}

	@Override
	public Person getPerson() {
		return this.plan.getPerson() ;
	}

	Scenario getScenario() {
		return scenario;
	}

	EventsManager getEvents() {
		return events;
	}

	MobsimTimer getSimTimer() {
		return simTimer;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public void setVehicle(MobsimVehicle vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) {
			return route.getVehicleId();
		} else {
	        if (!getScenario().getConfig().qsim().getUsePersonIdForMissingVehicleId()) {
	            throw new IllegalStateException("NetworkRoute without a specified vehicle id.");
	        }
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
		}
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return this.currentLinkId;
	}
	
	/* package */ final void setCurrentLinkId( Id<Link> linkId ) {
		this.currentLinkId = linkId ;
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.cachedDestinationLinkId;
	}

	final void setDestinationLinkId( Id<Link> linkId ) {
		this.cachedDestinationLinkId = linkId ;
	}
}