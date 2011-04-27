package playground.wrashid.parkingChoice;

import java.util.ArrayList;
import java.util.LinkedList;

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

import playground.wrashid.PSF.parking.ParkingTimes;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class ParkingScoring implements ScoringListener {


	private final ParkingTimesPlugin parkingTimes;
	private Controler controler;
	private final ParkingManager parkingManager;

	public ParkingScoring(ParkingTimesPlugin parkingTimes, Controler controler, ParkingManager parkingManager){
		this.parkingTimes = parkingTimes;
		this.controler = controler;
		this.parkingManager = parkingManager;
	}
	
	@Override
	public void notifyScoring(ScoringEvent event) {
		parkingTimes.closeLastAndFirstParkingIntervals();

		performVehicleParkingForAllVehicles();
		
		
		LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals = parkingTimes.getParkingTimeIntervals();
		
		EventsToScore eventsToScore = controler.getPlansScoring().getPlanScorer();
		
		for (Person person : controler.getPopulation().getPersons().values()) {
			
			Id personId = person.getId();
			
			double sumOfActivityDurations=0.0;
			double actTotalScore=0;
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			for (ParkingIntervalInfo parkingInterval:parkingIntervals){
				sumOfActivityDurations+=parkingInterval.getDuration();
			}
			
			System.out.println("person:" + personId);
			
			ScoringFunction scoringFunction = eventsToScore.getScoringFunctionForAgent(personId);
			
			if (scoringFunction instanceof ScoringFunctionAccumulator){
				
				
				ScoringFunctionAccumulator scoringFuncAccumulator=(ScoringFunctionAccumulator) scoringFunction;
			
				ArrayList<ActivityScoring> activityScoringFunctions = scoringFuncAccumulator.getActivityScoringFunctions();
				
				for (ActivityScoring activityScoring:activityScoringFunctions){
					BasicScoring bs=(BasicScoring) activityScoring;
					actTotalScore+=bs.getScore();
				}
				System.out.println("act total score:" + actTotalScore);
				
				// perform scoring for the car parking.
				performParkingScoring(scoringFuncAccumulator);
			}
			
			double averageActPerformanceEarningRate =actTotalScore/sumOfActivityDurations;
			System.out.println("averageActPerformanceEarningRate:" + averageActPerformanceEarningRate);
			
			
			System.out.println("total score:" + scoringFunction.getScore());
		}
		
	
	}

	private void performVehicleParkingForAllVehicles() {
		LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals = parkingTimes.getParkingTimeIntervals();
		
		for (Person person : controler.getPopulation().getPersons().values()) {
			parkingManager.parkVehicle(person.getId(), null);
		}
		
	}

	private void performParkingScoring(ScoringFunctionAccumulator sfAccum) {
		sfAccum.addMoney(-1000);
	}

	
	
}
