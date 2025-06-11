package org.matsim.contrib.ev.withinday;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import com.google.common.base.Preconditions;

/**
 * This is an internal utility class that manages the rewriting of agent plans
 * for everything that has to do with charging activities that are either
 * planned in the beginning of the day or online throughout the simulation.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
class ChargingScheduler {
	private final PopulationFactory populationFactory;
	private final TimeInterpretation timeInterpretation;
	private final ActivityFacilities facilities;
	private final RoutingModule roadRoutingModule;
	private final RoutingModule walkRoutingModule;
	private final Network network;

	public ChargingScheduler(PopulationFactory populationFactory, TimeInterpretation timeInterpretation,
			ActivityFacilities facilities, RoutingModule roadRoutingModule, RoutingModule walkRoutingModule,
			Network network) {
		this.populationFactory = populationFactory;
		this.timeInterpretation = timeInterpretation;
		this.facilities = facilities;
		this.roadRoutingModule = roadRoutingModule;
		this.walkRoutingModule = walkRoutingModule;
		this.network = network;
	}

	private int findPrecedingActivityIndex(List<PlanElement> elements, int index) {
		index--;

		while (index >= 0) {
			PlanElement element = elements.get(index);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (!TripStructureUtils.isStageActivityType(activity.getType())
						|| WithinDayEvEngine.isManagedActivityType(activity.getType())) {
					return index;
				}
			}

			index--;
		}

		throw new IllegalStateException();
	}

	private int findFollowingActivityIndex(List<PlanElement> elements, int index) {
		index++;

		while (index < elements.size()) {
			PlanElement element = elements.get(index);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (!TripStructureUtils.isStageActivityType(activity.getType())
						|| WithinDayEvEngine.isManagedActivityType(activity.getType())) {
					return index;
				}
			}

			index++;
		}

		throw new IllegalStateException();
	}

	public Activity scheduleInitialPlugActivity(MobsimAgent agent, Activity startActivity, Charger charger) {
		return schedulePlugActivity(agent, startActivity, charger, null);
	}

	public Activity scheduleSubsequentPlugActivity(MobsimAgent agent, Activity currentPlugActivity, Charger charger,
			double departureTime) {
		Preconditions.checkArgument(currentPlugActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int currentIndex = plan.getPlanElements().indexOf(currentPlugActivity);
		Preconditions.checkState(currentIndex >= 0);

		int nextActivityIndex = findFollowingActivityIndex(plan.getPlanElements(), currentIndex);
		Activity nextActivity = (Activity) plan.getPlanElements().get(nextActivityIndex);

		return schedulePlugActivity(agent, nextActivity, charger, departureTime);
	}

	/*
	 * This method schedules a ev:plug activity into the schedule. This happens at
	 * the beginning of the simulation to insert those plug activities as triggers
	 * for the charging procesess.
	 */
	private Activity schedulePlugActivity(MobsimAgent agent, Activity startActivity, Charger charger,
			Double departureTime) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int startActivityIndex = plan.getPlanElements().indexOf(startActivity);
		Preconditions.checkState(startActivityIndex >= 0);
		int precedingActivityIndex = findPrecedingActivityIndex(planElements, startActivityIndex);

		Activity precedingActivity = (Activity) planElements.get(precedingActivityIndex);
		Preconditions.checkState(!TripStructureUtils.isStageActivityType(startActivity.getType())
				|| WithinDayEvEngine.isManagedActivityType(startActivity.getType()));
		Preconditions.checkState(!TripStructureUtils.isStageActivityType(precedingActivity.getType())
				|| WithinDayEvEngine.isManagedActivityType(precedingActivity.getType()));

		// Find departure time
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		if (departureTime == null) {
			addTimeElements(timeTracker, planElements.subList(0, precedingActivityIndex + 1));
			departureTime = timeTracker.getTime().seconds();
		} else {
			timeTracker.setTime(departureTime);
			Preconditions.checkState(precedingActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));
		}

		// Remove existing trip
		planElements.removeAll(planElements.subList(precedingActivityIndex + 1, startActivityIndex));

		// insert drive to charger
		Facility precedingFacility = FacilitiesUtils.toFacility(precedingActivity, facilities);

		List<? extends PlanElement> driveToChargeElements = WithinDayAgentUtils
				.convertInteractionActivities(roadRoutingModule.calcRoute(
						DefaultRoutingRequest.of(precedingFacility, FacilitiesUtils.wrapLink(charger.getLink()),
								departureTime, plan.getPerson(), precedingActivity.getAttributes())));

		timeTracker.addElements(driveToChargeElements);

		int insertionIndex = precedingActivityIndex + 1;
		planElements.addAll(insertionIndex, driveToChargeElements);

		// insert plug activity
		Activity plugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.PLUG_ACTIVITY_TYPE,
				charger.getLink().getId());
		AttributesUtils.copyAttributesFromTo(startActivity, plugActivity);
		plugActivity.setStartTime(timeTracker.getTime().seconds());
		plugActivity.setMaximumDuration(Double.MAX_VALUE);

		insertionIndex += driveToChargeElements.size();
		planElements.add(insertionIndex, plugActivity);

		return plugActivity;
	}

	public Activity scheduleOnroutePlugActivity(MobsimAgent agent, Leg leg, Charger charger) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int legIndex = plan.getPlanElements().indexOf(leg);
		Preconditions.checkState(legIndex >= 0);
		int precedingActivityIndex = findPrecedingActivityIndex(planElements, legIndex);

		Activity precedingActivity = (Activity) planElements.get(precedingActivityIndex);
		Preconditions.checkState(!TripStructureUtils.isStageActivityType(precedingActivity.getType())
				|| WithinDayEvEngine.isManagedActivityType(precedingActivity.getType()));

		// Find departure time
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		addTimeElements(timeTracker, planElements.subList(0, precedingActivityIndex + 1));
		double departureTime = timeTracker.getTime().seconds();

		// Remove existing trip
		planElements.removeAll(planElements.subList(precedingActivityIndex + 1, legIndex + 1));

		// insert drive to charger
		Facility precedingFacility = FacilitiesUtils.toFacility(precedingActivity, facilities);

		List<? extends PlanElement> driveToChargeElements = WithinDayAgentUtils
				.convertInteractionActivities(roadRoutingModule.calcRoute(
						DefaultRoutingRequest.of(precedingFacility, FacilitiesUtils.wrapLink(charger.getLink()),
								departureTime, plan.getPerson(), precedingActivity.getAttributes())));

		timeTracker.addElements(driveToChargeElements);

		int insertionIndex = precedingActivityIndex + 1;
		planElements.addAll(insertionIndex, driveToChargeElements);

		// insert plug activity
		Activity plugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.PLUG_ACTIVITY_TYPE,
				charger.getLink().getId());
		AttributesUtils.copyAttributesFromTo(precedingActivity, plugActivity);
		plugActivity.setStartTime(timeTracker.getTime().seconds());
		plugActivity.setMaximumDuration(Double.MAX_VALUE);

		insertionIndex += driveToChargeElements.size();
		planElements.add(insertionIndex, plugActivity);

		return plugActivity;
	}

	/**
	 * This method lets an agent drive to the next main activity in the schedule.
	 * This happens when
	 * 
	 * (1) we are at an unplug activity, then charging process has
	 * finished successfully, and we now need to guide the agent from the unplug
	 * activity to the next planned main activity; and
	 * 
	 * (2) when a charging process
	 * is unsuccessful, but the agent should still continue, then we need to send
	 * him from the current charger to the initially planned main activity for
	 * charging.
	 */
	public void scheduleDriveToNextActivity(MobsimAgent agent) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int currentActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Activity currentActivity = (Activity) planElements.get(currentActivityIndex);

		Preconditions.checkState(currentActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE)
				|| currentActivity.getType().equals(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE));

		int mainActivityIndex = findFollowingActivityIndex(planElements, currentActivityIndex);
		Activity mainActivity = (Activity) planElements.get(mainActivityIndex);

		double departureTime = currentActivity.getEndTime().seconds();

		// remove intermediate elements
		planElements.removeAll(planElements.subList(currentActivityIndex + 1, mainActivityIndex));

		// insert drive to activity
		Facility currentFacility = FacilitiesUtils.toFacility(currentActivity, facilities);
		Facility mainFacility = FacilitiesUtils.toFacility(mainActivity, facilities);

		List<PlanElement> driveToActivityElements = WithinDayAgentUtils
				.convertInteractionActivities(roadRoutingModule.calcRoute(DefaultRoutingRequest.of(currentFacility,
						mainFacility, departureTime, plan.getPerson(), currentActivity.getAttributes())));

		int insertionIndex = currentActivityIndex + 1;
		planElements.addAll(insertionIndex, driveToActivityElements);
	}

	/**
	 * This method is called when a person is at a plug activity and the vehicle has
	 * been plugged successfully. We need to schedule the walk to the main activity,
	 * then skip everything until the end activity of the charging slot and insert
	 * another walk to the unplug activity.
	 */
	public Activity scheduleUntilUnplugActivity(MobsimAgent agent, Activity startActivity, Activity endActivity) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		// GOING TO THE MAIN ACTIVITY

		int plugActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Activity plugActivity = (Activity) planElements.get(plugActivityIndex);
		Preconditions.checkState(plugActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));

		int nextActivityIndex = findFollowingActivityIndex(planElements, plugActivityIndex);
		int startActivityIndex = planElements.indexOf(startActivity);
		Preconditions.checkState(nextActivityIndex == startActivityIndex);

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(plugActivity.getEndTime().seconds());

		// insert walk to activity
		Facility plugFacility = FacilitiesUtils.toFacility(plugActivity, facilities);
		Facility startFacility = FacilitiesUtils.toFacility(startActivity, facilities);

		List<? extends PlanElement> walkToStartElements = WithinDayAgentUtils.convertInteractionActivities(
				walkRoutingModule.calcRoute(DefaultRoutingRequest.of(plugFacility, startFacility,
						timeTracker.getTime().seconds(), plan.getPerson(), plugActivity.getAttributes())));

		timeTracker.addElements(walkToStartElements);
		timeTracker.addElement(startActivity);

		int insertionIndex = plugActivityIndex + 1;
		planElements.addAll(insertionIndex, walkToStartElements);

		// GOING TO THE UNPLUG ACTIVITY

		int endActivityIndex = planElements.indexOf(endActivity);
		Preconditions.checkState(endActivityIndex >= 0);
		Facility endFacility = FacilitiesUtils.toFacility(endActivity, facilities);

		if (endActivity == planElements.get(planElements.size() - 1)) {
			// the end activity is the last one of the schedule, so we don't actually
			// schedule an unplug activity
			return null;
		}

		for (int i = plugActivityIndex + 1; i <= endActivityIndex; i++) {
			timeTracker.addElement(planElements.get(i));
		}

		// insert walk from activity
		List<? extends PlanElement> walkToChargerElements = WithinDayAgentUtils.convertInteractionActivities(
				walkRoutingModule.calcRoute(DefaultRoutingRequest.of(endFacility, plugFacility,
						timeTracker.getTime().seconds(), plan.getPerson(), endActivity.getAttributes())));

		insertionIndex = endActivityIndex + 1;

		planElements.addAll(insertionIndex, walkToChargerElements);
		timeTracker.addElements(walkToChargerElements);

		// insert unplug activity
		Activity unplugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE,
				plugActivity.getLinkId());
		AttributesUtils.copyAttributesFromTo(endActivity, unplugActivity);
		unplugActivity.setStartTime(timeTracker.getTime().seconds());
		unplugActivity.setEndTime(Double.MAX_VALUE);

		insertionIndex += walkToChargerElements.size();
		planElements.add(insertionIndex, unplugActivity);
		timeTracker.addActivity(unplugActivity);

		// DRIVE TO NEXT MAIN ACTIVITY IN PLAN

		// find following activity
		int followingActivityIndex = findFollowingActivityIndex(planElements, insertionIndex + 1);
		Activity followingActivity = (Activity) planElements.get(followingActivityIndex);

		// remove deprecated drive after main activity
		planElements.removeAll(planElements.subList(insertionIndex + 1, followingActivityIndex));

		// replace with a new chain
		Facility unplugFacility = FacilitiesUtils.toFacility(unplugActivity, facilities);
		Facility followingFacility = FacilitiesUtils.toFacility(followingActivity, facilities);

		List<? extends PlanElement> driveToFollowingElements = WithinDayAgentUtils.convertInteractionActivities(
				roadRoutingModule.calcRoute(DefaultRoutingRequest.of(unplugFacility, followingFacility,
						timeTracker.getTime().seconds(), plan.getPerson(), plugActivity.getAttributes())));

		insertionIndex++;
		planElements.addAll(insertionIndex, driveToFollowingElements);

		return unplugActivity;
	}

	/**
	 * This method schedules an unplug activity after a main activity that is the
	 * first after which the charging mode is used. This means that the vehicle was
	 * plugged overnight, so the person needs to walk from the main activity to the
	 * unplug activity and then we need to reroute the agent.
	 */
	public Activity scheduleUnplugActivityAfterOvernightCharge(MobsimAgent agent, Activity endActivity,
			Charger charger) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int endActivityIndex = planElements.indexOf(endActivity);
		Preconditions.checkState(endActivityIndex >= 0);

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		addTimeElements(timeTracker, planElements.subList(0, endActivityIndex + 1));

		// create unplug activity
		Activity unplugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE,
				charger.getLink().getId());
		AttributesUtils.copyAttributesFromTo(endActivity, unplugActivity);

		// insert walk to unplug activity
		Facility endFacility = FacilitiesUtils.toFacility(endActivity, facilities);
		Facility unplugFacility = FacilitiesUtils.toFacility(unplugActivity, facilities);

		List<? extends PlanElement> walkElements = WithinDayAgentUtils.convertInteractionActivities(
				walkRoutingModule.calcRoute(DefaultRoutingRequest.of(endFacility, unplugFacility,
						timeTracker.getTime().seconds(), plan.getPerson(), endActivity.getAttributes())));

		int insertionIndex = endActivityIndex + 1;
		planElements.addAll(insertionIndex, walkElements);
		timeTracker.addElements(walkElements);

		// insert unplug activity
		insertionIndex += walkElements.size();
		unplugActivity.setStartTime(timeTracker.getTime().seconds());
		unplugActivity.setEndTime(Double.MAX_VALUE);
		planElements.add(insertionIndex, unplugActivity);

		return unplugActivity;
	}

	/**
	 * This method is called when a person successfully plugs at a charger in an
	 * on-route slot. The person should stay at the charger and stay there until the
	 * requested time has elapsed.
	 */
	public Activity scheduleUnplugActivityAtCharger(MobsimAgent agent, double duration) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int plugActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Activity plugActivity = (Activity) planElements.get(plugActivityIndex);
		Preconditions.checkState(plugActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));

		Activity waitActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.WAIT_ACTIVITY_TYPE,
				plugActivity.getLinkId());
		waitActivity.setMaximumDuration(duration);

		Activity unplugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE,
				plugActivity.getLinkId());
		unplugActivity.setEndTime(Double.MAX_VALUE);

		int insertionIndex = plugActivityIndex + 1;
		planElements.add(insertionIndex, waitActivity);

		insertionIndex++;
		planElements.add(insertionIndex, unplugActivity);

		return unplugActivity;
	}

	/**
	 * This method is called if charging at the first main activity which is
	 * followed by the charging mode is not successful. This means that an overnight
	 * charging did not succeed. The agent, hence, needs to walk to the charger
	 * location and pick up the car from there.
	 */
	public void scheduleAccessAfterOvernightCharge(MobsimAgent agent, Activity endActivity, Charger charger) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		int endActivityIndex = planElements.indexOf(endActivity);
		Preconditions.checkState(endActivityIndex >= 0);

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		addTimeElements(timeTracker, planElements.subList(0, endActivityIndex + 1));

		// create access activity
		Activity accessActivity = populationFactory.createActivityFromLinkId(
				WithinDayEvEngine.ACCESS_ACTIVITY_TYPE,
				charger.getLink().getId());
		AttributesUtils.copyAttributesFromTo(endActivity, accessActivity);

		// insert walk to access activity
		Facility endFacility = FacilitiesUtils.toFacility(endActivity, facilities);
		Facility accessFacility = FacilitiesUtils.toFacility(accessActivity, facilities);

		List<? extends PlanElement> walkElements = WithinDayAgentUtils.convertInteractionActivities(
				walkRoutingModule.calcRoute(DefaultRoutingRequest.of(endFacility, accessFacility,
						timeTracker.getTime().seconds(), plan.getPerson(), endActivity.getAttributes())));

		int insertionIndex = endActivityIndex + 1;
		planElements.addAll(insertionIndex, walkElements);
		timeTracker.addElements(walkElements);

		// insert access activity
		insertionIndex += walkElements.size();
		accessActivity.setStartTime(timeTracker.getTime().seconds());
		accessActivity.setEndTime(timeTracker.getTime().seconds());
		planElements.add(insertionIndex, accessActivity);

		int nextActivityIndex = findFollowingActivityIndex(planElements, insertionIndex);
		Activity nextActivity = (Activity) planElements.get(nextActivityIndex);

		double departureTime = timeTracker.getTime().seconds();

		// remove intermediate elements
		planElements.removeAll(planElements.subList(insertionIndex + 1, nextActivityIndex));

		// insert drive to next activity
		Facility nextFacility = FacilitiesUtils.toFacility(nextActivity, facilities);

		List<PlanElement> driveToActivityElements = WithinDayAgentUtils
				.convertInteractionActivities(roadRoutingModule.calcRoute(DefaultRoutingRequest.of(accessFacility,
						nextFacility, departureTime, plan.getPerson(), endActivity.getAttributes())));

		insertionIndex++;
		planElements.addAll(insertionIndex, driveToActivityElements);
	}

	/**
	 * This function is called when the plug activity is changed from one place to
	 * another, for instance, when an enroute change happens while approaching a
	 * planned plug activity.
	 */
	public Activity changePlugActivity(MobsimAgent agent, Activity currentPlugActivity, Charger charger,
			double now) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int currentLegIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Leg currentLeg = (Leg) plan.getPlanElements().get(currentLegIndex);

		Id<Link> currentLinkId = agent.getCurrentLinkId();
		Link currentLink = network.getLinks().get(currentLinkId);
		Facility currentFacility = FacilitiesUtils.wrapLink(currentLink);
		Facility plugFacility = FacilitiesUtils.wrapLink(charger.getLink());

		List<? extends PlanElement> driveToChargeElements = WithinDayAgentUtils
				.convertInteractionActivities(roadRoutingModule.calcRoute(DefaultRoutingRequest.of(currentFacility,
						plugFacility, now, plan.getPerson(), new AttributesImpl())));

		Preconditions.checkState(driveToChargeElements.size() == 1);
		Leg updatedLeg = (Leg) driveToChargeElements.get(0);
		Preconditions.checkState(updatedLeg.getMode().equals(currentLeg.getMode()));

		NetworkRoute currentRoute = (NetworkRoute) currentLeg.getRoute();
		NetworkRoute followingRoute = (NetworkRoute) updatedLeg.getRoute();

		int currentLinkIndex = currentRoute.getLinkIds().indexOf(currentLinkId);

		List<Id<Link>> updatedSequence = new LinkedList<>();
		updatedSequence.addAll(currentRoute.getLinkIds().subList(0, currentLinkIndex + 1));
		updatedSequence.addAll(followingRoute.getLinkIds());

		currentRoute.setLinkIds(currentRoute.getStartLinkId(), updatedSequence, followingRoute.getEndLinkId());

		if (currentPlugActivity != null) {
			Preconditions.checkState(currentPlugActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));
			Preconditions.checkState(plan.getPlanElements().get(currentLegIndex + 1) == currentPlugActivity);
			plan.getPlanElements().remove(currentLegIndex + 1); // remove plug activity
		}

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(now);
		timeTracker.addElements(driveToChargeElements);

		// insert new plug activity
		Activity plugActivity = populationFactory.createActivityFromLinkId(WithinDayEvEngine.PLUG_ACTIVITY_TYPE,
				charger.getLink().getId());

		if (currentPlugActivity != null) {
			AttributesUtils.copyAttributesFromTo(currentPlugActivity, plugActivity);
		}

		plugActivity.setStartTime(timeTracker.getTime().seconds());
		plugActivity.setMaximumDuration(Double.MAX_VALUE);
		plan.getPlanElements().add(currentLegIndex + 1, plugActivity);

		return plugActivity;
	}

	/**
	 * Called when an agent starts a charging process on a leg, but there is no
	 * planned activity-based nor leg-based charging slot. This is spontaneous
	 * charging.
	 */
	public Activity insertPlugActivity(MobsimAgent agent, Charger charger,
			double now) {
		return changePlugActivity(agent, null, charger, now);
	}

	private void addTimeElements(TimeTracker timeTracker, List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			if (element instanceof Activity activity && WithinDayEvEngine.isManagedActivityType(activity.getType())) {
				continue; // ignore our inifinite duration marker activities
			}

			timeTracker.addElement(element);
		}
	}
}
