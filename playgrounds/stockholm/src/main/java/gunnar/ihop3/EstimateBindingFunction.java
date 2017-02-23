package gunnar.ihop3;

import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.costwriting.HalfTourCostMatrices;
import gunnar.ihop2.regent.costwriting.LinkTravelDistanceInKilometers;
import gunnar.ihop2.regent.costwriting.LinkTravelTimeInMinutes;
import gunnar.ihop2.regent.costwriting.TripCostMatrices;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.utils.LexicographicallyOrderedPositiveNumberStrings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesReaderMatsimV1;
import org.matsim.vehicles.Vehicle;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class EstimateBindingFunction {

	private EstimateBindingFunction() {
	}

	public static void main(String[] args) {

		final boolean createMatrices = false;

		System.out.println("STARTED ...");

		final String zoneShapeFileName = "./ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final String networkFileName = "./ihop2-data/network-output/network.xml";
		final String populationFileName = "/Nobackup/Profilen/Documents/proposals/2015/IHOP2/"
				+ "showcase/2015-11-23ab_LARGE_RegentMATSim/2015-11-23a_No_Toll_large/"
				+ "summary/iteration-3/it.400/400.plans.xml.gz";
		final String eventsFileName = "/Nobackup/Profilen/Documents/proposals/2015/IHOP2/"
				+ "showcase/2015-11-23ab_LARGE_RegentMATSim/2015-11-23a_No_Toll_large/"
				+ "summary/iteration-3/it.400/400.events.xml.gz";

		final String traveltimesFileName = "./traveltimes.xml";
		final String distancesFileName = "./distances.xml";
		final String tollsFileName = "./tolls.xml";

		final int nodeSampleSize = 1;
		final int analysisStartTime_s = 0;
		final int analysisBinSize_s = 3600;
		final int analysisBinCnt = 24;

		// create matsim environment

		final Config config = ConfigUtils.createConfig();
		config.getModule("network").addParam("inputNetworkFile",
				networkFileName);
		config.getModule("plans")
				.addParam("inputPlansFile", populationFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// create case study environment

		final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		// load link travel times

		final EventsManager events = EventsUtils.createEventsManager();
		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), 15 * 60, 24 * 3600, scenario.getConfig()
						.travelTimeCalculator());
		events.addHandler(ttcalc);
		new MatsimEventsReader(events).readFile(eventsFileName);

		// >>>>>>>>>> OBTAIN TOUR COST MATRICES >>>>>>>>>>

		final Matrices timeMatrices;
		final Matrices distanceMatrices;

		if (createMatrices) {

			final Map<String, TravelDisutility> costType2travelDisutility = new LinkedHashMap<>();
			costType2travelDisutility.put(MATSimDummy.TRAVELTIME_COSTTYPE,
					new LinkTravelTimeInMinutes(ttcalc.getLinkTravelTimes()));
			costType2travelDisutility.put(MATSimDummy.DISTANCE_COSTTYPE,
					new LinkTravelDistanceInKilometers());

			// if (useToll) {
			// costType2travelDisutility.put(
			// TOLL_COSTTYPE,
			// new LinkTollCostInCrownes(matsimConfig.getModule(
			// "roadpricing").getValue("tollLinksFile")));
			// } else {
			costType2travelDisutility.put(MATSimDummy.TOLL_COSTTYPE,
					new TravelDisutility() {
						@Override
						public double getLinkTravelDisutility(Link link,
								double time, Person person, Vehicle vehicle) {
							return 0;
						}

						@Override
						public double getLinkMinimumTravelDisutility(Link link) {
							return 0;
						}
					});
			// }

			final Random rnd = MatsimRandom.getRandom();

			final TripCostMatrices tripCostMatrices = new TripCostMatrices(
					ttcalc.getLinkTravelTimes(),
					new OnlyTimeDependentTravelDisutility(ttcalc
							.getLinkTravelTimes()),
					scenario.getNetwork(), zonalSystem, analysisStartTime_s,
					analysisBinSize_s, analysisBinCnt, rnd, nodeSampleSize,
					costType2travelDisutility);
			tripCostMatrices.writeSummaryToFile("./travel-cost-statistics.txt");

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Computing tour travel times ...");

			final Map<String, String> costType2tourCostFileName = new LinkedHashMap<>();
			costType2tourCostFileName.put(MATSimDummy.TRAVELTIME_COSTTYPE,
					traveltimesFileName);
			costType2tourCostFileName.put(MATSimDummy.DISTANCE_COSTTYPE,
					distancesFileName);
			costType2tourCostFileName.put(MATSimDummy.TOLL_COSTTYPE,
					tollsFileName);

			// TODO ensure the population uses the experienced travel times.
			final HalfTourCostMatrices tourTravelTimes = new HalfTourCostMatrices(
					scenario, tripCostMatrices);
			tourTravelTimes
					.writeHalfTourTravelTimesToFiles(costType2tourCostFileName);
			tourTravelTimes
					.writeHistogramsToFile("./departure-time-histograms.txt");

			timeMatrices = tourTravelTimes
					.getHalfTourCostMatrices(MATSimDummy.TRAVELTIME_COSTTYPE);
			distanceMatrices = tourTravelTimes
					.getHalfTourCostMatrices(MATSimDummy.DISTANCE_COSTTYPE);

		} else { // LOAD MATRICES

			timeMatrices = new Matrices();
			MatricesReaderMatsimV1<?> reader = new MatricesReaderMatsimV1<>(
					timeMatrices);
			reader.readFile(traveltimesFileName);

			distanceMatrices = new Matrices();
			reader = new MatricesReaderMatsimV1<>(distanceMatrices);
			reader.readFile(distancesFileName);

		}

		// <<<<<<<<<< OBTAIN TOUR TRAVEL TIMES <<<<<<<<<<

		// >>>>>>>>>> ANALYSIS >>>>>>>>>>

		final OLSMultipleLinearRegression workTourRegr = new OLSMultipleLinearRegression();

		final LexicographicallyOrderedPositiveNumberStrings numberStrings = new LexicographicallyOrderedPositiveNumberStrings(
				analysisBinCnt - 1);

		// TODO CONTINUE HERE: WHAT EXPLAINS WHAT?
		final List<Double> workTourIndicators = new ArrayList<Double>();
		final List<Double> otherTourIndicators = new ArrayList<Double>();
		final List<Double> halfWorkTourTTs_min = new ArrayList<Double>();
		final List<Double> scores = new ArrayList<Double>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {

			System.out.println(person.getId() + " with score "
					+ person.getSelectedPlan().getScore());
			System.out.println();

			final double score = person.getSelectedPlan().getScore();

			double workTourIndicator = 0.0;
			double otherTourIndicator = 0.0;
			
			double halfWorkTourTT_min = 0.0;
			double halfWorkTourDist_km = 0.0;
			
			final Plan plan = person.getSelectedPlan();
			if (plan == null) {
				Logger.getLogger(EstimateBindingFunction.class.getName())
						.warning("person " + person + " has no selected plan");
			} else {
				Activity prevAct = null;
				Double prevDptTime_s = null;
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						final Activity currentAct = (Activity) planElement;
						if (!currentAct.getType().toUpperCase().startsWith("H")) {
							// Prev. trip was from home to activity location.

							final String fromZone = zonalSystem.getZone(
									scenario.getNetwork().getLinks()
											.get(currentAct.getLinkId())
											.getFromNode()).getId();
							final String toZone = zonalSystem.getZone(
									scenario.getNetwork().getLinks()
											.get(prevAct.getLinkId())
											.getFromNode()).getId();

							System.out.print(currentAct.getType() + "\t");
							System.out.print("dpt.time = " + prevDptTime_s
									+ "s\t");
							final double time_min = timeMatrices
									.getMatrix(
											currentAct.getType().toUpperCase())
									.getEntry(fromZone, toZone).getValue();
							System.out.print("half tour dur = " + time_min
									+ "min\t");
							final double dist_km = distanceMatrices
									.getMatrix(
											currentAct.getType().toUpperCase())
									.getEntry(fromZone, toZone).getValue();
							System.out.print("half tour length = " + dist_km
									+ "km\t");
							System.out.println();
							System.out.println();

							if (currentAct.getType().toUpperCase()
									.startsWith("W")) {
								workTourIndicator = 1.0;
								halfWorkTourTT_min = time_min;
								halfWorkTourDist_km = dist_km;
							}

						} else if (prevAct != null) {
							// Prev. trip was from activity location to home.
						}
						prevAct = currentAct;
						prevDptTime_s = currentAct.getEndTime();
					}
				}
			}

			// AND NOW HERE THE MODEL SHOULD BE ESTIMATED
			
			System.out.println("----------------------------------------");
			System.out.println();

		}

		System.out.println("... DONE");

	}

}
