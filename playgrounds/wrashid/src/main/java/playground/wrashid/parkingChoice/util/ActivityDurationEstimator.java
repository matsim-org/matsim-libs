package playground.wrashid.parkingChoice.util;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class ActivityDurationEstimator implements ActivityStartEventHandler, AgentDepartureEventHandler {

	private Controler controler;
	private LinkedList<Double> activityDurationEstimations = new LinkedList<Double>();
	private Id selectedPersonId;
	//private int indexOfActivity = 0;
	//private Double startTimeFirstAct = null;
	private ActDurationEstimationContainer actDurationEstimationContainer=new ActDurationEstimationContainer();

	public ActivityDurationEstimator(Controler controler, Id selectedPersonId) {
		this.controler = controler;
		this.selectedPersonId = selectedPersonId;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public LinkedList<Double> getActivityDurationEstimations() {
		return activityDurationEstimations;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().equals(selectedPersonId)) {

			GeneralLib.controler=controler;
			
			actDurationEstimationContainer.registerNewActivity();
			if (actDurationEstimationContainer.isCurrentParkingTimeOver()){
				double estimatedActduration = getEstimatedActDuration(event, controler, actDurationEstimationContainer);
				activityDurationEstimations.add(estimatedActduration);
			}
		}
	}

	public static double getEstimatedActDuration(ActivityStartEvent event, Controler controler,
			ActDurationEstimationContainer actDurationEstimationContainer) {
		
		Plan selectedPlan = controler.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();

		List<PlanElement> planElement = selectedPlan.getPlanElements();

		int indexOfActivity=actDurationEstimationContainer.indexOfCurrentActivity;
		double endTimeOfFirstAct=actDurationEstimationContainer.endTimeOfFirstAct;
		if (isLastAct(indexOfActivity, planElement)) {
			
			return GeneralLib.getIntervalDuration(event.getTime(), endTimeOfFirstAct);
		}

		int indexOfFirstCarLegAfterCurrentAct = getIndexOfFirstCarLegAfterCurrentAct(indexOfActivity, planElement);

		actDurationEstimationContainer.skipAllPlanElementsTillIndex=indexOfFirstCarLegAfterCurrentAct;
		
		double estimatedActduration = 0;

		for (int i = indexOfActivity; i < indexOfFirstCarLegAfterCurrentAct; i++) {
			if (planElement.get(i) instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) planElement.get(i);
				double endTime = act.getEndTime();
				double maximumDuration = act.getMaximumDuration();
				if (endTime != Double.NEGATIVE_INFINITY) {
					estimatedActduration += GeneralLib.getIntervalDuration(event.getTime(), endTime);
				} else {
					estimatedActduration += maximumDuration;
				}
			} else {
				// TODO: estimate travel time according to mode of travel...
				Leg leg = (Leg) planElement.get(i);
				ActivityImpl prevAct = (ActivityImpl) planElement.get(i - 1);
				ActivityImpl nextAct = (ActivityImpl) planElement.get(i + 1);
				double distance = GeneralLib.getDistance(prevAct.getCoord(), nextAct.getCoord());
				PlansCalcRouteConfigGroup plansCalcRoute = controler.getConfig().plansCalcRoute();
				if (leg.getMode().equalsIgnoreCase("walk") || leg.getMode().equalsIgnoreCase("transit_walk")) {
					estimatedActduration += GeneralLib.getWalkingTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("bike")) {
					estimatedActduration += GeneralLib.getBikeTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("pt")) {
					estimatedActduration += GeneralLib.getPtTravelDuration(distance);
				} else {
					// estimatedActduration +=
					// distance/plansCalcRoute.getUndefinedModeSpeed();
					// TODO: define speed for unknown mode here?
					DebugLib.stopSystemAndReportInconsistency("handle mode:" + leg.getMode());
				}
			}

		}
		return estimatedActduration;
	}

	private static int getIndexOfFirstCarLegAfterCurrentAct(int indexOfActivity, List<PlanElement> planElement) {
		int indexOfFirstCarLegAfterCurrentAct = 0;
		for (int i = indexOfActivity; i < planElement.size(); i++) {
			if (i % 2 == 1) {
				Leg leg = (Leg) planElement.get(i);
				if (leg.getMode().equalsIgnoreCase("car")) {
					indexOfFirstCarLegAfterCurrentAct = i;
					break;
				}
			}
		}
		return indexOfFirstCarLegAfterCurrentAct;
	}

	private static boolean isLastAct(int indexOfActivity, List<PlanElement> pe) {
		return indexOfActivity + 1 == pe.size();
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getPersonId().equals(selectedPersonId)) {
			if (actDurationEstimationContainer.endTimeOfFirstAct == null) {
				actDurationEstimationContainer.endTimeOfFirstAct = event.getTime();
			}
		}
	}

}
