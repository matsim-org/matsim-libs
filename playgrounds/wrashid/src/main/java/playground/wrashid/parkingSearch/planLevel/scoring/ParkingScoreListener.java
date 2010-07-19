package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingScoreListener implements AfterMobsimListener {

	private static HashMap<Id, Double> scoreHashMap = new HashMap<Id, Double>();

	public static HashMap<Id, Double> getScoreHashMap() {
		return scoreHashMap;
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			double score = 0.0;

			LinkedList<ActivityImpl> parkingTargetActivities = ParkingGeneralLib.getParkingTargetActivities(person
					.getSelectedPlan());

			for (ActivityImpl targetActivity : parkingTargetActivities) {
				Id parkingFacilityId = ParkingGeneralLib.getArrivalParkingAct(person.getSelectedPlan(), targetActivity)
						.getFacilityId();
				score += ParkingRoot.getParkingScoringFunction().getScore(targetActivity, person.getSelectedPlan(),
						parkingFacilityId, false);
			}

			scoreHashMap.put(person.getId(), score);

			event.getControler().getEvents().processEvent(new AgentMoneyEventImpl(0.0, person.getId(), score));
		}

	}

}
