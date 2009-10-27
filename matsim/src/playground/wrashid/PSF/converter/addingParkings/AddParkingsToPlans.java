package playground.wrashid.PSF.converter.addingParkings;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicRoute;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.population.BasicRouteImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

import playground.andreas.bln.pop.NewPopulation;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.tryouts.plan.KeepOnlyMIVPlans;

/*
 * add parking to plans (leg + activities)
 */
public class AddParkingsToPlans {
	
	/**
	 * As input this method receives a plan file, without parking acts (and
	 * related lets) and adds these.
	 */
	public static void generatePlanWithParkingActs(String inputPlansFile,
			String networkFile, String outputPlansFile, String facilitiesFile) {
		Population inPop = GeneralLib.readPopulation(inputPlansFile,
				networkFile);
		// modify population and write it out again
		GeneralLib.writePopulation(addParkings(inPop, facilitiesFile), outputPlansFile);
	}

	/*
	 * Returns the same population object, but added with parking act/legs. 
	 * The facility is also added to the existing acts.
	 */
	private static Population addParkings(Population population, String facilitiesFile) {
		ActivityFacilitiesImpl facilities=GeneralLib.readActivityFacilities(facilitiesFile);
		
		for (Person person : population.getPersons().values()) {
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
					newPlanElements.add(getParkingWalkLeg(previousActivity
							.getLink()));

					// add parking departure activity
					newPlanElements.add(getParkingFacility(previousActivity,
							"parkingDeparture",facilities));
					
					// add the actual car leg
					newPlanElements.add(planElements.get(i));
					
					// add parking arrival activity
					newPlanElements.add(getParkingFacility(nextActivity,
					"parkingArrival",facilities));

					// add leg from parking to next activity Activity to parking
					newPlanElements.add(getParkingWalkLeg(nextActivity.getLink()));
					
					
					// set the facility of the activities also
					previousActivity.setFacility(facilities.getFacilities().get(new IdImpl("facility_" + previousActivity.getLinkId().toString())));
					nextActivity.setFacility(facilities.getFacilities().get(new IdImpl("facility_" + previousActivity.getLinkId().toString())));
					
				} else {
					// add every thing else the new plan without change
					newPlanElements.add(planElements.get(i));
				}
			}

			planElements.clear();
			planElements.addAll(newPlanElements);
		}

		return population;
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
		leg.setRoute(new GenericRouteImpl(link, link));
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
			String activityType, ActivityFacilitiesImpl facilities) {
		double parkingActivityDuration = 10; // in seconds

		// copy the activity
		ActivityImpl parkingActivity = new ActivityImpl((ActivityImpl) activity);

		parkingActivity.setType(activityType);
		parkingActivity.setDuration(parkingActivityDuration);
		parkingActivity.setFacility(facilities.getFacilities().get(new IdImpl("facility_" + activity.getLinkId().toString())));

		return parkingActivity;
	}

}
