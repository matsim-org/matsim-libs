package gunnar.ihop2.regent.costwriting;

import static gunnar.ihop2.regent.demandreading.PopulationCreator.HOME;
import static org.matsim.matrices.MatrixUtils.add;
import static org.matsim.matrices.MatrixUtils.mult;
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
import org.matsim.matrices.MatsimMatricesReader;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.math.Histogram;
import gunnar.ihop2.regent.demandreading.ZonalSystem;

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

	private final Map<String, Histogram> actType2dptTimeHist = new LinkedHashMap<>();

	private final Matrices tourTTMatrices = new Matrices();

	// ------------------ CONSTRUCTION ------------------

	public TourTravelTimes(final Scenario scenario,
			final TravelTimeMatrices travelTimeMatrices) {
		this.travelTimeMatrices = travelTimeMatrices;
		this.extractHistograms(scenario);
		this.computeTourTravelTimes();
	}

	// ------------------ INTERNALS ------------------

	private void addDepartureTime(final String actType, final double dptTime_s) {
		Histogram histogram = this.actType2dptTimeHist.get(actType);
		if (histogram == null) {
			histogram = Histogram.newHistogramWithUniformBins(
					this.travelTimeMatrices.getStartTime_s(),
					this.travelTimeMatrices.getBinSize_s(),
					this.travelTimeMatrices.getBinCnt());
			this.actType2dptTimeHist.put(actType, histogram);
		}
		histogram.add(dptTime_s);
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
							this.addDepartureTime(currentAct.getType(),
									prevDptTime_s);
						} else if (prevAct != null) {
							// Prev. trip was from activity location to home.
							this.addDepartureTime(prevAct.getType(),
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
		for (String actType : this.actType2dptTimeHist.keySet()) {
			Logger.getLogger(this.getClass().getName()).info(
					"Computing " + actType + " tour travel times.");
			final Matrix tourTT_min = this.tourTTMatrices.createMatrix(
					actType.toUpperCase(), "tour travel times [min]");
			double freqSum = 0.0;
			for (int ttMatrixBin = 0; ttMatrixBin < this.travelTimeMatrices
					.getBinCnt(); ttMatrixBin++) {
				final double freq = this.actType2dptTimeHist.get(actType).freq(
						ttMatrixBin + 1);
				add(tourTT_min,
						this.travelTimeMatrices.ttMatrixList_min
								.get(ttMatrixBin), freq);
				freqSum += freq;
			}
			Logger.getLogger(this.getClass().getName()).info(
					(freqSum * 100.0) + " percent of the population traveled "
							+ "during the analyzed time window");
			if (freqSum < 1e-3) {
				Logger.getLogger(this.getClass().getName()).warning(
						"less than one permill of the population traveled "
								+ "during the analyzed time window");
			}
			mult(tourTT_min, 1.0 / freqSum);
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
			for (Map.Entry<String, Histogram> act2histEntry : this.actType2dptTimeHist
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

		/*
		 * FIRST CREATE TRAVEL TIME MATRICES
		 */

		/*
		 * TODO:
		 */

		final String configFileName = "./input/matsim-config.xml";
		final Config config = ConfigUtils.loadConfig(configFileName);
		config.getModule("plans").addParam("inputPlansFile",
				"./../10percentCarNetworkPlain/1000.plans.xml.gz");

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final String zonesShapeFileName = "./input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		// final String eventsFileName =
		// "./matsim-output/ITERS/it.0/0.events.xml.gz";
		final String regentMatrixFileName = "./exchange/regent-tts.xml";

		final int startTime_s = 5 * 3600 + 1800;
		final int binSize_s = 3600;
		// final int binCnt = 2;
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

		// final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
		// scenario.getNetwork(), linkTTs, null, zonalSystem,
		// new Random(), startTime_s, binSize_s, binCnt, sampleCnt);

		final Matrices ttMatrices = new Matrices();
		final MatsimMatricesReader matricesReader = new MatsimMatricesReader(
				ttMatrices, null);
		matricesReader
				.readFile("./../10percentCarNetworkPlain/travelTimeMatrices.xml");
		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				ttMatrices, startTime_s, binSize_s);

		final TourTravelTimes tsa = new TourTravelTimes(scenario,
				travelTimeMatrices);
		tsa.writeHistogramsToFile("./../10percentCarNetworkPlain/departureTimeHistograms.xml");
		tsa.writeTourTravelTimesToFile("./../10percentCarNetworkPlain/tourTravelTimeMatrices.xml");

		System.out.println("... DONE");
	}

}
