package gunnar.ihop2.regent.costwriting;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import floetteroed.utilities.math.Histogram;
import gunnar.ihop2.integration.MATSimDummy;

/**
 * 
 * Depends probably on using
 * config.plancalcscore().setWriteExperiencedPlans(true).
 * 
 * Every activity (tour) type is allowed to show up at most once.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class HalfTourCostMatrices {

	// ------------------ MEMBERS ------------------

	private final TripCostMatrices tripCostMatrices;

	private final Map<String, Histogram> actType2tourStartTimeHist = new LinkedHashMap<>();

	private final Map<String, Histogram> actType2returnTripStartTimeHist = new LinkedHashMap<>();

	private final Map<String, Matrices> costType2halfTourCostMatrices = new LinkedHashMap<>();

	// ------------------ CONSTRUCTION ------------------

	public HalfTourCostMatrices(final Scenario scenario,
			final TripCostMatrices tripCostMatrices) {
		this.tripCostMatrices = tripCostMatrices;
		this.extractHistograms(scenario);
		// >>>>> TODO NEW >>>>>
		this.ensureWellFormedHistograms();
		// <<<<< TODO NEW <<<<<
		if (tripCostMatrices != null) {
			this.computeHalfTourCosts();
		}
	}

	// ------------------ INTERNALS ------------------

	private void addTimeToHistogram(
			final Map<String, Histogram> actType2timeHist, String actType,
			final double time_s) {

		// >>>>>>>>>> TODO NEW >>>>>>>>>>

		if (actType.toUpperCase().startsWith("W")) {
			actType = "work";
		} else if (actType.toUpperCase().startsWith("O")) {
			actType = "other";
		} else {
			throw new RuntimeException("unknown activity type: " + actType);
		}

		// <<<<<<<<<< TODO NEW <<<<<<<<<<

		Histogram histogram = actType2timeHist.get(actType);
		if (histogram == null) {
			histogram = Histogram.newHistogramWithUniformBins(
					this.tripCostMatrices.getStartTime_s(),
					this.tripCostMatrices.getBinSize_s(),
					this.tripCostMatrices.getBinCnt());
			actType2timeHist.put(actType, histogram);
		}
		histogram.add(time_s);
	}

	private void addTourStartTime(final String actType, final double time_s) {
		this.addTimeToHistogram(this.actType2tourStartTimeHist, actType, time_s);
	}

	private void addReturnTripStartTime(final String actType,
			final double time_s) {
		this.addTimeToHistogram(this.actType2returnTripStartTimeHist, actType,
				time_s);
	}

	private void extractHistograms(final Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			if (plan == null) {
				Logger.getLogger(this.getClass().getName()).warning(
						"person " + person + " has no selected plan");
			} else {
				Activity prevAct = null;
				Double prevDptTime_s = null;
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						final Activity currentAct = (Activity) planElement;
						// TODO CHECK, NEW:
						if (!currentAct.getType().toUpperCase().startsWith("H")) {
							// if (!HOME.equals(currentAct.getType())) {
							// Prev. trip was from home to activity location.
							this.addTourStartTime(currentAct.getType(),
									prevDptTime_s);
						} else if (prevAct != null) {
							// Prev. trip was from activity location to home.
							this.addReturnTripStartTime(prevAct.getType(),
									prevDptTime_s);
						}
						prevAct = currentAct;
						prevDptTime_s = currentAct.getEndTime();
					}
				}
			}
		}
	}

	private void ensureWellFormedHistograms() {
		Logger.getLogger(this.getClass().getName()).info(
				"Adding one to all trip start histogram time bins in order to "
						+ "avoid numerical problems.");
		for (Histogram hist : this.actType2tourStartTimeHist.values()) {
			hist.makeNonZero();
		}
		for (Histogram hist : this.actType2returnTripStartTimeHist.values()) {
			hist.makeNonZero();
		}
	}

	private void computeHalfTourCosts() {

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Using " + threadCnt + " threads.");

		final ExecutorService threadPool = Executors
				.newFixedThreadPool(threadCnt);

		for (String costType : this.tripCostMatrices.getCostTypes()) {

			this.costType2halfTourCostMatrices.put(costType, new Matrices());

			for (String actType : this.actType2tourStartTimeHist.keySet()) {

				/*
				 * Identify the total share of trips contained in the analysis
				 * period.
				 */

				double freqSumTourStart = 0.0;
				double freqSumReturnTripStart = 0.0;
				for (int costMatrixBin = 0; costMatrixBin < this.tripCostMatrices
						.getBinCnt(); costMatrixBin++) {
					freqSumTourStart += this.actType2tourStartTimeHist.get(
							actType).freq(costMatrixBin + 1);
					freqSumReturnTripStart += this.actType2returnTripStartTimeHist
							.get(actType).freq(costMatrixBin + 1);
				}
				Logger.getLogger(this.getClass().getName()).info(
						(freqSumTourStart * 100.0)
								+ " percent of the population traveled to "
								+ actType + " during the analyzed time window");
				Logger.getLogger(this.getClass().getName())
						.info((freqSumReturnTripStart * 100.0)
								+ " percent of the population traveled back from "
								+ actType + " during the analyzed time window");

				/*
				 * Now compute the tour travel times as a weighted sum of trip
				 * travel times.
				 */

				{
					// inner block to ensure that this matrix is accessed by
					// only one thread
					final Matrix halfTourCostMatrix = this.costType2halfTourCostMatrices
							.get(costType).createMatrix(actType.toUpperCase(),
									costType);
					final HalfTourCostMatrixCalculator halfTourMatrixCostCalculator = new HalfTourCostMatrixCalculator(
							halfTourCostMatrix, costType, actType,
							actType2tourStartTimeHist, freqSumTourStart,
							actType2returnTripStartTimeHist,
							freqSumReturnTripStart, tripCostMatrices);
					threadPool.execute(halfTourMatrixCostCalculator);
				}
			}
		}

		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException();
		}
	}

	public void writeHalfTourTravelTimesToFiles(
			final Map<String, String> costType2fileName) {
		for (Map.Entry<String, String> costType2fileNameEntry : costType2fileName
				.entrySet()) {
			final MatricesWriter writer = new MatricesWriter(
					this.costType2halfTourCostMatrices
							.get(costType2fileNameEntry.getKey()));
			writer.setIndentationString("  ");
			writer.setPrettyPrint(true);
			writer.write(costType2fileNameEntry.getValue());
		}
	}

	// TODO NEW
	public Matrices getHalfTourCostMatrices(final String costType) {
		return this.costType2halfTourCostMatrices.get(costType);
	}
	
	public void writeHistogramsToFile(final String histogramFileName) {

		try {
			Logger.getLogger(this.getClass().getName()).info(
					"writing histogram file " + histogramFileName);
			final PrintWriter writer = new PrintWriter(histogramFileName);

			writer.print("TIMEBIN");
			for (String actType : this.actType2tourStartTimeHist.keySet()) {
				writer.print("\t");
				writer.print("TO-TRIPS-RATE(" + actType + ")");
				writer.print("\t");
				writer.print("BACK-TRIPS-RATE(" + actType + ")");
				writer.print("\t");
				writer.print("TO-TRIPS-TOTAL(" + actType + ")");
				writer.print("\t");
				writer.print("BACK-TRIPS-TOTAL(" + actType + ")");
			}
			writer.println();

			for (int bin = 0; bin < this.actType2tourStartTimeHist.values()
					.iterator().next().binCnt(); bin++) {
				writer.print(bin - 1);
				for (String actType : this.actType2tourStartTimeHist.keySet()) {
					writer.print("\t");
					writer.print(this.actType2tourStartTimeHist.get(actType)
							.freq(bin));
					writer.print("\t");
					writer.print(this.actType2returnTripStartTimeHist.get(
							actType).freq(bin));
					writer.print("\t");
					writer.print(this.actType2tourStartTimeHist.get(actType)
							.cnt(bin));
					writer.print("\t");
					writer.print(this.actType2returnTripStartTimeHist.get(
							actType).cnt(bin));
				}
				writer.println();
			}

			writer.flush();
			writer.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).severe(
					"Failed to write histograms to file: " + e.getMessage());
		}

		// try {
		// final PrintWriter writer = new PrintWriter(histogramFileName);
		// writer.println("Departure time histograms per activity type.");
		// writer.println();
		// for (Map.Entry<String, Histogram> act2histEntry :
		// this.actType2tourStartTimeHist
		// .entrySet()) {
		// writer.println(act2histEntry.getKey());
		// writer.println();
		// writer.println(act2histEntry.getValue());
		// writer.println();
		// writer.println();
		// }
		// writer.flush();
		// writer.close();
		// } catch (FileNotFoundException e) {
		// Logger.getLogger(this.getClass().getName()).warning(
		// "could not write departure " + "time histograms to file "
		// + histogramFileName);
		// }
	}

	// ------------------ MAIN-FUNCTION, ONLY FOR TESTING ------------------

	public static void main(String[] args) {
		//
		// System.out.println("STARTED ...");
		//
		// final String configFileName = "./input/matsim-config.xml";
		// final Config config = ConfigUtils.loadConfig(configFileName);
		// config.getModule("plans").addParam("inputPlansFile",
		// "matsim-output/ITERS/it.100/100.plans.xml.gz");
		//
		// final Scenario scenario = ScenarioUtils.loadScenario(config);
		//
		// final String zonesShapeFileName = "./input/sverige_TZ_EPSG3857.shp";
		// final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		// zonalSystem.addNetwork(scenario.getNetwork(),
		// StockholmTransformationFactory.WGS84_SWEREF99);

		// final String eventsFileName =
		// "./matsim-output/ITERS/it.0/0.events.xml.gz";
		// final String regentMatrixFileName = "./exchange/regent-tts.xml";
		//
		// final int startTime_s = 0;
		// final int binSize_s = 3600;
		// final int binCnt = 24;
		// final int sampleCnt = 2;

		// final int ttCalcTimeBinSize = 15 * 60;
		// final int ttCalcEndTime = 24 * 3600 - 1; // one sec before midnight
		// final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
		// scenario.getNetwork(), ttCalcTimeBinSize, ttCalcEndTime,
		// scenario.getConfig().travelTimeCalculator());
		// Logger.getLogger(TourTravelTimes.class.getName()).info(
		// "number of time bins in matsim tt calc: "
		// + ttcalc.getNumSlots());
		// Logger.getLogger(TourTravelTimes.class.getName()).info(
		// "time bin size in matsim tt calc: " + ttcalc.getTimeSlice()
		// + " seconds");
		// final EventsManager events = EventsUtils.createEventsManager();
		// events.addHandler(ttcalc);
		// final MatsimEventsReader reader = new MatsimEventsReader(events);
		// reader.readFile(eventsFileName);
		// final TravelTime linkTTs = ttcalc.getLinkTravelTimes();

		// load matrices from file!

		// final String configFileName = "./input/matsim-config.xml";
		// final Config config = ConfigUtils.loadConfig(configFileName);
		// final Scenario scenario = ScenarioUtils.loadScenario(config);
		//
		// final Matrices ttMatrices = new Matrices();
		// final MatsimMatricesReader matricesReader = new MatsimMatricesReader(
		// ttMatrices, null);
		// matricesReader.readFile("./traveltimes.xml");
		//
		// final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
		// ttMatrices, 6 * 3600, 3600);
		//
		// final TourTravelTimes tsa = new TourTravelTimes(scenario,
		// travelTimeMatrices);
		// tsa.writeTourTravelTimesToFile("./tourtts.xml");
		// tsa.writeHistogramsToFile("./departuretime-hist.txt");
		//
		// System.out.println("... DONE");
	}
}
