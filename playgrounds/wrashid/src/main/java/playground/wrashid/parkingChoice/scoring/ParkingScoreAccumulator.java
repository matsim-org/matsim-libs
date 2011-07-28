package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
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
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class ParkingScoreAccumulator implements AfterMobsimListener {

	private static Set<String> selectedParkings;
	private static double[] sumOfOccupancyCountsOfSelectedParkings;
	private final ParkingScoreCollector parkingScoreCollector;
	private Double averageWalkingDistance = null;
	public static DoubleValueHashMap<Id> scores = new DoubleValueHashMap<Id>();
	public static HashMap<Id, Double> parkingWalkDistances=new HashMap<Id, Double>();
	public static LinkedList<Double> parkingWalkDistancesInZHCity=new LinkedList<Double>();
	
	public Double getAverageWalkingDistance() {
		return averageWalkingDistance;
	}

	private ParkingWalkingDistanceMeanAndStandardDeviationGraph parkingWalkingDistanceGraph = new ParkingWalkingDistanceMeanAndStandardDeviationGraph();
	private ParkingManager parkingManager;

	public ParkingScoreAccumulator(ParkingScoreCollector parkingScoreCollector, ParkingManager parkingManager) {
		this.parkingScoreCollector = parkingScoreCollector;
		this.parkingManager = parkingManager;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		HashMap<Id, Double> sumOfParkingWalkDistances = new HashMap<Id, Double>();
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
				sumOfParkingWalkDistances.put(personId, sumOfWalkingTimes
						* event.getControler().getConfig().plansCalcRoute().getWalkSpeed());

				if (!ParkingChoiceLib.isTestCaseRun) {
					String parkingSelectionManager = controler.getConfig().findParam("parking", "parkingSelectionManager");
					if (parkingSelectionManager.equalsIgnoreCase("shortestWalkingDistance")) {
						// this is only the implicit disutility (not an explicit
						// one)
						disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
						scoringFuncAccumulator.addMoney(disutilityOfWalking);
						// TODO: here the explicit disutility of walking is
						// totally missing

					} else if (parkingSelectionManager.equalsIgnoreCase("PriceAndDistance_v1")) {
						// TODO: perhaps later make the parking price based on
						// acutal parking duration instead of
						// an estimation.
						scoringFuncAccumulator.addMoney(scores.get(personId));
						DebugLib.emptyFunctionForSettingBreakPoint();
						// implicity disutility of not beeing able to perform
						// TODO: the following lines are removed for the time, so that they
						// can be repaired. For the herbie run1, they give sumOfActTotalScore=0, because
						// activityScoringFunction.finish() is invoked after the current code for it.
						// therefore we need to find a solution to solve this problem...
						
						//disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
						//scoringFuncAccumulator.addMoney(disutilityOfWalking);
					} else {
						DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
					}
				}

			}

		}
		writeWalkingDistanceStatisticsGraph(controler, parkingWalkDistances);
		printWalkingDistanceHistogramm(controler, parkingWalkDistances);
		
		updateAverageValue(sumOfParkingWalkDistances);
		
		if (!ParkingChoiceLib.isTestCaseRun) {
			writeOutParkingOccupancies(controler);
			writeOutGraphParkingTypeOccupancies(controler);
			writeOutGraphComparingSumOfSelectedParkingsToCounts(controler);
		}

		// eventsToScore.finish();
		findLargestWalkingDistance(parkingWalkDistances);
		parkingWalkDistances=new HashMap<Id, Double>();
		parkingWalkDistancesInZHCity=new LinkedList<Double>();
		scores=new DoubleValueHashMap<Id>();
	}

	private void findLargestWalkingDistance(HashMap<Id, Double> parkingWalkDistances) {
		int numberOfLongDistanceWalks=0;
		for (Id personId:parkingWalkDistances.keySet()){
			Double walkingDistance = parkingWalkDistances.get(personId);
			if (walkingDistance> 300){
//				System.out.println(personId + "\t" + walkingDistance);
				numberOfLongDistanceWalks++;
			}
		}
		
		System.out.println();
		
		// 1000055924
	}

	private void updateAverageValue(HashMap<Id, Double> sumOfParkingWalkDistances) {
		double[] values = Collections.convertDoubleCollectionToArray(sumOfParkingWalkDistances.values());
		averageWalkingDistance = new Mean().evaluate(values);
	}

	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(Controler controler) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"parkingOccupancyCountsComparison.txt");
		
		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		int numberOfColumns=2;
		for (String parkingName : selectedParkings) {
			ParkingOccupancyBins parkingOccupancyBins = parkingScoreCollector.parkingOccupancies.get(new IdImpl(
					mappingOfParkingNameToParkingId.get(parkingName)));

			if (parkingOccupancyBins == null) {
				continue;
			}

			int[] occupancy = parkingOccupancyBins.getOccupancy();
			for (int i = 0; i < 96; i++) {
				sumOfSelectedParkingSimulatedCounts[i] += occupancy[i];
			}
		}

		double matrix[][] = new double[96][numberOfColumns];

		for (int i = 0; i < 96; i++) {
			matrix[i][0] = sumOfSelectedParkingSimulatedCounts[i];
			matrix[i][1] = sumOfOccupancyCountsOfSelectedParkings[i];
		}

		String title = "Parking Garage Counts Comparison";
		String xLabel = "time (15min-bin)";
		String yLabel = "# of occupied parkings";
		String[] seriesLabels = new String[2];
		seriesLabels[0] = "simulated counts";
		seriesLabels[1] = "real counts";
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) 4;
		}

		GeneralLib.writeGraphic(iterationFilenamePng, matrix, title, xLabel, yLabel, seriesLabels, xValues);
		
		String txtFileHeader=seriesLabels[0];
		
		for (int i=1;i<numberOfColumns;i++){
			txtFileHeader+="\t"+seriesLabels[i];
		}
		GeneralLib.writeMatrix(matrix, iterationFilenameTxt, txtFileHeader);
	}

	private void writeOutGraphParkingTypeOccupancies(Controler controler) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyByParkingTypes.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"parkingOccupancyByParkingTypes.txt");

		int numberOfColumns=4;
		double matrix[][] = new double[96][numberOfColumns];

		for (Id parkingId : parkingScoreCollector.parkingOccupancies.keySet()) {
			Parking parking = parkingManager.getParkingsHashMap().get(parkingId);
			int graphIndex = -1;
			if (parking.getId().toString().startsWith("gp")) {
				graphIndex = 0;
			} else if (parking.getId().toString().startsWith("privateParkings")) {
				graphIndex = 1;
			} else if (parking.getId().toString().startsWith("publicPOutsideCityZH")) {
				graphIndex = 2;
			} else if (parking.getId().toString().startsWith("stp")) {
				graphIndex = 3;
			} else {
				DebugLib.stopSystemAndReportInconsistency("parking type (Id) unknown: " + parking.getId());
			}

			int[] occupancy = parkingScoreCollector.parkingOccupancies.get(parking.getId()).getOccupancy();
			for (int i = 0; i < 96; i++) {
				matrix[i][graphIndex] += occupancy[i];
			}
		}

		String title = "ParkingTypeOccupancies";
		String xLabel = "time (15min-bin)";
		String yLabel = "# of occupied parkings";
		String[] seriesLabels = new String[numberOfColumns];
		seriesLabels[0] = "garageParkings";
		seriesLabels[1] = "privateParkings";
		seriesLabels[2] = "publicParkingsOutsideCityZH";
		seriesLabels[3] = "streetParkings";
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) numberOfColumns;
		}

		GeneralLib.writeGraphic(iterationFilenamePng, matrix, title, xLabel, yLabel, seriesLabels, xValues);
		
		String txtFileHeader=seriesLabels[0];
		for (int i=1;i<numberOfColumns;i++){
			txtFileHeader+="\t"+seriesLabels[i];
		}
		GeneralLib.writeMatrix(matrix, iterationFilenameTxt, txtFileHeader);
	}

	private void writeOutParkingOccupancies(Controler controler) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancy.txt");

		ArrayList<String> list = new ArrayList<String>();

		for (Id parkingId : parkingScoreCollector.parkingOccupancies.keySet()) {
			Parking parking = parkingManager.getParkingsHashMap().get(parkingId);
			StringBuffer row = new StringBuffer(parking.getId().toString());

			ParkingOccupancyBins parkingOccupancyBins = parkingScoreCollector.parkingOccupancies.get(parking.getId());

			for (int i = 0; i < 96; i++) {
				row.append("\t");
				row.append(parkingOccupancyBins.getOccupancy(i * 900));
			}

			list.add(row.toString());
		}

		GeneralLib.writeList(list, iterationFilename);
	}

	private void printWalkingDistanceHistogramm(Controler controler, HashMap<Id, Double> walkingDistance) {
		double[] values = Collections.convertDoubleCollectionToArray(walkingDistance.values());

		if (values.length == 0) {
			values = new double[1];
			values[0] = -1.0;
		}

		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"walkingDistanceHistogramm.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Parking Walking Distance - It." + controler.getIterationNumber(), "distance", "number");
		
		writeOutWalkingDistanceHistogramToTxtFile(controler,values);
		writeOutWalkingDistanceZHCity(controler);
	}

	private void writeOutWalkingDistanceZHCity(Controler controler) {
		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"walkingDistanceHistogrammZHCity.txt");
		
		double[] walkingDistances=new double[parkingWalkDistancesInZHCity.size()];
		
		for (int i=0;i<parkingWalkDistancesInZHCity.size();i++){
			walkingDistances[i]=parkingWalkDistancesInZHCity.get(i);
		}
		
		GeneralLib.writeArrayToFile(walkingDistances, fileName, "parking walking distance [m]");
	}

	private void writeOutWalkingDistanceHistogramToTxtFile(Controler controler, double[] values) {
		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		"walkingDistanceHistogramm.txt");
		
		GeneralLib.writeArrayToFile(values, fileName, "parkingWalkingDistances [m]");
	}

	private void writeWalkingDistanceStatisticsGraph(Controler controler, HashMap<Id, Double> walkingDistance) {
		parkingWalkingDistanceGraph.updateStatisticsForIteration(controler.getIterationNumber(), walkingDistance);
		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceOverIterations.png");
		parkingWalkingDistanceGraph.writeGraphic(fileName);
	}

	public static void initializeParkingCounts(Controler controler) {
		String baseFolder = null;
		Double countsScalingFactor = Double.parseDouble(controler.getConfig().findParam("parking", "countsScalingFactor"));

		if (ParkingHerbieControler.isRunningOnServer) {
			baseFolder = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/counts/";
		} else {
			baseFolder = "H:/data/experiments/TRBAug2011/parkings/counts/";
		}
		StringMatrix countsMatrix = GeneralLib.readStringMatrix(baseFolder + "parkingGarageCountsCityZH27-April-2011.txt", "\t");

		HashMap<String, Double[]> occupancyOfAllSelectedParkings = SingleDayGarageParkingsCount
				.getOccupancyOfAllSelectedParkings(countsMatrix);

		selectedParkings = occupancyOfAllSelectedParkings.keySet();

		sumOfOccupancyCountsOfSelectedParkings = new double[96];

		for (String parkingName : selectedParkings) {
			Double[] occupancyBins = occupancyOfAllSelectedParkings.get(parkingName);

			if (occupancyBins == null) {
				DebugLib.stopSystemAndReportInconsistency();
			}

			for (int i = 0; i < 96; i++) {
				sumOfOccupancyCountsOfSelectedParkings[i] += countsScalingFactor * occupancyBins[i];
			}
		}

	}

}
