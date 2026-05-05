
/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPlanAgentImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.agents;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import java.util.List;

public final class BasicPlanAgentImpl implements DistributedMobsimAgent, PlanAgent, HasPerson, VehicleUsingAgent, HasModifiablePlan {

	private static final Logger log = LogManager.getLogger(BasicPlanAgentImpl.class);
	private static int finalActHasDpTimeWrnCnt = 0;
	private static int noRouteWrnCnt = 0;

	private final Plan plan;
	private final Scenario scenario;
	private final EventsManager events;
	private final MobsimTimer simTimer;
	final private TimeInterpretation timeInterpretation;

	private MobsimVehicle vehicle;
	private double activityEndTime;
	private int currentPlanElementIndex = 0;
	private MobsimAgent.State state = MobsimAgent.State.ABORT;
	private Id<Link> currentLinkId = null;

	/**
	 * This notes how far a route is advanced.  One could move this into the DriverAgent(Impl).  There, however, is no method to know about the
	 * start of the route, thus one has to guess when to set it back to zero.  Also, IMO there is really some logic to providing this as a service
	 * by the entity that holds the plan. Better ideas are welcome.  kai, nov'14
	 */
	private int currentLinkIndex = 0;

	public BasicPlanAgentImpl(BasicPlanAgentMessage message, Scenario scenario, EventsManager events, MobsimTimer simTimer, TimeInterpretation timeInterpretation) {

		// use the plan from the message, as a message should have been generated from a BasicPlanAgentImpl, with its own copy of the plan.
		this.plan = PopulationUtils.createPlan(message.planElements());
		this.plan.setPerson(scenario.getPopulation().getPersons().get(message.personId()));
		this.currentPlanElementIndex = message.currentPlanElementIndex();
		this.activityEndTime = message.activityEndTime();
		this.state = message.state();
		this.currentLinkId = message.currentLinkId();
		this.currentLinkIndex = message.currentLinkIndex();
		this.scenario = scenario;
		this.events = events;
		this.simTimer = simTimer;
		this.timeInterpretation = timeInterpretation;
	}

	public BasicPlanAgentImpl(Plan plan2, Scenario scenario, EventsManager events, MobsimTimer simTimer, TimeInterpretation timeInterpretation) {

		// make a copy of the plan which we can modify. The population keeps the original plan which is later scored.
		this.plan = PopulationUtils.createPlan(plan2.getPerson());
		PopulationUtils.copyFromTo(plan2, this.plan);
		// yy MZ suggests, and I agree, to always give the agent a full plan, and consume that plan as the agent goes.  kai, nov'14

		this.scenario = scenario;
		this.events = events;
		this.simTimer = simTimer;
		this.timeInterpretation = timeInterpretation;

		List<PlanElement> planElements = this.plan.getPlanElements();
		Preconditions.checkArgument(!planElements.isEmpty(), "Plan must consist of at least one activity");
		Activity firstAct = (Activity) planElements.getFirst();
		final Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity(firstAct, scenario);
		this.setCurrentLinkId(linkId);
		this.setState(State.ACTIVITY);
		calculateAndSetDepartureTime(firstAct);
	}

	@Override
	public void endLegAndComputeNextState(final double now) {
		this.getEvents().processEvent(new PersonArrivalEvent(now, this.getId(), this.getDestinationLinkId(), getCurrentLeg().getMode()));
		if ((!(this.getCurrentLinkId() == null && this.getDestinationLinkId() == null))
			&& !this.getCurrentLinkId().equals(this.getDestinationLinkId())) {
			log.error("The agent {} has destination link {}, but arrived on link {}. Setting agent state to ABORT.", this.getPerson().getId(), this.getDestinationLinkId(), this.getCurrentLinkId());
			this.setState(MobsimAgent.State.ABORT);
		} else {
			// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
			advancePlan(now);
		}

		this.currentLinkIndex = 0;
	}

	@Override
	public void setStateToAbort(final double now) {
		this.setState(MobsimAgent.State.ABORT);
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.setCurrentLinkId(linkId);
	}

