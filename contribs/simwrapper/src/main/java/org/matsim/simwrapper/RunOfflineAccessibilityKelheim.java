package org.matsim.simwrapper;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.geotools.api.referencing.FactoryException;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.ApplicationUtils;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripDistanceBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.simwrapper.dashboard.AccessibilityDashboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class RunOfflineAccessibilityKelheim {

	// todo: move entirely to matsim-kelheim
	// todo: where are the new output_files being produced, can we stop this?
	static String OUTPUT_DIR = "../public-svn/matsim/scenarios/countries/de/kelheim/kelheim-accessibility-dashboard/kexi-seed-1-ASC-2.45-plus100/";

	public static void main(String[] args) throws FactoryException {

		// CONFIGURATION
//		List<String> relevantPois = List.of("train_station", "supermarket");
		List<String> relevantPois = List.of("train_station");

		double mapCenterX = 721455;
		double mapCenterY = 5410601;
		double tileSize = 500;
		double num_rows = 50;



		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();
		accConfig.setBoundingBoxLeft(mapCenterX - num_rows * tileSize - tileSize / 2);
		accConfig.setBoundingBoxRight(mapCenterX + num_rows * tileSize + tileSize / 2);
		accConfig.setBoundingBoxBottom(mapCenterY - num_rows * tileSize - tileSize / 2);
		accConfig.setBoundingBoxTop(mapCenterY + num_rows * tileSize + tileSize / 2);
		accConfig.setTileSize_m((int) tileSize);
		if(true) {
			accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);

		} else{
			accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromFacilitiesObject);
			ActivityFacilities testFacs = FacilitiesUtils.createActivityFacilities("facilities");
			ActivityFacility testFac = testFacs.getFactory().createActivityFacility(Id.create("test", ActivityFacility.class), new Coord(mapCenterX - num_rows * tileSize, mapCenterY - num_rows * tileSize - tileSize / 2));
			testFacs.addActivityFacility(testFac);
			ActivityFacility testFac2 = testFacs.getFactory().createActivityFacility(Id.create("test2", ActivityFacility.class), new Coord(mapCenterX - num_rows * tileSize + tileSize, mapCenterY - num_rows * tileSize - tileSize / 2));
			testFacs.addActivityFacility(testFac2);
			ActivityFacility testFac3 = testFacs.getFactory().createActivityFacility(Id.create("test3", ActivityFacility.class), new Coord(mapCenterX - num_rows * tileSize + 2*  tileSize, mapCenterY - num_rows * tileSize - tileSize / 2));
			testFacs.addActivityFacility(testFac3);
			accConfig.setMeasuringPointsFacilities(testFacs);

		}

		List<Double> timesHour = List.of(6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0);
		List<Double> timesSeconds = timesHour.stream().map(t -> t * 60 * 60).toList();

		accConfig.setTimeOfDay(timesSeconds);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, false);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.walk, false);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.bike, false);


		// Part 1: Generate Parameters for Estimator

		EstimatorParameters estimatorParameters = step1_generateParams();

		// Part 2: Calculate Accessibility

		step2_calculateAccessibility(estimatorParameters, relevantPois, accConfig);

		// Part 3: Create Dashboard
		step3_createDashboard(relevantPois);


	}

	private static void step3_createDashboard(List<String> relevantPois) {

		final Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		//CONFIG
		config.controller().setLastIteration(0);
		config.controller().setWritePlansInterval(-1);
		config.controller().setWriteEventsInterval(-1);
		config.global().setCoordinateSystem("EPSG:25832");



		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.sampleSize = 0.001;
		group.defaultParams().mapCenter = "11.891000, 48.911000";
		group.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		// Scenario
//		Scenario scenario = ScenarioUtils.createScenario(config);

		SimWrapper sw = SimWrapper.create(config).addDashboard(new AccessibilityDashboard(config.global().getCoordinateSystem(), relevantPois));
		boolean append = true;
		try {
			sw.generate(Path.of(OUTPUT_DIR), append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sw.run(Path.of(OUTPUT_DIR));

//		Controller controller = ControllerUtils.createController(scenario);
//		controller.addOverridingModule(new SimWrapperModule(sw));
//		controller.run();


	}

	public static class EstimatorParameters {
		private final double intercept;
		private final double slope;
		private final double waitTime;

		public EstimatorParameters(double value1, double value2, double value3) {
			this.intercept = value1;
			this.slope = value2;
			this.waitTime = value3;
		}

		public double getIntercept() {
			return intercept;
		}

		public double getSlope() {
			return slope;
		}

		public double getWaitTime() {
			return waitTime;
		}
	}


	private static EstimatorParameters step1_generateParams() {

		DoubleList inVehicleTravelTime = new DoubleArrayList();
		DoubleList directTravelDistance_m = new DoubleArrayList();

		double n = 0.;
		double waitTimeSum = 0.;

		Path filePath = Path.of(OUTPUT_DIR + "kexi-seed1-ASC-2.45.output_drt_legs_drt.csv");
		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(Files.newInputStream(filePath))),
			CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				n++;
				waitTimeSum += Double.parseDouble(record.get("waitTime"));
				inVehicleTravelTime.add(Double.parseDouble(record.get("inVehicleTravelTime")));
				directTravelDistance_m.add(Double.parseDouble(record.get("directTravelDistance_m")));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		double waitTime_s = waitTimeSum / n;
		System.out.println("Wait time (seconds):" + waitTime_s);
		System.out.println("Wait time (minutes):" + waitTime_s/60);

		SimpleRegression regression = new SimpleRegression(true);

		for (int i = 0; i < n; i++) {
			regression.addData(directTravelDistance_m.getDouble(i), inVehicleTravelTime.getDouble(i));
		}

		// Get the intercept and slope
		double intercept = regression.getIntercept();
		double slope = regression.getSlope();


		System.out.println("Intercept: " + intercept);
		System.out.println("Slope: " + slope);
		System.out.println("R2: " + regression.getRSquare());

		return new EstimatorParameters(intercept, slope, waitTime_s); // Return the parameters as an object
	}



	private static void step2_calculateAccessibility(EstimatorParameters estimatorParameters, List<String> relevantPois, ConfigGroup accConfig) {

		// input files
		String stopsFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-land.xml";
		String poiFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/pois_complete.csv";

		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", Path.of(OUTPUT_DIR)).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml.gz", Path.of(OUTPUT_DIR)).toString();
		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml.gz", Path.of(OUTPUT_DIR)).toString();


		// CONFIG
		//global
		final Config config = ConfigUtils.createConfig();


		config.controller().setLastIteration(0);
//		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		config.routing().setRoutingRandomness(0.);

		config.global().setCoordinateSystem("EPSG:25832");

		config.network().setInputFile(networkFile);


		// transit
		config.transit().setTransitScheduleFile(transportScheduleFile);

		// change walk speed to match kelheim scenario
		config.routing().getTeleportedModeParams().get(TransportMode.walk).setTeleportedModeSpeed(3.8 / 3.6);

		// change scoring default to match kelheim scenario
		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
		drtParams.setMarginalUtilityOfDistance(-2.5E-4);
		drtParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(drtParams);

		ScoringConfigGroup.ModeParams walkParams = config.scoring().getModes().get(TransportMode.walk);
		walkParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(walkParams);

		// accessibility config

		config.addModule(accConfig);

		// drt config
		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
		drtConfigGroup.transitStopFile = stopsFile;

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 100000.;

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		// SCENARIO

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);


		// allow walk on all links for walk accessibility
