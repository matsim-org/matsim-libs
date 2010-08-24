package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingScoreExecutor {

	private static HashMap<Id, Double> scoreHashMap = new HashMap<Id, Double>();

	public static HashMap<Id, Double> getScoreHashMap() {
		return scoreHashMap;
	}

	public void performScoring(AfterMobsimEvent event) {
		for (Person person : event.getControler().getPopulation().getPersons()
				.values()) {
			double score = 0.0;

			LinkedList<ActivityImpl> parkingTargetActivities = ParkingGeneralLib
					.getParkingTargetActivities(person.getSelectedPlan());

			for (ActivityImpl targetActivity : parkingTargetActivities) {
				Id parkingFacilityId = ParkingGeneralLib.getArrivalParkingAct(
						person.getSelectedPlan(), targetActivity)
						.getFacilityId();
				score += ParkingRoot.getParkingScoringFunction().getScore(
						targetActivity, person.getSelectedPlan(),
						parkingFacilityId, false);
			}

			scoreHashMap.put(person.getId(), score);

			event.getControler()
					.getEvents()
					.processEvent(
							new AgentMoneyEventImpl(0.0, person.getId(), score));

			updateParkingScoreSumInPersonGroupsForPerson(person.getId(), score);
		}
		printParkingScoreGraphForPersonGroups();
	}

	private void printParkingScoreGraphForPersonGroups() {
		if (ParkingRoot.getPersonGroupsForStatistics() != null) {
			PersonGroupParkingScoreGraphicGenerator.generateGraphic(ParkingRoot
					.getPersonGroupsForStatistics(),
					GlobalRegistry.controler.getControlerIO()
							.getOutputFilename("personGroupsParkingScore.png"));
		}
	}

	private void updateParkingScoreSumInPersonGroupsForPerson(Id personId,
			Double sumOfParkingScoresForPerson) {
		if (ParkingRoot.getPersonGroupsForStatistics() != null) {
			PersonGroups personGroupsStatistics = ParkingRoot
					.getPersonGroupsForStatistics();
			int iterationNumber = GlobalRegistry.controler.getIterationNumber();
			String attribute = PersonGroupParkingScoreGraphicGenerator.iterationScoreSum + iterationNumber;
			Double currentSum = (Double) personGroupsStatistics
					.getAttributeValueForGroupToWhichThePersonBelongs(personId,
							attribute);

			if (currentSum == null) {
				currentSum = 0.0;
			}

			currentSum += sumOfParkingScoresForPerson;

			personGroupsStatistics
					.setAttributeValueForGroupToWhichThePersonBelongs(personId,
							attribute, currentSum);
		}
	}

}
