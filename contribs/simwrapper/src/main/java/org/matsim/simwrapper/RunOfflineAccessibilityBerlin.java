package org.matsim.simwrapper;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.ApplicationUtils;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.simwrapper.dashboard.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class RunOfflineAccessibilityBerlin {
	private enum ageScenario{base, child, elderly};
	private enum economicStatusScenario{base, lowIncome, highIncome};

	//		static String OUTPUT_DIR = "../public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/policy";
	static String OUTPUT_DIR = "../public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/policy";

	private static final ageScenario age = ageScenario.base;
	private static final economicStatusScenario economicStatus = economicStatusScenario.base;

	private static final String crs = "EPSG:25832";
	private static final String poiFile = "../public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/poi/poi_bb.csv";

	private static final String shpFile = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/Berlin_25832.shp";

//		Coordinate leftBottomWgs84 = new Coordinate(12.745, 52.179);
//		Coordinate topRightWgs84 = new Coordinate(13.816, 52.805);


	public static double xMin;
	public static double yMin;
	public static double yMax;
	public static double xMax;



	public static void main(String[] args) throws FactoryException, TransformException {

		// CONFIGURATION
//		List<String> relevantPois = List.of("train_station", "supermarket");
//		List<String> relevantPois = List.of("ber_airport", "school");
		List<String> relevantPois = List.of("spa");


		AccessibilityConfigGroup accConfig = new AccessibilityConfigGroup();
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);


//		Coordinate leftBottomWgs84 = new Coordinate(13.31, 52.32);
//		Coordinate topRightWgs84 = new Coordinate(13.77, 52.53);
//		String mapCenterString = (leftBottomWgs84.x + topRightWgs84.x) / 2 + "," + (leftBottomWgs84.y + topRightWgs84.y) / 2;
//		Coordinate leftBottom = transformCoordinate(CRS.decode("EPSG:4326",true),CRS.decode(crs), leftBottomWgs84);
//		Coordinate rightTop = transformCoordinate(CRS.decode("EPSG:4326",true), CRS.decode(crs), topRightWgs84);
//		xMin = leftBottom.x;
//		yMin = leftBottom.y;
//		xMax = rightTop.x;
//		yMax = rightTop.y;
//		accConfig.setBoundingBoxLeft(xMin);
//		accConfig.setBoundingBoxBottom(yMin);
//		accConfig.setBoundingBoxRight(xMax);
//		accConfig.setBoundingBoxTop(yMax);
		accConfig.setShapeFileCellBasedAccessibility(shpFile);
		accConfig.setTileSize_m(5000);//250

		List<Double> timesHour = List.of(6.0);
//		List<Double> timesHour = List.of(0.0, 3.0, 6.0, 8.0);
//		List<Double> timesHour = List.of(6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0);
		List<Double> timesSeconds = timesHour.stream().map(t -> t * 60 * 60).toList();

		accConfig.setTimeOfDay(timesSeconds);
//		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.car, Modes4Accessibility.pt,Modes4Accessibility.walk, Modes4Accessibility.bike, Modes4Accessibility.teleportedWalk);
//		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.car, Modes4Accessibility.pt, Modes4Accessibility.teleportedWalk);
//		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.car, Modes4Accessibility.pt);
		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.car);

		for(Modes4Accessibility mode : Modes4Accessibility.values()) {
			accConfig.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}


		// Part 1: Calculate Accessibility

		step2_calculateAccessibility(null, relevantPois, accConfig);

		// Part 2: Create Dashboard
		step3_createDashboard(relevantPois, accModes, null); //mapCenterString


	}

	private static void step3_createDashboard(List<String> relevantPois, List<Modes4Accessibility> accModes, String mapCenterString) {

		final Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		//CONFIG
		config.controller().setLastIteration(0);
		config.controller().setWritePlansInterval(-1);
		config.controller().setWriteEventsInterval(-1);

		config.global().setCoordinateSystem(crs);

		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
//		group.sampleSize = 1.0;

		group.sampleSize = 0.01;
//		group.defaultParams().mapCenter = mapCenterString;
		group.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;


		String[] args = new String[]{"--xMin", String.valueOf(xMin), "--xMax", String.valueOf(xMax), "--yMin", String.valueOf(yMin), "--yMax", String.valueOf(yMax)};

		String coordinateSystem = config.global().getCoordinateSystem();
		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new OverviewDashboardHeart(relevantPois, coordinateSystem))
			.addDashboard(new AccessibilityBerlinDashboard(coordinateSystem, relevantPois, Modes4Accessibility.car))
			.addDashboard(new AccessibilityBerlinDashboard(coordinateSystem, relevantPois, Modes4Accessibility.pt))
			.addDashboard(new EquityBerlinDashboard(coordinateSystem, relevantPois));
