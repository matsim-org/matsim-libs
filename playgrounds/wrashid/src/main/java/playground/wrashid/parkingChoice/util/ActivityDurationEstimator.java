package playground.wrashid.parkingChoice.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;

import java.util.LinkedList;
import java.util.List;


public class ActivityDurationEstimator implements ActivityStartEventHandler, PersonDepartureEventHandler {

	private final Controler controler;
	private final LinkedList<Double> activityDurationEstimations = new LinkedList<Double>();
	private final Id<Person> selectedPersonId;
	//private int indexOfActivity = 0;
	//private Double startTimeFirstAct = null;
	private final ActDurationEstimationContainer actDurationEstimationContainer=new ActDurationEstimationContainer();

	public ActivityDurationEstimator(final Controler controler, final Id<Person> selectedPersonId) {
		this.controler = controler;
		this.selectedPersonId = selectedPersonId;
	}

	@Override
	public void reset(final int iteration) {
		// TODO Auto-generated method stub

	}

	public LinkedList<Double> getActivityDurationEstimations() {
		return this.activityDurationEstimations;
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		if (event.getPersonId().equals(this.selectedPersonId)) {

			GeneralLib.controler=this.controler;

			this.actDurationEstimationContainer.registerNewActivity();
			if (this.actDurationEstimationContainer.isCurrentParkingTimeOver()){
				double estimatedActduration = getEstimatedActDuration(event, this.controler.getScenario(), this.actDurationEstimationContainer);
				this.activityDurationEstimations.add(estimatedActduration);
			}
		}
	}

	public static double getEstimatedActDuration(final ActivityStartEvent event, final Scenario scenario,
			final ActDurationEstimationContainer actDurationEstimationContainer) {

        Plan selectedPlan = scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();

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
				PlansCalcRouteConfigGroup plansCalcRoute = scenario.getConfig().plansCalcRoute();
				if (leg.getMode().equalsIgnoreCase("walk") || leg.getMode().equalsIgnoreCase("transit_walk")) {
					estimatedActduration += GeneralLib.getWalkingTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("bike")) {
					estimatedActduration += GeneralLib.getBikeTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("pt")) {
					estimatedActduration += GeneralLib.getPtTravelDuration(distance);
				} else if (leg.getMode().equalsIgnoreCase("ride")) {
					//as ride should disappear anyway, this the closest simple estimation,
					// which must not be correct for the algorithm to work.
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

	private static int getIndexOfFirstCarLegAfterCurrentAct(final int indexOfActivity, final List<PlanElement> planElements) {
		int indexOfFirstCarLegAfterCurrentAct = 0;
		for (int i = indexOfActivity; i < planElements.size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getMode().equalsIgnoreCase("car")) {
					indexOfFirstCarLegAfterCurrentAct = i;
					break;
				}
			}
		}
		return indexOfFirstCarLegAfterCurrentAct;
	}

	private static boolean isLastAct(final int indexOfActivity, final List<PlanElement> pe) {
		return indexOfActivity + 1 == pe.size();
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		if (event.getPersonId().equals(this.selectedPersonId)) {
			if (this.actDurationEstimationContainer.endTimeOfFirstAct == null) {
				this.actDurationEstimationContainer.endTimeOfFirstAct = event.getTime();
			}
		}
	}

}
