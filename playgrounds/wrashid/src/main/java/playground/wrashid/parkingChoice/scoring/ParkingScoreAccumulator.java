package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;
import java.util.HashMap;
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
import playground.wrashid.parkingChoice.apiDefImpl.PriceAndDistanceParkingSelectionManager;
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
				walkingDistances.put(personId, sumOfWalkingTimes
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
						// implicity disutility of not beeing able to perform
						disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
						scoringFuncAccumulator.addMoney(disutilityOfWalking);
					} else {
						DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
					}
				}

			}

		}
		writeWalkingDistanceStatisticsGraph(controler, walkingDistances);
		printWalkingDistanceHistogramm(controler, walkingDistances);
		if (!ParkingChoiceLib.isTestCaseRun) {
			writeOutParkingOccupancies(controler);
			writeOutGraphParkingTypeOccupancies(controler);
			writeOutGraphComparingSumOfSelectedParkingsToCounts(controler);
		}

		// eventsToScore.finish();
	}

	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(Controler controler) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.png");

		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];

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

		double matrix[][] = new double[96][2];

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

		GeneralLib.writeGraphic(iterationFilename, matrix, title, xLabel, yLabel, seriesLabels, xValues);
	}

	private void writeOutGraphParkingTypeOccupancies(Controler controler) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancy.png");

		double matrix[][] = new double[96][4];

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
		String[] seriesLabels = new String[4];
		seriesLabels[0] = "garageParkings";
		seriesLabels[1] = "privateParkings";
		seriesLabels[2] = "publicParkingsOutsideCityZH";
		seriesLabels[3] = "streetParkings";
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) 4;
		}

		GeneralLib.writeGraphic(iterationFilename, matrix, title, xLabel, yLabel, seriesLabels, xValues);
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

		averageWalkingDistance = new Mean().evaluate(values);

		String fileName = controler.getControlerIO().getOutputFilename("walkingDistanceHistogramm.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Parking Walking Distance - It." + controler.getIterationNumber(), "distance", "number");
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
