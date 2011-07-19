package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class ParkingScoreAccumulator implements AfterMobsimListener {

	private final ParkingScoreCollector parkingScoreCollector;
	private Double averageWalkingDistance=null;

	public Double getAverageWalkingDistance() {
		return averageWalkingDistance;
	}

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
				walkingDistances.put(personId, sumOfWalkingTimes * event.getControler().getConfig().plansCalcRoute().getWalkSpeed());

				// this is only the implicit disutility (not an explicit one)
				disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;

				scoringFuncAccumulator.addMoney(disutilityOfWalking);
			}

		}
		writeWalkingDistanceStatisticsGraph(controler, walkingDistances);
		printWalkingDistanceHistogramm(controler, walkingDistances);
		writeOutParkingOccupancies(controler);
		writeOutGraphParkingTypeOccupancies(controler);
		
		//eventsToScore.finish();
	}

	private void writeOutGraphParkingTypeOccupancies(Controler controler) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(), "parkingOccupancy.png");

		double matrix[][]=new double[96][5];
		
		for (Parking parking:parkingScoreCollector.parkingOccupancies.keySet()){
			int graphIndex=-1;
			if (parking.getId().toString().startsWith("gp")){
				graphIndex=0;
			} else if (parking.getId().toString().startsWith("ppIndoor")){
				graphIndex=1;
			}else if (parking.getId().toString().startsWith("ppOutdoor")){
				graphIndex=2;
			}else if (parking.getId().toString().startsWith("publicPOutsideCityZH")){
				graphIndex=3;
			}else if (parking.getId().toString().startsWith("stp")){
				graphIndex=4;
			} else {
				DebugLib.stopSystemAndReportInconsistency("parking type (Id) unknown: " + parking.getId());
			}
			
			int[] occupancy = parkingScoreCollector.parkingOccupancies.get(parking).getOccupancy();
			for (int i=0;i<96;i++){
				matrix[i][graphIndex]+=occupancy[i];
			}
		}
		
		String title="ParkingTypeOccupancies";
		String xLabel="time (15min-bin)";
		String yLabel="# of occupied parkings";
		String[] seriesLabels=new String[5];
		seriesLabels[0]="garageParkings";
		seriesLabels[1]="privateParkingsIndoor";
		seriesLabels[2]="privateParkingsOutdoor";
		seriesLabels[3]="publicParkingsOutsideCityZH";
		seriesLabels[4]="streetParkings";
		double[] xValues=new double[96];
		
		for (int i=0;i<96;i++){
			xValues[i]=i/(double)4;
		}
		
		GeneralLib.writeGraphic(iterationFilename, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}

	private void writeOutParkingOccupancies(Controler controler) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(), "parkingOccupancy.txt");
		
		
		ArrayList<String> list=new ArrayList<String>();
		
		for (Parking parking:parkingScoreCollector.parkingOccupancies.keySet()){
			StringBuffer row = new StringBuffer (parking.getId().toString());
			
			ParkingOccupancyBins parkingOccupancyBins = parkingScoreCollector.parkingOccupancies.get(parking);
			
			for (int i=0;i<96;i++){
				row.append("\t");
				row.append(parkingOccupancyBins.getOccupancy(i*900));
			}
			
			list.add(row.toString());
		}
		
		GeneralLib.writeList(list, iterationFilename);
	}

	private void printWalkingDistanceHistogramm(Controler controler, HashMap<Id, Double> walkingDistance){
		double[] values=Collections.convertDoubleCollectionToArray(walkingDistance.values());
		
		if (values.length==0){
			values=new double[1];
			values[0]=-1.0;
		}
		
		averageWalkingDistance=new Mean().evaluate(values);
		
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceHistogramm.png");
		
		GeneralLib.generateHistogram(fileName, values, 80, "Histogram Parking Walking Distance - It."+controler.getIterationNumber(), "distance", "number");
	}

	private void writeWalkingDistanceStatisticsGraph(Controler controler, HashMap<Id, Double> walkingDistance) {
		parkingWalkingDistanceGraph.updateStatisticsForIteration(controler.getIterationNumber(), walkingDistance);
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceOverIterations.png");
		parkingWalkingDistanceGraph.writeGraphic(fileName);
	}

	

}
