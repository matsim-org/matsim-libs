package playground.wrashid.parkingChoice.scoring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;

public class ParkingScoreAccumulator implements AfterMobsimListener {

	private final ParkingScoreCollector parkingScoreCollector;

	private ParkingWalkingDistanceMeanAndStandardDeviationGraph parkingWalkingDistanceGraph = new ParkingWalkingDistanceMeanAndStandardDeviationGraph();

	public ParkingScoreAccumulator(ParkingScoreCollector parkingScoreCollector) {
		this.parkingScoreCollector = parkingScoreCollector;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		HashMap<Id, Double> walkingDistances = new HashMap<Id, Double>();
		parkingScoreCollector.finishHandling();

		Controler controler = event.getControler();

		EventsToScore eventsToScore = controler.getPlansScoring().getPlanScorer();

		for (Id personId : parkingScoreCollector.getPersonIdsWhoUsedCar()) {

			ScoringFunction scoringFunction = eventsToScore.getScoringFunctionForAgent(personId);

			if (scoringFunction instanceof ScoringFunctionAccumulator) {

				ScoringFunctionAccumulator scoringFuncAccumulator = (ScoringFunctionAccumulator) scoringFunction;

				ArrayList<ActivityScoring> activityScoringFunctions = scoringFuncAccumulator.getActivityScoringFunctions();

				double sumOfActTotalScore = 0;
				for (ActivityScoring activityScoring : activityScoringFunctions) {
					BasicScoring bs = (BasicScoring) activityScoring;
					sumOfActTotalScore += bs.getScore();
				}

				double disutilityOfWalking = 0;
				double sumOfWalkingTimes = parkingScoreCollector.getSumOfWalkingTimes(personId);
				double sumOfParkingDurations = parkingScoreCollector.getSumOfParkingDurations(personId);
				walkingDistances.put(personId, sumOfWalkingTimes * ParkingChoiceLib.getWalkingSpeed());

				disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;

				// System.out.println("sum of act score:" + sumOfActTotalScore);
				// System.out.println("sum of parking duration: "+
				// sumOfParkingDurations);
				// System.out.println("sum of walking durations: "+
				// sumOfWalkingTimes);
				// System.out.println("disutilityOfWalking:" +
				// disutilityOfWalking);
				// System.out.println("================");

				scoringFuncAccumulator.addMoney(disutilityOfWalking);
			}

		}
		writeWalkingDistanceStatisticsGraph(controler, walkingDistances);
		printWalkingDistanceHistogramm(controler, walkingDistances);
		
		//eventsToScore.finish();
	}

	private void printWalkingDistanceHistogramm(Controler controler, HashMap<Id, Double> walkingDistance){
		double[] values=Collections.convertDoubleCollectionToArray(walkingDistance.values());
		
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceHistogramm.png");
		
		GeneralLib.generateHistogram(fileName, values, 80, "Histogram Parking Walking Distance - It."+controler.getIterationNumber(), "distance", "number");
	}

	private void writeWalkingDistanceStatisticsGraph(Controler controler, HashMap<Id, Double> walkingDistance) {
		parkingWalkingDistanceGraph.updateStatisticsForIteration(controler.getIterationNumber(), walkingDistance);
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceOverIterations.png");
		parkingWalkingDistanceGraph.writeGraphic(fileName);
	}

	

}