//		for (Link link : scenario.getNetwork().getLinks().values()) {
//			HashSet<String> modes = new HashSet<>(link.getAllowedModes());
//			modes.add(TransportMode.walk);
//			modes.add(TransportMode.bike);
//			link.setAllowedModes(modes);
//
//		}

		// add pois to scenario as facilities
		readPoiCsv(scenario.getActivityFacilities(), poiFile);


		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, relevantPois);

		// configure DRT Estimator with wait time, and ride time parameters
		DrtEstimator drtEstimator = new DirectTripDistanceBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(estimatorParameters.getWaitTime()))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(estimatorParameters.getSlope(), estimatorParameters.getIntercept()))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();



		builder.addDrtEstimator(drtEstimator);

//		ConfigUtils.writeConfig(config, utils.getOutputDirectory() + "config.xml");

		builder.build().run();

	}

	private static void readPoiCsv(ActivityFacilities activityFacilities, String filePath) {

		ActivityFacilitiesFactory af = activityFacilities.getFactory();
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) new URL(filePath).openConnection();
			connection.setRequestMethod("GET");
		} catch (ProtocolException | MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(connection.getInputStream())),

			CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {

				String id = record.get("id");
				double x = Double.parseDouble(record.get("x"));
				double y = Double.parseDouble(record.get("y"));
				String type = record.get("type");
				ActivityFacility fac = af.createActivityFacility(Id.create(id, ActivityFacility.class), new Coord(x, y));
				ActivityOption ao = af.createActivityOption(type);
				fac.addActivityOption(ao);
				activityFacilities.addActivityFacility(fac);


			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
