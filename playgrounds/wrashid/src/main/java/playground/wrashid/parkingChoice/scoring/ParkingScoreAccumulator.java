package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;
import java.util.HashMap;

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

import playground.wrashid.lib.DebugLib;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;

public class ParkingScoreAccumulator implements ScoringListener {

	private final ParkingScoreCollector parkingScoreCollector;

	private ParkingWalkingDistanceMeanAndStandardDeviationGraph parkingWalkingDistanceGraph=new ParkingWalkingDistanceMeanAndStandardDeviationGraph();
	
	public ParkingScoreAccumulator(ParkingScoreCollector parkingScoreCollector) {
		this.parkingScoreCollector = parkingScoreCollector;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		HashMap<Id, Double> walkingDistances=new HashMap<Id, Double>();
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
				walkingDistances.put(personId, sumOfWalkingTimes*ParkingChoiceLib.getWalkingSpeed());
				
				disutilityOfWalking=-1*Math.abs(sumOfActTotalScore)*sumOfWalkingTimes/sumOfParkingDurations;
				
				System.out.println("sum of act score:" + sumOfActTotalScore);
				System.out.println("sum of parking duration: "+ sumOfParkingDurations);
				System.out.println("sum of walking durations: "+ sumOfWalkingTimes);
				System.out.println("disutilityOfWalking:" + disutilityOfWalking);
				System.out.println("================");
				
				
				
				scoringFuncAccumulator.addMoney(disutilityOfWalking);
				
				writeWalkingDistanceStatisticsGraph(controler,walkingDistances);
			}
		
		}
		
	}
	
	
	private void writeWalkingDistanceStatisticsGraph(Controler controler, HashMap<Id, Double> walkingDistance){
		parkingWalkingDistanceGraph.updateStatisticsForIteration(controler.getIterationNumber(), walkingDistance);
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistance.png");
		parkingWalkingDistanceGraph.writeGraphic(fileName);
	}

	
	
}
