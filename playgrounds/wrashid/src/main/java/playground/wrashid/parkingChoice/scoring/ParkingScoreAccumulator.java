package playground.wrashid.parkingChoice.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;


public class ParkingScoreAccumulator implements AfterMobsimListener {
	private static final Logger log = Logger.getLogger( ParkingScoreAccumulator.class );

	private static Set<String> selectedParkings;
	private static double[] sumOfOccupancyCountsOfSelectedParkings;
	private final ParkingScoreCollector parkingScoreCollector;
	private Double averageWalkingDistance = null;
	public static DoubleValueHashMap<Id<Person>> scores = new DoubleValueHashMap<>();
	public static HashMap<Id<Person>, Double> parkingWalkDistances=new HashMap<>();
	public static LinkedList<Double> parkingWalkDistancesInZHCity=new LinkedList<Double>();

	public Double getAverageWalkingDistance() {
		return averageWalkingDistance;
	}

	private ParkingWalkingDistanceMeanAndStandardDeviationGraph parkingWalkingDistanceGraph = new ParkingWalkingDistanceMeanAndStandardDeviationGraph();
	private ParkingManager parkingManager;

	public ParkingScoreAccumulator(ParkingScoreCollector parkingScoreCollector, ParkingManager parkingManager, final Controler controler) {
		this.parkingScoreCollector = parkingScoreCollector;
		this.parkingManager = parkingManager;

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			CharyparNagelScoringParametersForPerson parametersForPerson = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				sumScoringFunction.addScoringFunction(new ParkingScoring(controler.getConfig(), params, person.getId()));
				return sumScoringFunction;
			}
		});
	}

	class ParkingScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		private final Config config;
		private CharyparNagelActivityScoring activityScoring;
		private CharyparNagelMoneyScoring moneyScoring;
		private Id<Person> personId;

		ParkingScoring(Config config, CharyparNagelScoringParameters params, Id<Person> personId) {
			this.config = config;
			this.activityScoring = new CharyparNagelActivityScoring(params);
			this.moneyScoring = new CharyparNagelMoneyScoring(params);
			this.personId = personId;
		}

		@Override
		public void handleFirstActivity(Activity act) {
			activityScoring.handleFirstActivity(act);
		}

		@Override
		public void handleActivity(Activity act) {
			activityScoring.handleActivity(act);
		}

		@Override
		public void handleLastActivity(Activity act) {
			activityScoring.handleLastActivity(act);
		}

		@Override
		public void finish() {
			activityScoring.finish();
		}

		@Override
		public double getScore() {
			String parkingSelectionManager = config.findParam("parkingChoice", "parkingSelectionManager");
			if ( parkingSelectionManager==null ) {
				throw new RuntimeException("parkingSelectionManager is null, and there is no defense against this") ;
			}
			if (parkingSelectionManager.equalsIgnoreCase("shortestWalkingDistance")) {
				double sumOfActTotalScore = activityScoring.getScore();
				double sumOfWalkingTimes = parkingScoreCollector.getSumOfWalkingTimes(personId);
				System.err.println(" walkingTimes: " + sumOfWalkingTimes);
				double sumOfParkingDurations = parkingScoreCollector.getSumOfParkingDurations(personId);
				
				double disutilityOfWalking;
				if ( sumOfParkingDurations != 0. ) {
					disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
				} else {
					disutilityOfWalking = 0. ;
				}
				moneyScoring.addMoney(disutilityOfWalking);
				moneyScoring.finish();
			} else if (parkingSelectionManager.equalsIgnoreCase("PriceAndDistance_v1")) {
				// TODO: perhaps later make the parking price based on
				// acutal parking duration instead of
				// an estimation.

				moneyScoring.addMoney(scores.get(personId));


				DebugLib.emptyFunctionForSettingBreakPoint();
				// implicity disutility of not beeing able to perform
				// TODO: the following lines are removed for the time, so that they
				// can be repaired. For the herbie run1, they give sumOfActTotalScore=0, because
				// activityScoringFunction.finish() is invoked after the current code for it.
				// therefore we need to find a solution to solve this problem...

				// I think they should work now. (mz 2014)


				//                double sumOfActTotalScore = activityScoring.getScore();
				//                double sumOfWalkingTimes = parkingScoreCollector.getSumOfWalkingTimes(personId);
				//                System.err.println(" walkingTimes: " + sumOfWalkingTimes);
				//                double sumOfParkingDurations = parkingScoreCollector.getSumOfParkingDurations(personId);
				//disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
				//scoringFuncAccumulator.addMoney(disutilityOfWalking);

				moneyScoring.finish();

			} else {
				DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
			}
			if ( Double.isNaN( moneyScoring.getScore() ) ) {
				throw new RuntimeException( "score contribution is NaN") ;
			}
			return moneyScoring.getScore();
		}
	}



	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		HashMap<Id<Person>, Double> sumOfParkingWalkDistances = new HashMap<>();
		parkingScoreCollector.finishHandling();

		Controler controler = event.getControler();

		for (Id<Person> personId : parkingScoreCollector.getPersonIdsWhoUsedCar()) {

			double sumOfWalkingTimes = parkingScoreCollector.getSumOfWalkingTimes(personId);
			System.err.println(" walkingTimes: " + sumOfWalkingTimes );

			sumOfParkingWalkDistances.put(personId, sumOfWalkingTimes
					* event.getControler().getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk));

			//            if (!ParkingChoiceLib.isTestCaseRun) {
			//                String parkingSelectionManager = controler.getConfig().findParam("parking", "parkingSelectionManager");
			//                if (parkingSelectionManager.equalsIgnoreCase("PriceAndDistance_v1")) {
			//                    // TODO: perhaps later make the parking price based on
			//                    // acutal parking duration instead of
			//                    // an estimation.
			//
			//                    // In this case we have not set a custom scoring function and just throw an event for the money
			//                    // the agent hast to pay.
			//                    event.getControler().getEvents().processEvent(new PersonMoneyEvent(0.0, personId, scores.get(personId)));
			//
			//
			//                    DebugLib.emptyFunctionForSettingBreakPoint();
			//                    // implicity disutility of not beeing able to perform
			//                    // TODO: the following lines are removed for the time, so that they
			//                    // can be repaired. For the herbie run1, they give sumOfActTotalScore=0, because
			//                    // activityScoringFunction.finish() is invoked after the current code for it.
			//                    // therefore we need to find a solution to solve this problem...
			//
			//                    //disutilityOfWalking = -1 * Math.abs(sumOfActTotalScore) * sumOfWalkingTimes / sumOfParkingDurations;
			//                    //scoringFuncAccumulator.addMoney(disutilityOfWalking);
			//                } else {
			//                    DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
			//                }
			//            }

		}
		writeWalkingDistanceStatisticsGraph(controler, event.getIteration(), parkingWalkDistances);
		printWalkingDistanceHistogramm(controler, event.getIteration(), parkingWalkDistances);

		updateAverageValue(sumOfParkingWalkDistances);

		if (!ParkingChoiceLib.isTestCaseRun) {
			writeOutParkingOccupancies(controler, event.getIteration());
			writeOutGraphParkingTypeOccupancies(controler, event.getIteration());
			writeOutGraphComparingSumOfSelectedParkingsToCounts(controler, event.getIteration());
		}

		// eventsToScore.finish();
		findLargestWalkingDistance(parkingWalkDistances);
		parkingWalkDistances=new HashMap<>();
		parkingWalkDistancesInZHCity=new LinkedList<Double>();
		scores=new DoubleValueHashMap<>();

		if (event.getIteration()==0 || event.getIteration()%10==0){
			logParkingUsed(controler, event.getIteration());
		}

	}

	private void logParkingUsed(Controler controler, int iteration) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(iteration,
				"parkingLogInfo.txt");

		ArrayList<String> list=new ArrayList<String>();

		list.add("agentId\tparkingId\tstartParkingTime\tendParkingTime");

		StringBuffer sb=null;

		for (Id personId:parkingScoreCollector.parkingLog.getKeySet()){
			LinkedList<ParkingInfo>  parkingInfos=parkingScoreCollector.parkingLog.get(personId);

			for (ParkingInfo parkingInfo:parkingInfos){
				sb=new StringBuffer();

				sb.append(personId);
				sb.append("\t");
				sb.append(parkingInfo.getParkingId());
				sb.append("\t");
				sb.append(parkingInfo.getArrivalTime());
				sb.append("\t");
				sb.append(0.0);

				//TODO: there is some bug in the departure time, therefore it is set to zero at the
				// moment (it is also not needed for the application here, if needed, turn it on).

				list.add(sb.toString());
			}


		}

		GeneralLib.writeList(list, iterationFilename);
	}

	private void findLargestWalkingDistance(HashMap<Id<Person>, Double> parkingWalkDistances) {


		int numberOfLongDistanceWalks=0;
		for (Id<Person> personId:parkingWalkDistances.keySet()){
			Double walkingDistance = parkingWalkDistances.get(personId);
			if (walkingDistance> 300){
				//				System.out.println(personId + "\t" + walkingDistance);
				numberOfLongDistanceWalks++;
			}
		}

		System.out.println();

		// 1000055924
	}

	private void updateAverageValue(HashMap<Id<Person>, Double> sumOfParkingWalkDistances) {
		if ( sumOfParkingWalkDistances.isEmpty() ) {
			Logger.getLogger(this.getClass()).warn("sumOfParkingWalkDistances is empty; averaging will produce NaN") ;
		}
		double[] values = Collections.convertDoubleCollectionToArray(sumOfParkingWalkDistances.values());
		averageWalkingDistance = new Mean().evaluate(values);
	}

	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(Controler controler, int iteration) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyCountsComparison.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyCountsComparison.txt");

		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		int numberOfColumns=2;
		if ( selectedParkings==null ) {
			log.warn("not writing ... since selectedParkings is null") ;
			return ;		
		}
		for (String parkingName : selectedParkings) {
			ParkingOccupancyBins parkingOccupancyBins = parkingScoreCollector.parkingOccupancies.get(Id.create(
					mappingOfParkingNameToParkingId.get(parkingName), PParking.class));

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

	private void writeOutGraphParkingTypeOccupancies(Controler controler, int iteration) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyByParkingTypes.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyByParkingTypes.txt");

		int numberOfColumns=4;
		double matrix[][] = new double[96][numberOfColumns];

		for (Id<PParking> parkingId : parkingScoreCollector.parkingOccupancies.keySet()) {
			PParking parking = parkingManager.getParkingsHashMap().get(parkingId);
			if ( parking.getId()==null ) {
				log.warn("not writing out parking type occupancies since parking does not have its id set") ;
				continue ;
			}
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

	private void writeOutParkingOccupancies(Controler controler, int iteration) {
		String iterationFilename = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancy.txt");

		ArrayList<String> list = new ArrayList<String>();

		for (Id<PParking> parkingId : parkingScoreCollector.parkingOccupancies.keySet()) {
			PParking parking = parkingManager.getParkingsHashMap().get(parkingId);
			if ( parking.getId()==null ) {
				log.warn("not writing out parking occupancies since parking does not have its id set") ;
				continue ;
			}
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

	private static void printWalkingDistanceHistogramm(Controler controler, int iteration, HashMap<Id<Person>, Double> walkingDistance) {
		double[] values = Collections.convertDoubleCollectionToArray(walkingDistance.values());

		if (values.length == 0) {
			values = new double[1];
			values[0] = -1.0;
		}

		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"walkingDistanceHistogramm.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Parking Walking Distance - It." + iteration, "distance", "number");

		writeOutWalkingDistanceHistogramToTxtFile(controler, iteration, values);
		writeOutWalkingDistanceZHCity(controler, iteration);
	}

	private static void writeOutWalkingDistanceZHCity(Controler controler, int iteration) {
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"walkingDistanceHistogrammZHCity.txt");

		double[] walkingDistances=new double[parkingWalkDistancesInZHCity.size()];

		for (int i=0;i<parkingWalkDistancesInZHCity.size();i++){
			walkingDistances[i]=parkingWalkDistancesInZHCity.get(i);
		}

		GeneralLib.writeArrayToFile(walkingDistances, fileName, "parking walking distance [m]");
	}

	private static void writeOutWalkingDistanceHistogramToTxtFile(Controler controler, int iteration, double[] values) {
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"walkingDistanceHistogramm.txt");

		GeneralLib.writeArrayToFile(values, fileName, "parkingWalkingDistances [m]");
	}

	private void writeWalkingDistanceStatisticsGraph(Controler controler, int iteration, HashMap<Id<Person>, Double> walkingDistance) {
		parkingWalkingDistanceGraph.updateStatisticsForIteration(iteration, walkingDistance);
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
		Matrix countsMatrix = GeneralLib.readStringMatrix(baseFolder + "parkingGarageCountsCityZH27-April-2011.txt", "\t");

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