//			.addDashboard(new NoiseDashboard(coordinateSystem, shpFile));


		boolean append = false;
		try {
			sw.generate(Path.of(OUTPUT_DIR), append);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		sw.run(Path.of(OUTPUT_DIR));




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
//		String stopsFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-land.xml";


		String configFile = ApplicationUtils.matchInput("output_config.xml", Path.of(OUTPUT_DIR)).toString();
		String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", Path.of(OUTPUT_DIR)).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml.gz", Path.of(OUTPUT_DIR)).toString();
		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml.gz", Path.of(OUTPUT_DIR)).toString();


		// CONFIG
		//global
		final Config configFromScenario = ConfigUtils.loadConfig(configFile);
		final Config config = ConfigUtils.createConfig();


		config.controller().setLastIteration(0);
//		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		config.routing().setRoutingRandomness(0.);

		config.global().setCoordinateSystem(crs);

		config.network().setInputFile(networkFile);


		// transit
		config.transit().setTransitScheduleFile(transportScheduleFile);

		// change walk speed to match kelheim scenario
		config.routing().getTeleportedModeParams().get(TransportMode.walk).setTeleportedModeSpeed(3.8 / 3.6);

		// change scoring default to match kelheim scenario
//		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
//		drtParams.setMarginalUtilityOfDistance(-2.5E-4);
//		drtParams.setMarginalUtilityOfTraveling(0.0);
//		config.scoring().addModeParams(drtParams);


		// scoring:

		for (ScoringConfigGroup.ModeParams modeParams : configFromScenario.scoring().getModes().values()) {
			config.scoring().addModeParams(modeParams);
		}

		ScoringConfigGroup.ModeParams walkParams = config.scoring().getModes().get(TransportMode.walk);
		ScoringConfigGroup.ModeParams carParams = config.scoring().getModes().get(TransportMode.car);
		ScoringConfigGroup.ModeParams ptParams = config.scoring().getModes().get(TransportMode.pt);

		walkParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(walkParams);

		// income dependence
		if(economicStatus == economicStatusScenario.highIncome){
			config.scoring().setMarginalUtilityOfMoney(config.scoring().getMarginalUtilityOfMoney() * 0.5);


//			carParams.getMarginalUtilityOfTraveling(); //see RandomizingTimeDistanceTravelDisutilityFactory.java
//			carParams.getMonetaryDistanceRate();

		} else if (economicStatus == economicStatusScenario.lowIncome) {
			config.scoring().setMarginalUtilityOfMoney(config.scoring().getMarginalUtilityOfMoney() * 2.0);
		}

		// age dependence

		if(age == ageScenario.child){
			carParams.setConstant(carParams.getConstant() - 3);
		} else if (age == ageScenario.elderly) {
			carParams.setConstant(carParams.getConstant() - 3);
			config.routing().getTeleportedModeParams().get(TransportMode.walk).setTeleportedModeSpeed(config.routing().getTeleportedModeParams().get(TransportMode.walk).getTeleportedModeSpeed() * 0.5);
//			config.routing().getTeleportedModeParams().getTeleportedModeParamset(TransportMode.non_network_walk	).setTeleportedModeSpeed(config.routing().getTeleportedModeParams().get(TransportMode.walk).getTeleportedModeSpeed() * 0.5);
		}



		// accessibility config
		config.addModule(accConfig);

		// drt config
