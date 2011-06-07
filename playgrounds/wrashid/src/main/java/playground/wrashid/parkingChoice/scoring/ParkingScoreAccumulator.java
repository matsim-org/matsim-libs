package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;

public class ParkingScoreAccumulator implements ScoringListener {

	private final ParkingScoreCollector parkingScoreCollector;

	public ParkingScoreAccumulator(ParkingScoreCollector parkingScoreCollector) {
		this.parkingScoreCollector = parkingScoreCollector;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		parkingScoreCollector.finishHandling();		
		
		Controler controler=event.getControler();
		
		EventsToScore eventsToScore = controler.getPlansScoring().getPlanScorer();
		
		for (Id personId : parkingScoreCollector.getPersonIdsWhoUsedCar()) {
			
			ScoringFunction scoringFunction = eventsToScore.getScoringFunctionForAgent(personId);
			
			if (scoringFunction instanceof ScoringFunctionAccumulator){
				
				
				ScoringFunctionAccumulator scoringFuncAccumulator=(ScoringFunctionAccumulator) scoringFunction;
			
				ArrayList<ActivityScoring> activityScoringFunctions = scoringFuncAccumulator.getActivityScoringFunctions();
				
				double sumOfActTotalScore=0;
				for (ActivityScoring activityScoring:activityScoringFunctions){
					BasicScoring bs=(BasicScoring) activityScoring;
					sumOfActTotalScore+=bs.getScore();
				}
				
				double disutilityOfWalking=0;
				double sumOfWalkingTimes = parkingScoreCollector.getSumOfWalkingTimes(personId);
				double sumOfParkingDurations = parkingScoreCollector.getSumOfParkingDurations(personId);
				
				disutilityOfWalking=-1*sumOfActTotalScore*sumOfWalkingTimes/sumOfParkingDurations;
				
				System.out.println("sum of act score:" + sumOfActTotalScore);
				System.out.println("sum of parking duration: "+ sumOfParkingDurations);
				System.out.println("sum of walking durations: "+ sumOfWalkingTimes);
				System.out.println("disutilityOfWalking:" + disutilityOfWalking);
				System.out.println("================");
				
				scoringFuncAccumulator.addMoney(disutilityOfWalking);
			}
		
		}
		
		
		
		
	}

	
	
}
