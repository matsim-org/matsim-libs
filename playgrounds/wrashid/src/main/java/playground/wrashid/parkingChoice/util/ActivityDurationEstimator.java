package playground.wrashid.parkingChoice.util;

import java.util.LinkedList;
import java.util.List;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.qsim.agents.ActivityDurationUtils;
import org.matsim.core.population.ActivityImpl;


public class ActivityDurationEstimator implements ActivityStartEventHandler, PersonDepartureEventHandler {

	private final LinkedList<Double> activityDurationEstimations = new LinkedList<Double>();
	private final Id<Person> selectedPersonId;
	//private int indexOfActivity = 0;
	//private Double startTimeFirstAct = null;
	private final ActDurationEstimationContainer actDurationEstimationContainer=new ActDurationEstimationContainer();
	private Scenario scenario;

	public ActivityDurationEstimator(final Scenario scenario, final Id<Person> selectedPersonId) {
		this.selectedPersonId = selectedPersonId;
		this.scenario = scenario ;
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

			this.actDurationEstimationContainer.registerNewActivity();
			if (this.actDurationEstimationContainer.isCurrentParkingTimeOver()){
				Plan plan = scenario.getPopulation().getPersons().get( selectedPersonId ).getSelectedPlan() ;
				double estimatedActduration = getEstimatedActDuration(event, plan, this.actDurationEstimationContainer, this.scenario.getConfig() );
				this.activityDurationEstimations.add(estimatedActduration);
			}
		}
	}

	public static double getEstimatedActDuration(final ActivityStartEvent event, final Plan plan,
			final ActDurationEstimationContainer actDurationEstimationContainer, Config config) {
		PlansCalcRouteConfigGroup pcrConfig = config.plansCalcRoute() ;

		int indexOfActivity=actDurationEstimationContainer.getIndexOfCurrentActivity();
		double endTimeOfFirstAct=actDurationEstimationContainer.getEndTimeOfFirstAct();
		
		// if this is the last activity ...
		if (isLastAct(indexOfActivity, plan.getPlanElements())) {
			// ... then return the ``wrap around'' time from now to the beginning of the first activity "tomorrow":
			return GeneralLib.getIntervalDuration(event.getTime(), endTimeOfFirstAct);
		}

		int indexOfFirstCarLegAfterCurrentAct = getIndexOfFirstCarLegAfterCurrentAct(indexOfActivity, plan.getPlanElements());
		// (seems to do what it say)

		actDurationEstimationContainer.setSkipAllPlanElementsTillIndex(indexOfFirstCarLegAfterCurrentAct);

		double estimatedActduration = 0;

		for (int i = indexOfActivity; i < indexOfFirstCarLegAfterCurrentAct; i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(i);
				// this is hard-coded "tryEndtimeThenDuration":
				if (act.getEndTime() != Double.NEGATIVE_INFINITY) {
					estimatedActduration += GeneralLib.getIntervalDuration(event.getTime(), act.getEndTime());
				} else {
					estimatedActduration += act.getMaximumDuration();
				}
			} else {
				// TODO: estimate travel time according to mode of travel...
				Leg leg = (Leg) plan.getPlanElements().get(i);
				ActivityImpl prevAct = (ActivityImpl) plan.getPlanElements().get(i - 1);
				ActivityImpl nextAct = (ActivityImpl) plan.getPlanElements().get(i + 1);
				double distance = GeneralLib.getDistance(prevAct.getCoord(), nextAct.getCoord());
				if (leg.getMode().equalsIgnoreCase("walk") || leg.getMode().equalsIgnoreCase("transit_walk")) {
					estimatedActduration += GeneralLib.getWalkingTravelDuration(distance, pcrConfig );
				} else if (leg.getMode().equalsIgnoreCase("bike")) {
					estimatedActduration += GeneralLib.getBikeTravelDuration(distance, pcrConfig );
				} else if (leg.getMode().equalsIgnoreCase("pt")) {
					estimatedActduration += GeneralLib.getPtTravelDuration(distance, pcrConfig );
				} else if (leg.getMode().equalsIgnoreCase("ride")) {
					//as ride should disappear anyway, this the closest simple estimation,
					// which must not be correct for the algorithm to work.
					estimatedActduration += GeneralLib.getPtTravelDuration(distance, pcrConfig );
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
			if (this.actDurationEstimationContainer.getEndTimeOfFirstAct() == null) {
				this.actDurationEstimationContainer.setEndTimeOfFirstAct(event.getTime());
			}
		}
	}

}
