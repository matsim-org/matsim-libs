package playground.wrashid.PSF.converter.addingParkings;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;

/**
 * add parking to plans (leg + activities)
 * TODO: This is not consistent with the new parking model (where there is just one type of activity: parking...)
 * => change that!!!
 * => change also required in PSF, where the activity types are used.
 */
public class AddParkingsToPlans {

	/**
	 * As input this method receives a population, without parking acts (and
	 * related lets) and adds these.
	 */
	public static void generatePlanWithParkingActs(ScenarioImpl scenario) {
		// modify population and write it out again
		addParkings(scenario);
//		GeneralLib.writePopulation(scenario.getPopulation(), outputPlansFile);
	}

	/**
	 * Returns the same population object, but added with parking act/legs.
	 * The facility is also added to the existing acts.
	 */
	private static void addParkings(ScenarioImpl scenario) {

		for (Person person : scenario.getPopulation().getPersons().values()) {
			List<PlanElement> planElements = person.getSelectedPlan()
					.getPlanElements();
			List<PlanElement> newPlanElements = new LinkedList<PlanElement>();

			for (int i = 0; i < planElements.size(); i++) {
				if (planElements.get(i) instanceof Leg
						&& ((Leg) planElements.get(i)).getMode().equals(
								TransportMode.car)) {
					// only handle car legs specially
					// expand the car leg into 3 legs and 2 parking activities
					// home-car-work =>
					// home-walk-parkingDeparuture-car-parkingArrival-walk-work

					ActivityImpl previousActivity = (ActivityImpl)planElements.get(i - 1);
					ActivityImpl nextActivity = (ActivityImpl) planElements.get(i + 1);

					// add leg from previous Activity to parking
					newPlanElements.add(getParkingWalkLeg(scenario.getNetwork().getLinks().get(previousActivity.getLinkId())));

					// add parking departure activity
					newPlanElements.add(getParkingFacility(previousActivity,
							"parkingDeparture"));

					// add the actual car leg
					newPlanElements.add(planElements.get(i));

					// add parking arrival activity
					newPlanElements.add(getParkingFacility(nextActivity,
					"parkingArrival"));

					// add leg from parking to next activity Activity to parking
					newPlanElements.add(getParkingWalkLeg(scenario.getNetwork().getLinks().get(nextActivity.getLinkId())));


					// set the facility of the activities also
					previousActivity.setFacilityId(new IdImpl("facility_" + previousActivity.getLinkId().toString()));
					nextActivity.setFacilityId(new IdImpl("facility_" + previousActivity.getLinkId().toString()));

				} else {
					// add every thing else the new plan without change
					newPlanElements.add(planElements.get(i));
				}
			}

			planElements.clear();
			planElements.addAll(newPlanElements);
		}

	}

	// the leg for going to parking or back
	/**
	 * walk duration in seconds link: where both the facility and the parking
	 * is located
	 */
	private static Leg getParkingWalkLeg(Link link) {
		double walkDuration = 10; // in seconds

		Leg leg = new LegImpl(TransportMode.walk);

		leg.setTravelTime(walkDuration);
		leg.setRoute(new GenericRouteImpl(link.getId(), link.getId()));
		leg.getRoute().setTravelTime(walkDuration);

		return leg;
	}

	/**
	 * Add a parking facility at the same location as the given activity.
	 *
	 * activityType should either be parkingDeparture or parkingArrival
	 *
	 * @param activity
	 * @param activityType
	 * @return
	 */
	private static ActivityImpl getParkingFacility(ActivityImpl activity,
			String activityType) {
		double parkingActivityDuration = 10; // in seconds

		// copy the activity
		ActivityImpl parkingActivity = new ActivityImpl(activity);

		parkingActivity.setType(activityType);
		parkingActivity.setMaximumDuration(parkingActivityDuration);
		parkingActivity.setFacilityId(new IdImpl("facility_" + activity.getLinkId().toString()));

		return parkingActivity;
	}

}
