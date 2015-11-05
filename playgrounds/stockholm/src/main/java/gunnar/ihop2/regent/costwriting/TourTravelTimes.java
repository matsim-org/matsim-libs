package gunnar.ihop2.regent.costwriting;

import static gunnar.ihop2.regent.demandreading.PopulationCreator.HOME;
import static org.matsim.matrices.MatrixUtils.add;
import static org.matsim.matrices.MatrixUtils.round;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatrixUtils;
import org.matsim.matrices.MatsimMatricesReader;

import floetteroed.utilities.math.Histogram;

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
public class TourTravelTimes {

	// ------------------ MEMBERS ------------------

	final TravelTimeMatrices travelTimeMatrices;

	private final Map<String, Histogram> actType2dptTimeHistTourStart = new LinkedHashMap<>();

	private final Map<String, Histogram> actType2dptTimeHistTourEnd = new LinkedHashMap<>();

	private final Matrices tourTTMatrices = new Matrices();

	// ------------------ CONSTRUCTION ------------------

	public TourTravelTimes(final Scenario scenario,
			final TravelTimeMatrices travelTimeMatrices) {
		this.travelTimeMatrices = travelTimeMatrices;
		this.extractHistograms(scenario);
		if (travelTimeMatrices != null) {
			this.computeTourTravelTimes();
		}
	}

	// ------------------ INTERNALS ------------------

	private void addDepartureTime(
			final Map<String, Histogram> actType2dptTimeHist,
			final String actType, final double dptTime_s) {
		Histogram histogram = actType2dptTimeHist.get(actType);
		if (histogram == null) {
			histogram = Histogram.newHistogramWithUniformBins(
					this.travelTimeMatrices.getStartTime_s(),
					this.travelTimeMatrices.getBinSize_s(),
					this.travelTimeMatrices.getBinCnt());
			actType2dptTimeHist.put(actType, histogram);
		}
		histogram.add(dptTime_s);
	}

	private void addDepartureTimeTourStart(final String actType,
			final double dptTime_s) {
		this.addDepartureTime(this.actType2dptTimeHistTourStart, actType,
				dptTime_s);
	}

	private void addDepartureTimeTourEnd(final String actType,
			final double dptTime_s) {
		this.addDepartureTime(this.actType2dptTimeHistTourEnd, actType,
				dptTime_s);
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
						if (!HOME.equals(currentAct.getType())) {
							// Prev. trip was from home to activity location.
							this.addDepartureTimeTourStart(
									currentAct.getType(), prevDptTime_s);
						} else if (prevAct != null) {
							// Prev. trip was from activity location to home.
							this.addDepartureTimeTourEnd(prevAct.getType(),
									prevDptTime_s);
						}
						prevAct = currentAct;
						prevDptTime_s = currentAct.getEndTime();
					}
				}
			}
		}
	}

	private void computeTourTravelTimes() {

		for (String actType : this.actType2dptTimeHistTourStart.keySet()) {

			/*
			 * Identify the total share of trips contained in the analysis
			 * period.
			 */

			double freqSumTourStart = 0.0;
			double freqSumTourEnd = 0.0;
			for (int ttMatrixBin = 0; ttMatrixBin < this.travelTimeMatrices
					.getBinCnt(); ttMatrixBin++) {
				freqSumTourStart += this.actType2dptTimeHistTourStart.get(
						actType).freq(ttMatrixBin + 1);
				freqSumTourEnd += this.actType2dptTimeHistTourEnd.get(actType)
						.freq(ttMatrixBin + 1);
			}
			Logger.getLogger(this.getClass().getName()).info(
					(freqSumTourStart * 100.0)
							+ " percent of the population traveled to "
							+ actType + " during the analyzed time window");
			Logger.getLogger(this.getClass().getName()).info(
					(freqSumTourEnd * 100.0)
							+ " percent of the population traveled back from "
							+ actType + " during the analyzed time window");

			/*
			 * Now compute the tour travel times as a weighted sum of trip
			 * travel times.
			 */

			Logger.getLogger(this.getClass().getName()).info(
					"Computing " + actType + " tour travel times.");
			final Matrix tourTT_min = this.tourTTMatrices.createMatrix(
					actType.toUpperCase(), "tour travel times [min]");
			for (int ttMatrixBin = 0; ttMatrixBin < this.travelTimeMatrices
					.getBinCnt(); ttMatrixBin++) {
				// contribution of trips to the activity
				add(tourTT_min,
						this.travelTimeMatrices.ttMatrixList_min
								.get(ttMatrixBin),
						this.actType2dptTimeHistTourStart.get(actType).freq(
								ttMatrixBin + 1)
								/ freqSumTourStart);
				// contribution of trips back from the activity
				add(tourTT_min,
						this.travelTimeMatrices.ttMatrixList_min
								.get(ttMatrixBin),
						this.actType2dptTimeHistTourEnd.get(actType).freq(
								ttMatrixBin + 1)
								/ freqSumTourEnd);
			}

			/*
			 * Dividing tour travel times by half, for compatibility with
			 * Regent's way of processing that data.
			 */

			Logger.getLogger(this.getClass().getName()).info(
					"Dividing *tour* travel time matrix for " + actType
							+ " by two in order to obtain "
							+ "*trip* travel times, as required by Regent.");
			MatrixUtils.mult(tourTT_min, 0.5);

			/*
			 * Finally, round this down to two digits in order to obtain
			 * somewhat manageable file sizes.
			 */

			round(tourTT_min, 2);
		}
	}

	public void writeTourTravelTimesToFile(final String regentMatrixFileName) {
		final MatricesWriter writer = new MatricesWriter(this.tourTTMatrices);
		writer.setIndentationString("  ");
		writer.setPrettyPrint(true);
		writer.write(regentMatrixFileName);
	}

	public void writeHistogramsToFile(final String histogramFileName) {
		try {
			final PrintWriter writer = new PrintWriter(histogramFileName);
			writer.println("Departure time histograms per activity type.");
			writer.println();
			for (Map.Entry<String, Histogram> act2histEntry : this.actType2dptTimeHistTourStart
					.entrySet()) {
				writer.println(act2histEntry.getKey());
				writer.println();
				writer.println(act2histEntry.getValue());
				writer.println();
				writer.println();
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			Logger.getLogger(this.getClass().getName()).warning(
					"could not write departure " + "time histograms to file "
							+ histogramFileName);
		}
	}

	// ------------------ MAIN-FUNCTION, ONLY FOR TESTING ------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

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

		final String configFileName = "./input/matsim-config.xml";
		final Config config = ConfigUtils.loadConfig(configFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Matrices ttMatrices = new Matrices();
		final MatsimMatricesReader matricesReader = new MatsimMatricesReader(
				ttMatrices, null);
		matricesReader.readFile("./traveltimes.xml");

		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				ttMatrices, 6 * 3600, 3600);

		final TourTravelTimes tsa = new TourTravelTimes(scenario,
				travelTimeMatrices);
		tsa.writeTourTravelTimesToFile("./tourtts.xml");
		tsa.writeHistogramsToFile("./departuretime-hist.txt");

		System.out.println("... DONE");
	}

}
