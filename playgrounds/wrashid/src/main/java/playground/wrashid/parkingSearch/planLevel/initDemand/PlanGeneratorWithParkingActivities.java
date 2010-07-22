package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;

public class PlanGeneratorWithParkingActivities {

	ClosestParkingMatrix closestParkingMatrix;
	NetworkLayer network;
	ScenarioImpl scenario;

	public ScenarioImpl getScenario() {
		return scenario;
	}

	public PlanGeneratorWithParkingActivities(String inputPlansFilePath, String networkFilePath, String facilitiesFilePath) {
		scenario = new ScenarioImpl();

		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilePath);

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilePath);

		this.closestParkingMatrix = new ClosestParkingMatrix(scenario.getActivityFacilities(), network);

		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(inputPlansFilePath);
	}

	public void processPlans() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			addParkings(person.getSelectedPlan());
		}
	}

	private void addParkings(Plan plan) {
		int index = getIndexOfMissingParking(plan);

		while (index != Integer.MIN_VALUE) {
			if (index > 0) {
				addParkingActAndWalkLeg(plan.getPlanElements(), index);
			} else {
				addWalkLegAndParkingAct(plan.getPlanElements(), -1 * index);
			}
			index = getIndexOfMissingParking(plan);
		}

	}

	private void addWalkLegAndParkingAct(List<PlanElement> planElements, int index) {
		addParkingAct(planElements, index, (ActivityImpl) planElements.get(index - 1));
		addWalkLeg(planElements, index);
	}

	private void addWalkLeg(List<PlanElement> planElements, int index) {
		Leg leg = new LegImpl("walk");
		planElements.add(index, leg);
	}

	/**
	 * add a parking activity in the plan at the specified index and closest to
	 * the related activity.
	 * 
	 * @param plan
	 * @param index
	 * @param parkingFacility
	 */
	private void addParkingAct(List<PlanElement> planElements, int index, ActivityImpl relatedActivity) {
		double parkingActivityDuration = 30; // in seconds

		ActivityFacilityImpl parkingFacility = closestParkingMatrix.getClosestParkings(relatedActivity.getCoord(), 1, 1).get(0);

		ActivityImpl newParkingActivity = new ActivityImpl("parking", parkingFacility.getCoord());
		newParkingActivity.setFacilityId(parkingFacility.getId());
		newParkingActivity.setLinkId(network.getNearestLink(parkingFacility.getCoord()).getId());
		newParkingActivity.setDuration(parkingActivityDuration);

		planElements.add(index, newParkingActivity);
	}

	private void addParkingActAndWalkLeg(List<PlanElement> planElements, int index) {
		addWalkLeg(planElements, index);
		addParkingAct(planElements, index, (ActivityImpl) planElements.get(index + 1));
	}

	/**
	 * return plus index, if arriving parking missing. return minus index, id
	 * departing parking missing.
	 * 
	 * @param plan
	 * @return
	 */
	private int getIndexOfMissingParking(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg && ((Leg) pe).getMode().equalsIgnoreCase("car")) {
				int indexOfLeg = plan.getPlanElements().indexOf(pe);

				Activity nextAct = (Activity) plan.getPlanElements().get(indexOfLeg + 1);
				Activity previousAct = (Activity) plan.getPlanElements().get(indexOfLeg - 1);

				if (!nextAct.getType().equalsIgnoreCase("parking")) {
					return indexOfLeg + 1;
				}

				if (!previousAct.getType().equalsIgnoreCase("parking")) {
					return -1 * indexOfLeg;
				}
			}
		}

		return Integer.MIN_VALUE;
	}

	public void writePlans(String outputPlansFile) {
		GeneralLib.writePersons(scenario.getPopulation().getPersons().values(), outputPlansFile,network);
	}

}
