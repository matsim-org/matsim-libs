/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.ev;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.withinday.utils.EditPlans;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

class ActivityWhileChargingFinder {

	private final Collection<String> activityTypes;
	private final double minimumActDuration;

	private Logger log = LogManager.getLogger(ActivityWhileChargingFinder.class);

	public ActivityWhileChargingFinder(Collection<String> possibleWhileChargingStartActTypes, double minimumActDuration){
		this.activityTypes = possibleWhileChargingStartActTypes;
		this.minimumActDuration = minimumActDuration;
	}

	/**
	 * last possible activity before leg that fulfills all criteria is returned
	 *
	 * @param plan
	 * @param leg
	 * @return null if no suitable activity was found
	 */
	@Nullable
	Activity findActivityWhileChargingBeforeLeg(MobsimAgent mobsimAgent, Plan plan, Leg leg){
		String evRoutingMode = TripStructureUtils.getRoutingMode(leg);
		Preconditions.checkState(plan.getPlanElements().contains(leg));
		List<PlanElement> planElementsBeforeLeg = plan.getPlanElements().subList(0, plan.getPlanElements().indexOf(leg));

		//collect precedent activitiesWhileCharging
		List<PlanElement> precedentPluginInteractions = planElementsBeforeLeg.stream().filter(planElement -> planElement.toString().contains((UrbanVehicleChargingHandler.PLUGIN_IDENTIFIER))).collect(Collectors.toList());
		List<Activity> precedentActsWhileCharging = new ArrayList<>();
		for (PlanElement precedentPluginInteraction : precedentPluginInteractions) {
			int index = plan.getPlanElements().indexOf(precedentPluginInteraction);
			precedentActsWhileCharging.add(EditPlans.findRealActAfter(mobsimAgent, index));
		}

//		List<Activity> activities = TripStructureUtils.getActivities(planElementsBeforeLeg, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
//
//		//get ev legs that can be diverted (i.e. where we can incorporate charging) and the corresponding, following activities (during which charging will take place)
//		List<Tuple<Leg, Activity>> evLegsWithFollowingActs = activities.stream()
//				.filter(activity -> activityTypes.contains(activity.getType()) && !precedentActsWhileCharging.contains(activity)) //look for the correct types and do no attempt to charge twice during one act
//				.map(activity -> planElementsBeforeLeg.indexOf(activity))
//				.filter(idx -> idx > 0)
////				.map(idx -> new Tuple<>(getPrecedentLegOfRoutingModeBeforeActivity(planElementsBeforeLeg, (Activity) planElementsBeforeLeg.get(idx), evRoutingMode), (Activity) planElementsBeforeLeg.get(idx)))
//				.map(idx -> new Tuple<>((Leg) planElementsBeforeLeg.get(idx - 1), (Activity) planElementsBeforeLeg.get(idx)))
//				.filter(tuple -> TripStructureUtils.getRoutingMode(tuple.getFirst()).equals(evRoutingMode))
//				.filter(tuple -> tuple.getFirst() != null)
//				.collect(Collectors.toList());
//
//		if(evLegsWithFollowingActs.isEmpty()) return null;

		List<Leg> evLegs = TripStructureUtils.getLegs(planElementsBeforeLeg).stream()
				.filter(ll -> ll.getMode().equals(leg.getMode())) //only legs of ev considered (not even access or egress legs, as we wanna use arrival times later)
				.collect(Collectors.toList());
		if(evLegs.isEmpty()) return null; // no ev leg previous to critical leg

		List<Tuple<Leg, Activity>> evLegsWithFollowingRealActs = evLegs.stream()
				.map(ll -> new Tuple<>(ll, EditPlans.findRealActAfter(mobsimAgent, planElementsBeforeLeg.indexOf(ll))))
				.filter(tuple -> !precedentActsWhileCharging.contains(tuple.getSecond())) // we need to make sure not trying to charge twice during the same activity
				.collect(Collectors.toList());

		evLegsWithFollowingRealActs.sort(Comparator.comparingDouble(tuple -> tuple.getFirst().getDepartureTime().seconds()));

		for (int i = evLegsWithFollowingRealActs.size() - 1; i >= 0; i--) {
			Tuple<Leg, Activity> tuple = evLegsWithFollowingRealActs.get(i);
			//all legs should be routed at this time so we do not expect leg.getRoute().getTravelTime() to be undefined
			double begin = tuple.getFirst().getDepartureTime().seconds() + tuple.getFirst().getRoute().getTravelTime().seconds();
			//the reason we do not the activity duration is that there could be a subtour in between the activity before which we start charging (i.e. tuple.getSecond()) and the next ev leg
			Leg nextEVRoutingModeLeg = getNextLegOfRoutingModeAfterActivity(plan.getPlanElements(), tuple.getSecond(), evRoutingMode);
			Preconditions.checkNotNull(nextEVRoutingModeLeg, "should not happen");
			double end = nextEVRoutingModeLeg.getDepartureTime().seconds();
			if(end - begin >= minimumActDuration) return tuple.getSecond();
		}
		return null ;
	}

	/**
	 *
	 * @param planElements
	 * @param activity
	 * @param routingMode
	 * @return null if no suitable leg was found
	 */
	@Nullable
	Leg getNextLegOfRoutingModeAfterActivity(List<PlanElement> planElements, Activity activity, String routingMode){
		int actIndex = planElements.indexOf(activity);
		if(actIndex < 0){
			log.warn("could not find activity within given list of plan elements");
			return null;
		}
		if(actIndex == planElements.size() - 1) {
			log.warn("the given activity is the last activity in the given list of plan elements");
			return null;
		}
		for (int ii = actIndex + 1; ii < planElements.size(); ii++){
			PlanElement element = planElements.get(ii);
			if(element instanceof Leg && TripStructureUtils.getRoutingMode((Leg) element).equals(routingMode)){
				return (Leg) element;
			}
		}
		return null;
	}

	/**
	 *
	 * @param planElements
	 * @param activity
	 * @param routingMode
	 * @return returns the leg with the highest index within {@code planElements}, that has the {@code routingMode}
	 */
	@Nullable
	Leg getPrecedentLegOfRoutingModeBeforeActivity(List<PlanElement> planElements, Activity activity, String routingMode){
		int actIndex = planElements.indexOf(activity);
		if(actIndex < 0){
			log.warn("could not find activity within given list of plan elements");
			return null;
		}
		if(actIndex == 0) {
			log.warn("the given activity is the first activity in the given list of plan elements");
			return null;
		}
		for (int ii = actIndex -1 ; ii >= 0 ; ii--){
			PlanElement element = planElements.get(ii);
			if(element instanceof Leg && TripStructureUtils.getRoutingMode((Leg) element).equals(routingMode)){
					return (Leg) element;
			}
		}
		return null;
	}

}