	private void advancePlan(double now) {
		this.currentPlanElementIndex++;

		// check if plan has run dry:
		if (this.getCurrentPlanElementIndex() >= this.getCurrentPlan().getPlanElements().size()) {
			log.error("plan of agent with id = {} has run empty.  Setting agent state to ABORT (but continuing the mobsim).", this.getId());
			this.setState(MobsimAgent.State.ABORT);
			return;
		}

		PlanElement pe = this.getCurrentPlanElement();
		if (pe instanceof Activity act) {
			initializeActivity(act, now);
		} else if (pe instanceof Leg leg) {
			initializeLeg(leg);
		} else {
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}

	private void initializeLeg(Leg leg) {
		this.setState(MobsimAgent.State.LEG);
		this.currentLinkIndex = 0;
		if (leg.getRoute() == null) {
			log.error("The agent {} has no route in its leg.  Setting agent state to ABORT.", this.getPerson().getId());
			if (noRouteWrnCnt < 1) {
				log.info("(Route is needed inside Leg even if you want teleportation since Route carries the start/endLinkId info.)");
				noRouteWrnCnt++;
			}
			this.setState(MobsimAgent.State.ABORT);
		}
	}

	private void initializeActivity(Activity act, double now) {
		this.setState(MobsimAgent.State.ACTIVITY);
		this.getEvents().processEvent(new ActivityStartEvent(now, this.getId(), this.getCurrentLinkId(), act.getFacilityId(), act.getType(),
			act.getCoord()));
		calculateAndSetDepartureTime(act);
		act.setStartTime(now);
	}

	/**
	 * If this method is called to update a changed ActivityEndTime please
	 * ensure that the ActivityEndsList in the {@link QSim} is also updated.
	 */
	private void calculateAndSetDepartureTime(Activity act) {
		double now = this.getSimTimer().getTimeOfDay();
		double departure = Math.max(now, timeInterpretation.decideOnActivityEndTime(act, now).orElse(Double.POSITIVE_INFINITY));

		if (this.getCurrentPlanElementIndex() == this.getCurrentPlan().getPlanElements().size() - 1) {
			if (finalActHasDpTimeWrnCnt < 1 && departure != Double.POSITIVE_INFINITY) {
				log.error("last activity of person driver agent id {} has end time < infty; setting it to infty", this.getId());
				log.error(Gbl.ONLYONCE);
				finalActHasDpTimeWrnCnt++;
			}
			departure = Double.POSITIVE_INFINITY;
		}

		this.activityEndTime = departure;
	}

	@Override
	public void endActivityAndComputeNextState(final double now) {
		if (!(this.getCurrentPlanElement() instanceof Activity)) {
			log.warn("trying to end an activity but current plan element is not an activity; agentId={}", this.getId());
		}
		Activity act = (Activity) this.getCurrentPlanElement();
		this.getEvents().processEvent(new ActivityEndEvent(now, this.getPerson().getId(), this.currentLinkId, act.getFacilityId(), act.getType(), act.getCoord()));

		// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
		advancePlan(now);
	}

	@Override
	public void resetCaches() {
		if (this.getCurrentPlanElement() instanceof Activity act) {
			this.calculateAndSetDepartureTime(act);
		}
	}

	// ============================================================================
	// (nearly) pure getters and setters below here

	/**
	 * Returns a modifiable Plan for use by WithinDayAgentUtils in this package.
	 * This agent retains the copied plan and forgets the original one.  However, the original plan remains in the population file
	 * (and will be scored).  This is deliberate behavior!
	 */
	@Override
	public Plan getModifiablePlan() {
		// the agent has a modifiable version of the plan anyway
		return getCurrentPlan();
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		NetworkRoute route = (NetworkRoute) this.getCurrentLeg().getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) {
			return route.getVehicleId();
		}
		Gbl.assertIf(scenario.getConfig().qsim().getUsePersonIdForMissingVehicleId());
		return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
	}

	@Override
	public String getMode() {
		if (this.getCurrentPlanElementIndex() >= this.getCurrentPlan().getPlanElements().size()) {
			// just having run out of plan elements it not an argument for not being able to answer the "mode?" question,
			// thus we answer with "null".  This will likely result in an "abort". kai, nov'14
			return null;
		}

		if (getCurrentPlanElement() instanceof Leg leg) {
			return leg.getMode();
		} else {
			return null;
		}
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return timeInterpretation.decideOnLegTravelTime((Leg) this.getCurrentPlanElement());
	}

	@Override
	public Double getExpectedTravelDistance() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return ((Leg) currentPlanElement).getRoute().getDistance();
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return this.plan.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public PlanElement getNextPlanElement() {
		if (this.currentPlanElementIndex < this.plan.getPlanElements().size() - 1) {
			return this.plan.getPlanElements().get(this.currentPlanElementIndex + 1);
		} else {
			return null;
		}
	}

