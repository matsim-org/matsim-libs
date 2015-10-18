package gunnar.ihop2.regent.costwriting;

import static gunnar.ihop2.regent.demandreading.PopulationCreator.HOME;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.math.Histogram;
import gunnar.ihop2.regent.demandreading.ZonalSystem;

/**
 * TODO Untested, not yet running.
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

	private final Map<String, Histogram> actType2dptTimeHist = new LinkedHashMap<>();

	private final Matrices tourTTMatrices = new Matrices();

	// ------------------ CONSTRUCTION ------------------

	public TourTravelTimes(final Scenario scenario,
			final TravelTimeMatrices travelTimeMatrices,
			final String regentMatrixFileName) {
		this.extractHistograms(scenario, travelTimeMatrices);
		this.computeTourTravelTimes(travelTimeMatrices);
		this.writeToFile(regentMatrixFileName);
	}

	// ------------------ INTERNALS ------------------

	private void addDepartureTime(final String actType, final double dptTime_s,
			final TravelTimeMatrices travelTimeMatrices) {
		Histogram histogram = this.actType2dptTimeHist.get(actType);
		if (histogram == null) {
			histogram = Histogram.newHistogramWithUniformBins(
					travelTimeMatrices.getStartTime_s(),
					travelTimeMatrices.getBinSize_s(),
					travelTimeMatrices.getBinCnt());
			this.actType2dptTimeHist.put(actType, histogram);
		}
		histogram.add(dptTime_s);
	}

	private void extractHistograms(final Scenario scenario,
			final TravelTimeMatrices travelTimeMatrices) {

		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			if (plan == null) {
				Logger.getLogger(this.getClass().getName()).warning(
						"person " + person + " has no selected plan");
			} else {
				Activity previousActivity = null;
				Double previousDptTime = null;
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						final Activity currentActivity = (Activity) planElement;
						if (!HOME.equals(currentActivity.getType())) {
							/*
							 * Trip from home to activity location.
							 */
							this.addDepartureTime(currentActivity.getType(),
									previousDptTime, travelTimeMatrices);
						} else if (previousActivity != null) {
							/*
							 * Trip from activity location to home.
							 */
							this.addDepartureTime(previousActivity.getType(),
									previousDptTime, travelTimeMatrices);
						}
						previousActivity = currentActivity;
						previousDptTime = currentActivity.getEndTime();
					}
				}
			}
		}
		for (Map.Entry<String, Histogram> entry : this.actType2dptTimeHist
				.entrySet()) {
			Logger.getLogger(this.getClass().getName()).info(entry.getKey());
		}
	}

	private void computeTourTravelTimes(
			final TravelTimeMatrices travelTimeMatrices) {

		for (String actType : this.actType2dptTimeHist.keySet()) {
			Logger.getLogger(this.getClass().getName()).info(
					"Computing " + actType + " tour travel times.");
			final Histogram dptTimeHist = this.actType2dptTimeHist.get(actType);

			final Matrix tourTT = this.tourTTMatrices.createMatrix(
					actType.toUpperCase(), "tour travel times");

			for (int bin = 0; bin < travelTimeMatrices.getBinCnt(); bin++) {
				for (List<Entry> row : travelTimeMatrices.matrices.get(bin)
						.getFromLocations().values()) {
					for (Entry plainTTEntry : row) {
						Entry tourTTEntry = tourTT.getEntry(
								plainTTEntry.getFromLocation(),
								plainTTEntry.getToLocation());
						final double addend_s = dptTimeHist.freq(bin)
								* plainTTEntry.getValue();
						// System.out.println("freq = " + dptTimeHist.freq(bin)
						// + "; val = " + plainTTEntry.getValue());
						if (tourTTEntry != null) {
							tourTTEntry.setValue(tourTTEntry.getValue()
									+ addend_s);
						} else {
							tourTT.createEntry(plainTTEntry.getFromLocation(),
									plainTTEntry.getToLocation(), addend_s);
						}
					}
				}
			}
		}
	}

	private void writeToFile(final String regentMatrixFileName) {
		final MatricesWriter writer = new MatricesWriter(this.tourTTMatrices);
		writer.setIndentationString("  ");
		writer.setPrettyPrint(true);
		writer.write(regentMatrixFileName);
	}

	// ------------------ MAIN-FUNCTION, ONLY FOR TESTING ------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		/*
		 * FIRST CREATE TRAVEL TIME MATRICES
		 */

		final String configFileName = "./input/matsim-config.xml";
		final Config config = ConfigUtils.loadConfig(configFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final int ttCalcTimeBinSize = 15 * 60;
		final int ttCalcEndTime = 24 * 3600 - 1; // one sec before midnight
		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), ttCalcTimeBinSize, ttCalcEndTime,
				scenario.getConfig().travelTimeCalculator());
		Logger.getLogger(TourTravelTimes.class.getName()).info(
				"number of time bins in matsim tt calc: "
						+ ttcalc.getNumSlots());
		Logger.getLogger(TourTravelTimes.class.getName()).info(
				"time bin size in matsim tt calc: " + ttcalc.getTimeSlice()
						+ " seconds");

		final String linkAttributesFileName = "./input/link-attributes.xml";
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);
		final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
				ObjectAttributeUtils2.allObjectKeys(linkAttributes));

		final String zonesShapeFileName = "./input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final String eventsFileName = "./matsim-output/ITERS/it.0/0.events.xml.gz";
		final String regentMatrixFileName = "./exchange/regent-tts.xml";

		final int startTime_s = 6 * 3600 + 1800;
		final int binSize_s = 3600;
		final int binCnt = 1;
		final int sampleCnt = 1;

		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				scenario.getNetwork(), ttcalc, eventsFileName,
				// regentMatrixFileName,
				relevantLinkIDs, null, zonalSystem, new Random(), startTime_s,
				binSize_s, binCnt, sampleCnt);

		/*
		 * THEN AGGREGATE
		 */

		final TourTravelTimes tsa = new TourTravelTimes(scenario,
				travelTimeMatrices, regentMatrixFileName);

		System.out.println("... DONE");
	}

}