//		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
//
//		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
//		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
//		drtConfigGroup.transitStopFile = stopsFile;
//
//		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 100000.;
//
//		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
//		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
//		config.addModule(multiModeDrtConfigGroup);
//		config.addModule(drtConfigGroup);

		// SCENARIO

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		for( Link link : scenario.getNetwork().getLinks().values() ){
			if(!link.getId().toString().startsWith("pt_")) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add(TransportMode.walk);
				modes.add(TransportMode.bike);
				link.setAllowedModes(modes);
			}

		}

		// add pois to scenario as facilities
		ActivityFacilities activityFacilities = scenario.getActivityFacilities();
		readPoiCsv(activityFacilities, poiFile);

		ActivityFacilitiesFactory af = activityFacilities.getFactory();
		ActivityFacility airport = af.createActivityFacility(Id.create("ber_airport1", ActivityFacility.class), new Coord(807227.8,5811201.7));

		ActivityOption ao = af.createActivityOption("ber_airport");
		airport.addActivityOption(ao);
		activityFacilities.addActivityFacility(airport);


		ActivityOption aoSpa = af.createActivityOption("spa");
		ActivityFacility vabali = af.createActivityFacility(Id.create("vabali", ActivityFacility.class), new Coord(795634.64,5828763.74));
		vabali.addActivityOption(aoSpa);
		activityFacilities.addActivityFacility(vabali);

		ActivityFacility liquidrom = af.createActivityFacility(Id.create("liquidrom", ActivityFacility.class), new Coord(797312.78, 5825825.94));
		liquidrom.addActivityOption(aoSpa);
		activityFacilities.addActivityFacility(liquidrom);



		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, relevantPois);

//		// configure DRT Estimator with wait time, and ride time parameters
//		DrtEstimator drtEstimator = new DirectTripDistanceBasedDrtEstimator.Builder()
//			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(estimatorParameters.getWaitTime()))
//			.setWaitingTimeDistributionGenerator(new NoDistribution())
//			.setRideDurationEstimator(new ConstantRideDurationEstimator(estimatorParameters.getSlope(), estimatorParameters.getIntercept()))
//			.setRideDurationDistributionGenerator(new NoDistribution())
//			.build();
//
//
//
//		builder.addDrtEstimator(drtEstimator);


		builder.build().run();

	}


	private static Coordinate transformCoordinate(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, Coordinate sourceCoordinate) throws TransformException, FactoryException {

		// Create transform
		boolean lenient = true;
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);

		// Create coordinate
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Point sourcePoint = geometryFactory.createPoint(sourceCoordinate);

		// Transform
		Point targetPoint = (Point) org.geotools.geometry.jts.JTS.transform(sourcePoint, transform);

		return targetPoint.getCoordinate();
	}

	private static void readPoiCsv(ActivityFacilities activityFacilities, String filePath) {

		ActivityFacilitiesFactory af = activityFacilities.getFactory();

		try (BufferedReader reader = getBufferedReader(filePath);
			 CSVParser parser = new CSVParser(reader,
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
			throw new RuntimeException("Failed to read CSV from " + filePath, e);
		}
	}

	private static BufferedReader getBufferedReader(String filePath) throws IOException {
		if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
			// It's a URL
			HttpURLConnection connection = (HttpURLConnection) new URL(filePath).openConnection();
			connection.setRequestMethod("GET");
			return new BufferedReader(new InputStreamReader(connection.getInputStream()));
		} else {
			// Assume it's a local file path
			return new BufferedReader(new FileReader(filePath));
		}
	}
}