	public Activity getNextActivity() {
		for (int idx = this.currentPlanElementIndex + 1; idx < this.plan.getPlanElements().size(); idx++) {
			if (this.plan.getPlanElements().get(idx) instanceof Activity act) {
				return act;
			}
		}
		return null;
	}

	public Activity getPreviousActivity() {
		for (int idx = this.currentPlanElementIndex - 1; idx >= 0; idx--) {
			if (this.plan.getPlanElements().get(idx) instanceof Activity act) {
				return act;
			}
		}
		return null;
	}

	@Override
	public PlanElement getPreviousPlanElement() {
		if (this.currentPlanElementIndex >= 1) {
			return this.plan.getPlanElements().get(this.currentPlanElementIndex - 1);
		} else {
			return null;
		}
	}

	/* default */
	int getCurrentPlanElementIndex() {
		// Should this be made public?
		// Pro: Many programmers urgently seem to need this: They do everything possible to get to this index, including copy/paste of the whole class.
		// Con: I don't think that it is needed in many of those cases with a bit of thinking, and without it it really makes the code more flexible including
		// the possibility to, say, "consume" the plan while it is executed.
		// kai, may'15
		return currentPlanElementIndex;
	}

	@Override
	public Plan getCurrentPlan() {
		return plan;
	}

	@Override
	public Id<Person> getId() {
		return this.plan.getPerson().getId();
	}

	@Override
	public Person getPerson() {
		return this.plan.getPerson();
	}

	public Scenario getScenario() {
		return scenario;
	}

	public EventsManager getEvents() {
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
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId;
	}

	/* package */
	void setCurrentLinkId(@NotNull Id<Link> linkId) {
		this.currentLinkId = Preconditions.checkNotNull(linkId);
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.getCurrentLeg().getRoute().getEndLinkId();
	}

	@Override
	public double getActivityEndTime() {
		return this.activityEndTime;
	}

	@Override
	public MobsimAgent.State getState() {
		return state;
	}

	void setState(MobsimAgent.State state) {
		this.state = state;
	}

	Leg getCurrentLeg() {
		try {
			return (Leg) this.getCurrentPlanElement();
		} catch (ClassCastException e) {
			log.error("Oh no!");
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getCurrentLinkIndex() {
		// Other then getPlanElementIndex, it seems that this one here has to be accessible, since routes may contain loops, in which
		// case an indexOf search fails.  kai, nov'17
		return currentLinkIndex;
	}

	void incCurrentLinkIndex() {
		currentLinkIndex++;
	}

	@Override
	public Facility getCurrentFacility() {
		PlanElement pe = this.getCurrentPlanElement();
		Activity activity;
		if (pe instanceof Activity) {
			activity = (Activity) pe;
		} else if (pe instanceof Leg) {
			activity = this.getPreviousActivity();
		} else {
			throw new RuntimeException("unexpected type of PlanElement");
		}
		return FacilitiesUtils.toFacility(activity, scenario.getActivityFacilities());
	}

	@Override
	public Facility getDestinationFacility() {
		PlanElement pe = this.getCurrentPlanElement();
		if (pe instanceof Leg) {
			Activity activity = this.getNextActivity();
			return FacilitiesUtils.toFacility(activity, scenario.getActivityFacilities());

			// the above assumes alternating acts/legs.  I start having the suspicion that we should revoke our decision to give that up.
			// If not, one will have to use TripUtils to find the preceeding activity ... but things get more difficult.  Preferably, the
			// factility should then sit in the leg (since there it is used for routing).  kai, dec'15
		} else if (pe instanceof Activity) {
			return null;
		}
		throw new RuntimeException("unexpected type of PlanElement");
	}

	@Override
	public BasicPlanAgentMessage toMessage() {
		return new BasicPlanAgentMessage(plan.getPerson().getId(), plan.getPlanElements(), currentPlanElementIndex, activityEndTime, state, currentLinkId, currentLinkIndex);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("plan", plan)
			.add("vehicle", vehicle)
			.add("state", state)
			.add("currentPlanElementIndex", currentPlanElementIndex)
			.add("currentLinkId", currentLinkId)
			.toString();
	}

	/**
	 * Class to represent a basic plan agent as message object.
	 */
	public record BasicPlanAgentMessage(
		Id<Person> personId,
		List<PlanElement> planElements,
		int currentPlanElementIndex,
		double activityEndTime,
		MobsimAgent.State state,
		Id<Link> currentLinkId,
		int currentLinkIndex
	) implements Message {
	}
}
