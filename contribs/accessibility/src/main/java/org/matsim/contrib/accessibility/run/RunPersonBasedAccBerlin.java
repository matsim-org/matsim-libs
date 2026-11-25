package org.matsim.contrib.accessibility.run;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contrib.accessibility.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "run-person-based-acc-berlin",
	description = "Compute person-based accessibility for one Berlin scenario, mode and opportunity source.")
public class RunPersonBasedAccBerlin implements Callable<Integer> {

	private static final String CRS = "EPSG:25832";

	private static final List<Modes4Accessibility> ANALYSIS_MODES = List.of(
		Modes4Accessibility.car,
		Modes4Accessibility.pt,
		Modes4Accessibility.teleportedWalk
	);

	private static final boolean PERSON_BASED = true;

	private Scenario scenario;

	private static final Logger LOG = LogManager.getLogger(RunPersonBasedAccBerlin.class);

//	Daily monetary constants as in the Berlin v6.4 scoring config (modeParams car/pt) [money/day].
//	Only used if --daily-monetary-constant is set; change the values here, not on the command line.
	private static final double CAR_DAILY_MONETARY_CONSTANT = -5.0;
	private static final double PT_DAILY_MONETARY_CONSTANT = -3.0;

	private static final List<String> FACILITY_ACTIVITY_TYPES = List.of(
		// "dining",
		// "edu_higher",
		// "edu_kiga",
		// "edu_other",
		// "edu_primary",
		// "edu_secondary",
		// "leisure",
		// "personal_business",
		// "shop_daily",
		// "shop_other",
		// "work_business",
		"work"
	);

	private enum RunScenario {base, policy}

	private enum OpportunityType {
		hospital("hospital"),
		spa("spa"),
		baeder("baeder"),
		jobcenter("jobcenter"),
		university("university"),
		citycenter("citycenter"),
		facilities("facilities");

		private final String activityType;

		OpportunityType(String activityType) {
			this.activityType = activityType;
		}

		String activityType() {
			return activityType;
		}

		String outputDir(String resultsDir) {
//			The R analysis directory is named "jobCenter" (capital C), unlike the enum constant.
			String directory = this == jobcenter ? "jobCenter" : name();
			return new File(resultsDir, directory).getPath();
		}
	}

	@Option(names = "--opportunity", required = true, converter = OpportunityTypeConverter.class,
		description = "Opportunity source: ${COMPLETION-CANDIDATES}.")
	private OpportunityType opportunity;

	@Option(names = "--activity-type", description = "Run a single activity type from the simulation facilities.")
	private String facilityActivityType;

	@Option(names = "--scenario", required = true, description = "Run to analyze: ${COMPLETION-CANDIDATES}.")
	private RunScenario runScenario;

	@Option(names = "--mode", required = true, description = "Transport mode: car, pt, or teleportedWalk.")
	private Modes4Accessibility mode;

	@Option(names = "--measure-type", defaultValue = "rawSum",
		description = "Accessibility measure: ${COMPLETION-CANDIDATES}. Default: ${DEFAULT-VALUE}.")
	private AccessibilityConfigGroup.AccessibilityMeasureType measureType;

	@Option(names = "--scenario-dir", required = true, description = "MATSim simulation output directory.")
	private String scenarioDir;

	@Option(names = "--output-dir", description = "Accessibility results directory. Defaults to analysis/accessibility under the scenario directory.")
	private String resultsDir;

	private String workDir;

	@Option(names = "--hospital-input-dir", description = "Directory containing the hospital WFS XML files.")
	private String hospitalInputDir;

	private String facilitiesFile;

	private String networkFile;

	private String plansFile;

	private String transitScheduleFile;

	private String eventsFile;

	@Option(names = "--plankrankenhaeuser-xml", description = "Override plankrankenhaeuser.xml.")
	private String plankrankenhaeuserXml;

	@Option(names = "--weitere-krankenhaeuser-xml", description = "Override weitere_krankenhaeuser.xml.")
	private String weitereKrankenhaeuserXml;

	@Option(names = "--baeder-geojson", description = "Override baeder.geojson.")
	private String baederGeoJson;

	@Option(names = "--sample", defaultValue = "1.0", description = "Fraction of loaded persons to keep, in (0, 1].")
	private double sample;

	@Option(names = "--daily-monetary-constant", description = "Include the daily monetary constant in car and PT costs.")
	private boolean includeDailyMonetaryConstant;

	@Spec
	private CommandSpec spec;

	public static void main(String[] args) {
		CommandLine command = new CommandLine(new RunPersonBasedAccBerlin());
		if (args.length == 0) {
			command.usage(System.out);
			return;
		}
		System.exit(command.execute(args));
	}

	private static final class OpportunityTypeConverter implements CommandLine.ITypeConverter<OpportunityType> {
		@Override
		public OpportunityType convert(String value) {
			return OpportunityType.valueOf(value.trim().toLowerCase(Locale.ROOT));
		}
	}

	@Override
	public Integer call() {
		if (!ANALYSIS_MODES.contains(mode)) {
			throw new CommandLine.ParameterException(spec.commandLine(), "Unsupported --mode. Choose one of " + ANALYSIS_MODES);
		}
		if (!(sample > 0 && sample <= 1)) {
			throw new CommandLine.ParameterException(spec.commandLine(), "--sample must be in (0, 1].");
		}

		resolvePaths();
		String combination = opportunity + "/" + runScenario + "/" + mode + "/" + measureType;


		LOG.info("=== Running {} ===", combination);
		long start = System.currentTimeMillis();

		if (facilityActivityType != null && !facilityActivityType.matches("[a-zA-Z0-9_]+")) {
			throw new CommandLine.ParameterException(spec.commandLine(), "Invalid --activity-type: " + facilityActivityType);
		}
		if (facilityActivityType != null && opportunity != OpportunityType.facilities) {
			throw new CommandLine.ParameterException(spec.commandLine(), "--activity-type requires --opportunity=facilities");
		}
		List<String> activityTypes = opportunity == OpportunityType.facilities
			? facilityActivityType == null ? FACILITY_ACTIVITY_TYPES : List.of(facilityActivityType)
			: List.of(opportunity.activityType());
		loadFiles(activityTypes);
		for (String activityType : activityTypes) {
			runActivityType(activityType);
		}
		LOG.info("Accessibility computation finished in {} seconds", (System.currentTimeMillis() - start) / 1000);
		return 0;
	}

	private void runActivityType(String activityType) {
		String outputDir = opportunity == OpportunityType.facilities
			? file(resultsDir, activityType) : opportunity.outputDir(resultsDir);
		writeLocationsCSV(opportunity, activityType, file(outputDir, "locations.csv"));

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(
			scenario, eventsFile, List.of(activityType));

		PersonBasedResultsComparator dataListener = new PersonBasedResultsComparator();
		builder.addDataListener(dataListener);
		builder.build().run();

		Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap =
			dataListener.getAccessibilitiesMap();
		writeCSV(accessibilitiesMap, new File(outputDir,
			"pbAcc_" + mode + "_" + runScenario + "_" + measureType + ".csv").getPath());
		writePersonsCSV(accessibilitiesMap, new File(outputDir,
			"persons_" + mode + "_" + runScenario + ".csv").getPath());
	}

	private void resolvePaths() {
		if (resultsDir == null) {
			resultsDir = file(scenarioDir, "analysis/accessibility");
		}
		if (hospitalInputDir == null) {
			hospitalInputDir = file(scenarioDir, "input");
		}

		// The accessibility engine requires a controller output directory for its scratch files.
		workDir = Path.of(System.getProperty("java.io.tmpdir"), "matsim-acc",
			runScenario + "-" + opportunity + "-" + mode + "-" + ProcessHandle.current().pid()).toString();

		String prefix = simulationOutputPrefix(Path.of(scenarioDir));
		eventsFile = matsimFile(scenarioDir, prefix + "output_events.xml");
		networkFile = matsimFile(scenarioDir, prefix + "output_network.xml");
		plansFile = matsimFile(scenarioDir, prefix + "output_plans.xml");
		transitScheduleFile = matsimFile(scenarioDir, prefix + "output_transitSchedule.xml");
		if (opportunity == OpportunityType.facilities) {
			facilitiesFile = matsimFile(scenarioDir, prefix + "output_facilities.xml");
		}

		plankrankenhaeuserXml = orDefault(plankrankenhaeuserXml,
			file(hospitalInputDir, "plankrankenhaeuser.xml"));
		weitereKrankenhaeuserXml = orDefault(weitereKrankenhaeuserXml,
			file(hospitalInputDir, "weitere_krankenhaeuser.xml"));
		baederGeoJson = orDefault(baederGeoJson,
			file(scenarioDir, "baeder.geojson"));
	}

	private String simulationOutputPrefix(Path directory) {
		Pattern eventsName = Pattern.compile("((?:.+\\.)?)output_events\\.xml(?:\\.(?:zst|gz))?");
		List<String> prefixes;
		try (var files = Files.list(directory)) {
			prefixes = files.filter(Files::isRegularFile)
				.map(path -> eventsName.matcher(path.getFileName().toString()))
				.filter(Matcher::matches)
				.map(match -> match.group(1))
				.distinct()
				.sorted()
				.toList();
		} catch (IOException e) {
			throw new CommandLine.ParameterException(spec.commandLine(),
				"Cannot read simulation output directory " + directory + ": " + e.getMessage());
		}
		if (prefixes.isEmpty()) {
			throw new CommandLine.ParameterException(spec.commandLine(), "No output_events.xml found in " + directory);
		}
		if (prefixes.size() > 1) {
			throw new CommandLine.ParameterException(spec.commandLine(),
				"Multiple MATSim runs found in " + directory + ". Use a directory containing one run.");
		}
		return prefixes.getFirst();
	}

	private String matsimFile(String dir, String baseName) {
		for (String ending : List.of(".zst", ".gz", "")) {
			File candidate = new File(dir, baseName + ending);
			if (candidate.isFile()) {
				return candidate.getPath();
			}
		}
		throw new CommandLine.ParameterException(spec.commandLine(),
			"Missing " + baseName + " (.zst, .gz, or plain XML) in " + dir);
	}

	private static String file(String parent, String child) {
		return new File(parent, child).getPath();
	}

	private static String orDefault(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}

	private void loadFiles(List<String> activityTypes) {
//		== Create config ==
		final Config config = ConfigUtils.createConfig();

//		== Adjust config ==
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(workDir);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(CRS);
		config.routing().setRoutingRandomness(0.);

		// add monetary distance rate for car (Berlin v6.4 scoring config, modeParams car)
		config.scoring().getModes().get(TransportMode.car).setMonetaryDistanceRate(-1.49E-4);

		// daily monetary constants (fixed monetary costs per day); only used by the car and pt
		// calculators if includeDailyMonetaryConstant is switched on (--daily-monetary-constant)
		config.scoring().getModes().get(TransportMode.car).setDailyMonetaryConstant(CAR_DAILY_MONETARY_CONSTANT);
		config.scoring().getModes().get(TransportMode.pt).setDailyMonetaryConstant(PT_DAILY_MONETARY_CONSTANT);

		// mode constants (ASC) as in the Berlin v6.4 scoring config (modeParams car/pt).
		// Used by the car and pt calculators via AccessibilityUtils.getModeSpecificConstantForAccessibilities.
		config.scoring().getModes().get(TransportMode.car).setConstant(-0.487593225702316);
		config.scoring().getModes().get(TransportMode.pt).setConstant(0.43220302370275404);

		config.network().setInputFile(networkFile);
		if (opportunity == OpportunityType.facilities) {
			config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
			config.facilities().setInputFile(facilitiesFile);
		} else {
			config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);
		}
		config.plans().setInputFile(plansFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);

//		== Accessibility config ==
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setPersonBased(PERSON_BASED);
		acg.setIncludeDailyMonetaryConstant(includeDailyMonetaryConstant);
		LOG.info("Daily monetary constant included: {} (car {}, pt {})", includeDailyMonetaryConstant,
			CAR_DAILY_MONETARY_CONSTANT, PT_DAILY_MONETARY_CONSTANT);
		acg.setAccessibilityMeasureType(measureType);
		acg.setTimeOfDay(8 * 60 * 60.); // does it run until the end of scenario from start time?
		acg.setTileSize_m(1000);
		acg.setAreaOfAccessibilityComputation(
			AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromPopulation);
//		Skip the Voronoi measure-point geometries. They are only needed for additional facility
//		data and Geoserver/QGIS output, neither of which is used here, and building them compares
//		every person with every other person (hours for a 10% population). Results are unchanged.
		acg.setMeasurePointGeometryProvision(
			AccessibilityConfigGroup.MeasurePointGeometryProvision.fromShapeFile);

//		== Mode selection ==
		List<Modes4Accessibility> accModes = List.of(mode);
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			acg.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}

//		== Load scenario ==
		scenario = ScenarioUtils.loadScenario(config);

//		== Optional downsampling of the population ==
		int sampleSeed = 4711;
		if (sample < 1.0) {
//			sampleDown draws from MatsimRandom; resetting it makes the selection reproducible.
			MatsimRandom.reset(sampleSeed);
			PopulationUtils.sampleDown(scenario.getPopulation(), sample);
			LOG.info("Downsampled population to {} (seed {}): {} persons",
				sample, sampleSeed, scenario.getPopulation().getPersons().size());
		}

//		== Adding facilities ==
		ActivityFacilities activityFacilities = scenario.getActivityFacilities();
		if (opportunity == OpportunityType.facilities) {
			for (String activityType : activityTypes) {
				if (activityFacilities.getFacilitiesForActivityType(activityType).isEmpty()) {
					throw new IllegalArgumentException("No facilities for activity type " + activityType
						+ " in " + facilitiesFile);
				}
			}
		} else {
			ActivityFacilitiesFactory af = activityFacilities.getFactory();

			ActivityOption activityOption = af.createActivityOption(opportunity.activityType());
			switch (opportunity) {
				case hospital -> addHospitals(activityFacilities, af, activityOption);
				case spa -> addSpas(activityFacilities, af, activityOption);
				case baeder -> addGeoJsonOpportunities(activityFacilities, af, activityOption,
					baederGeoJson, location -> true);
				case jobcenter -> addJobcenters(activityFacilities, af, activityOption);
				case university -> addUniversities(activityFacilities, af, activityOption);
				case citycenter -> addCityCenter(activityFacilities, af, activityOption);
			}
		}

//		== Assigning home coords for each person ==
		int[] kept = {0};
		int[] skipped = {0};
		scenario.getPopulation().getPersons().values().removeIf(person -> {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				LOG.warn("Skipping {} (no plan)", person.getId());
				skipped[0]++;
				return true;
			}

			// Find first home activity in the plan
			Activity home = selectedPlan.getPlanElements().stream()
				.filter(pe -> pe instanceof Activity)
				.map(pe -> (Activity) pe)
				.filter(activity -> activity.getType().startsWith("home"))
				.findFirst()
				.orElse(null);

			if (home == null || home.getCoord() == null) {
				LOG.warn("Skipping {} (no home activity/coord)", person.getId());
				skipped[0]++;
				return true;
			}

			// Store as attributes so AccessibilityModule can find them
			person.getAttributes().putAttribute("homeX", home.getCoord().getX());
			person.getAttributes().putAttribute("homeY", home.getCoord().getY());

			kept[0]++;
			return false;
		});
		LOG.info("Sanity check: {} persons kept with valid home coords, {} skipped", kept[0], skipped[0]);
		LOG.info("Final population size after filtering: {}", scenario.getPopulation().getPersons().size());

//		== Bounding box from persons home coords ==
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Double x = (Double) person.getAttributes().getAttribute("homeX");
			Double y = (Double) person.getAttributes().getAttribute("homeY");
			if (x != null && y != null) {
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		acg.setBoundingBoxBottom(minY).setBoundingBoxTop(maxY)
			.setBoundingBoxLeft(minX).setBoundingBoxRight(maxX);
		LOG.info("Bounding box set to: minX={}, maxX={}, minY={}, maxY={}", minX, maxX, minY, maxY);
	}

//	Kept for the spa-based reference run.
	private static void addSpas(
		ActivityFacilities facilities,
		ActivityFacilitiesFactory factory,
		ActivityOption activityOption) {
		for (Spa spa : SPAS) {
			ActivityFacility facility = factory.createActivityFacility(
				Id.create(spa.name(), ActivityFacility.class),
				new Coord(spa.x(), spa.y())
			);
			facility.addActivityOption(activityOption);
			facilities.addActivityFacility(facility);
		}
		LOG.info("Added {} spas as accessibility opportunities", SPAS.size());
	}

//	Job centers are a fixed list of sites, handled exactly like the spas.
	private static void addJobcenters(
		ActivityFacilities facilities,
		ActivityFacilitiesFactory factory,
		ActivityOption activityOption) {
		for (Jobcenter jobcenter : JOBCENTERS) {
			ActivityFacility facility = factory.createActivityFacility(
				Id.create(jobcenter.name(), ActivityFacility.class),
				new Coord(jobcenter.x(), jobcenter.y())
			);
			facility.addActivityOption(activityOption);
			facilities.addActivityFacility(facility);
		}
		LOG.info("Added {} job centers as accessibility opportunities", JOBCENTERS.size());
	}

//	Universities are a fixed list of sites, handled exactly like the job centers.
	private static void addUniversities(
		ActivityFacilities facilities,
		ActivityFacilitiesFactory factory,
		ActivityOption activityOption) {
		for (University university : UNIVERSITIES) {
			ActivityFacility facility = factory.createActivityFacility(
				Id.create(university.name(), ActivityFacility.class),
				new Coord(university.x(), university.y())
			);
			facility.addActivityOption(activityOption);
			facilities.addActivityFacility(facility);
		}
		LOG.info("Added {} universities as accessibility opportunities", UNIVERSITIES.size());
	}

//	The city center is a single fixed point, handled like the universities.
	private static void addCityCenter(
		ActivityFacilities facilities,
		ActivityFacilitiesFactory factory,
		ActivityOption activityOption) {
		ActivityFacility facility = factory.createActivityFacility(
			Id.create(CITY_CENTER.name(), ActivityFacility.class),
			new Coord(CITY_CENTER.x(), CITY_CENTER.y())
		);
		facility.addActivityOption(activityOption);
		facilities.addActivityFacility(facility);
		LOG.info("Added the city center ({}) as accessibility opportunity", CITY_CENTER.name());
	}

	private static class PersonBasedResultsComparator implements PersonDataExchangeInterface {
		private final Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap = new HashMap<>();

		@Override
		public synchronized void setPersonAccessibilities(Person person, Double timeOfDay, String mode, double accessibility) {
			Tuple<Person, Double> key = new Tuple<>(person, timeOfDay);
			/*FIX: using computeIfAbsent to ensure inner map (mode → accessibility values)
			always exists for each (person, time) tuple. without this, accessibilitiesMap.get(key)
			could return null, causing NullPointerException when calling .put(mode, accessibility).*/
			accessibilitiesMap.computeIfAbsent(key, k -> new HashMap<>());
			accessibilitiesMap.get(key).put(mode, accessibility);
		}

		public Map<Tuple<Person, Double>, Map<String, Double>> getAccessibilitiesMap() {
			return accessibilitiesMap;
		}

		@Override
		public void finish() {

		}
	}

	private static void writeCSV(Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap, String csvFile) {
		new File(csvFile).getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(csvFile)) {
			// Write header
			writer.write("personId,age,sex,income,carAvail,restricted_mobility,homeX,homeY,accessibility\n");//removed time & mode

			// Write data
			for (var entry : accessibilitiesMap.entrySet()) {
				Person person = entry.getKey().getFirst();
				Map<String, Double> modeAccs = entry.getValue();

				// Get attributes
				Object age = person.getAttributes().getAttribute("age");
				Object sex = person.getAttributes().getAttribute("sex");
				Object income = person.getAttributes().getAttribute("income");
				Object carAvail = person.getAttributes().getAttribute("carAvail");
				Object restricted = person.getAttributes().getAttribute("restricted_mobility");
				Object homeX = person.getAttributes().getAttribute("homeX");
				Object homeY = person.getAttributes().getAttribute("homeY");

				for (Map.Entry<String, Double> modeAcc : modeAccs.entrySet()) {
					writer.write(person.getId() + ","
						+ (age != null ? age : "") + ","
						+ (sex != null ? sex : "") + ","
						+ (income != null ? income : "") + ","
						+ (carAvail != null ? carAvail : "") + ","
						+ (restricted != null ? restricted : "") + ","
						+ (homeX != null ? homeX : "") + ","
						+ (homeY != null ? homeY : "") + ","
						+ modeAcc.getValue() + "\n");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error writing CSV", e);
		}
		LOG.info("Custom CSV written to {}", csvFile);
	}

	/**
	 * Writes one row per person and mode with their home coordinates and accessibility (EPSG:25832).
	 * The file name encodes the mode and scenario (base/policy), since each run computes exactly one of each.
	 */
	private static void writePersonsCSV(Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap,
	                                    String csvFile) {
		new File(csvFile).getParentFile().mkdirs();
		int rows = 0;
		try (FileWriter writer = new FileWriter(csvFile)) {
			writer.write("personId,homeX,homeY,accessibility\n");

			for (var entry : accessibilitiesMap.entrySet()) {
				Person person = entry.getKey().getFirst();
				Object homeX = person.getAttributes().getAttribute("homeX");
				Object homeY = person.getAttributes().getAttribute("homeY");

				for (Map.Entry<String, Double> modeAcc : entry.getValue().entrySet()) {
					writer.write(person.getId() + ","
						+ (homeX != null ? homeX : "") + ","
						+ (homeY != null ? homeY : "") + ","
						+ modeAcc.getValue() + "\n");
					rows++;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error writing persons CSV", e);
		}
		LOG.info("Persons CSV ({} rows) written to {}", rows, csvFile);
	}

	/**
	 * Writes the locations of the selected opportunity type (EPSG:25832) to a CSV for mapping in R.
	 * One row per location, with a {@code type} column to distinguish the opportunity source.
	 */
	private void writeLocationsCSV(OpportunityType opportunity, String activityType, String csvFile) {
		File target = new File(csvFile);
		target.getParentFile().mkdirs();
//		Parallel cluster jobs share locations.csv per opportunity; write a private
//		temp file and move it into place so readers never see a partial file.
		File temp = new File(target.getParentFile(),
			target.getName() + ".tmp." + ProcessHandle.current().pid());
		int rows = 0;
		try (FileWriter writer = new FileWriter(temp)) {
			writer.write("type,id,name,x,y,district,beds,street,house_number,postal_code,gisid,source_layer\n");

			switch (opportunity) {
				case facilities -> {
					for (ActivityFacility facility : scenario.getActivityFacilities()
							.getFacilitiesForActivityType(activityType).values()) {
						writer.write(String.join(",", csv(activityType), csv(facility.getId().toString()),
							"", Double.toString(facility.getCoord().getX()), Double.toString(facility.getCoord().getY()),
							"", "", "", "", "", "", "") + "\n");
						rows++;
					}
				}
				case spa -> {
					for (Spa spa : SPAS) {
						writer.write(String.join(",",
							csv("spa"),
							csv(spa.name()),
							csv(spa.name()),
							Double.toString(spa.x()),
							Double.toString(spa.y()),
							csv(spa.district()),
							"", "", "", "", "", ""
						) + "\n");
						rows++;
					}
				}
				case hospital -> {
					for (Hospital hospital : hospitalLocations) {
						Map<String, String> attributes = hospital.attributes();
						writer.write(String.join(",",
							csv("hospital"),
							csv(hospital.id()),
							csv(hospital.name()),
							Double.toString(hospital.x()),
							Double.toString(hospital.y()),
							csv(attributes.getOrDefault("district", "")),
							csv(attributes.getOrDefault("beds", "")),
							csv(attributes.getOrDefault("street", "")),
							csv(attributes.getOrDefault("house_number", "")),
							csv(attributes.getOrDefault("postal_code", "")),
							csv(attributes.getOrDefault("gisid", "")),
							csv(attributes.getOrDefault("source_layer", ""))
						) + "\n");
						rows++;
					}
				}
				case jobcenter -> {
					for (Jobcenter jobcenter : JOBCENTERS) {
						writer.write(String.join(",",
							csv("jobcenter"),
							csv(jobcenter.name()),
							csv(jobcenter.name()),
							Double.toString(jobcenter.x()),
							Double.toString(jobcenter.y()),
							csv(jobcenter.district()),
							"", "", "", "", "", ""
						) + "\n");
						rows++;
					}
				}
				case university -> {
					for (University university : UNIVERSITIES) {
						writer.write(String.join(",",
							csv("university"),
							csv(university.name()),
							csv(university.name()),
							Double.toString(university.x()),
							Double.toString(university.y()),
							csv(university.district()),
							"", "", "", "", "", ""
						) + "\n");
						rows++;
					}
				}
				case citycenter -> {
					writer.write(String.join(",",
						csv("citycenter"),
						csv(CITY_CENTER.name()),
						csv("Unter den Linden / Friedrichstraße"),
						Double.toString(CITY_CENTER.x()),
						Double.toString(CITY_CENTER.y()),
						csv(CITY_CENTER.district()),
						"", "", "", "", "", ""
					) + "\n");
					rows++;
				}
				case baeder -> {
					for (GeoJsonOpportunity location : geoJsonLocations) {
						Map<String, String> attributes = location.attributes();
						writer.write(String.join(",",
							csv(opportunity.activityType()),
							csv(location.id()),
							csv(location.name()),
							Double.toString(location.x()),
							Double.toString(location.y()),
							csv(firstNonEmpty(attributes.get("bezirk"), attributes.get("district"))),
							csv(firstNonEmpty(attributes.get("betten"), attributes.get("beds"))),
							csv(firstNonEmpty(attributes.get("adresse"), attributes.get("street"))),
							csv(firstNonEmpty(attributes.get("hausnummer"), attributes.get("house_number"))),
							csv(firstNonEmpty(attributes.get("postleitzahl"), attributes.get("postal_code"))),
							csv(firstNonEmpty(attributes.get("gml_id"), attributes.get("gisid"))),
							csv(firstNonEmpty(attributes.get("source_layer"), opportunity.activityType()))
						) + "\n");
						rows++;
					}
				}
			}
		} catch (IOException e) {
			temp.delete();
			throw new RuntimeException("Error writing locations CSV", e);
		}

		try {
			Files.move(temp.toPath(), target.toPath(),
				StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			try {
				Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException moveError) {
				temp.delete();
				throw new RuntimeException("Error moving locations CSV into place", moveError);
			}
		} catch (IOException e) {
			temp.delete();
			throw new RuntimeException("Error moving locations CSV into place", e);
		}

		LOG.info("Wrote {} {} locations to {}", rows, activityType, csvFile);
	}

	private static String csv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private record Spa(String name, double x, double y, String district) {}

	private static final List<Spa> SPAS = List.of(
		new Spa("vabali", 795634.64, 5828763.74, "001_Mitte"),
		new Spa("liquidrom", 797312.78, 5825825.94, "002_Friedrichshain-Kreuzberg"),
		new Spa("meridian", 784623.35, 5828694.12, "005_Spandau"),
		new Spa("aspria", 791428.73, 5825366.54, "004_Charlottenburg-Wilmersdorf"),
		new Spa("hamam", 800182.05, 5825953.76, "002_Friedrichshain-Kreuzberg"),
		new Spa("saunabad", 800320.0, 5829910.0, "003_Pankow"),
		new Spa("kristall", 786190.0, 5791400.0, "NA (Ludwigsfelde)")
	);

	private record Jobcenter(String name, double x, double y, String district) {}

//	One site per Berlin job center (one per district), EPSG:25832.
//	Coordinates: Google Maps lookup of the address on 2026-11-16, converted from WGS84.
	private static final List<Jobcenter> JOBCENTERS = List.of(
		new Jobcenter("mitte", 798698.44, 5826935.14, "001_Mitte"),                                    // Seydelstraße 2-5, 10117
		new Jobcenter("friedrichshain-kreuzberg", 801337.60, 5828666.40, "002_Friedrichshain-Kreuzberg"), // Landsberger Allee 50/52, 10249
		new Jobcenter("pankow", 802068.37, 5829842.42, "003_Pankow"),                                  // Storkower Str. 133, 10407
		new Jobcenter("charlottenburg-wilmersdorf", 792616.09, 5828353.94, "004_Charlottenburg-Wilmersdorf"), // Goslarer Ufer 37, 10589
		new Jobcenter("spandau", 783921.00, 5828572.10, "005_Spandau"),                                // Altonaer Str. 70/72, 13581
		new Jobcenter("steglitz-zehlendorf", 793563.59, 5820041.03, "006_Steglitz-Zehlendorf"),        // Birkbuschstraße 10, 12167
		new Jobcenter("tempelhof-schoeneberg", 797488.57, 5820537.45, "007_Tempelhof-Schöneberg"),     // Wolframstraße 89-92, 12105
		new Jobcenter("neukoelln", 800647.83, 5823553.86, "008_Neukölln"),                             // Mainzer Str. 27, 12053
		new Jobcenter("treptow-koepenick", 806846.77, 5820236.07, "009_Treptow-Köpenick"),             // Groß-Berliner Damm 73 A-E, 12487
		new Jobcenter("marzahn-hellersdorf", 807063.57, 5829242.13, "010_Marzahn-Hellersdorf"),        // Allee der Kosmonauten 29, 12681
		new Jobcenter("lichtenberg", 804448.81, 5828153.84, "011_Lichtenberg"),                        // Gotlindestraße 93, 10365
		new Jobcenter("reinickendorf", 791900.76, 5834857.02, "012_Reinickendorf")                     // Miraustraße 54, 13509
	);

	private record University(String name, double x, double y, String district) {}

//	One site per university (main building / main campus), EPSG:25832.
//	Coordinates: WGS84 of the main address (looked up 2026-11-19, accurate to ~100 m), converted with pyproj.
	private static final List<University> UNIVERSITIES = List.of(
		new University("fu", 791161.51, 5819555.71, "006_Steglitz-Zehlendorf"),      // FU, Kaiserswerther Str. 16-18, 14195
		new University("hu", 798034.45, 5827706.95, "001_Mitte"),                    // HU, Unter den Linden 6, 10117
		new University("tu", 793535.69, 5826810.07, "004_Charlottenburg-Wilmersdorf"), // TU, Straße des 17. Juni 135, 10623
		new University("udk", 793650.65, 5826482.32, "004_Charlottenburg-Wilmersdorf"), // UdK, Hardenbergstraße 33, 10623
		new University("charite", 796973.18, 5828277.46, "001_Mitte")               // Charité Campus Mitte, Charitéplatz 1, 10117
	);

//	City center: corner of Unter den Linden and Friedrichstraße, EPSG:25832.
//	From WGS84 52.517083527727955, 13.388832256388733 (given 2026-11-25), converted with pyproj.
	private static final University CITY_CENTER =
		new University("unter-den-linden-friedrichstrasse", 797736.37, 5827608.85, "001_Mitte");

	private record Hospital(String id, String name, double x, double y,
	                        Map<String, String> attributes) {}

	private List<Hospital> hospitalLocations = new ArrayList<>();

	private record GeoJsonOpportunity(String id, String name, double x, double y,
	                                  Map<String, String> attributes) {}

	private List<GeoJsonOpportunity> geoJsonLocations = new ArrayList<>();

	private static final String WFS_NS = "http://www.opengis.net/wfs/2.0";
	private static final String GML_NS = "http://www.opengis.net/gml/3.2";

	private static List<Hospital> parseWfsFile(String path, String idPrefix,
	                                           String nameField, String bedsField) {
		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			document = factory.newDocumentBuilder().parse(new File(path));
		} catch (Exception e) {
			throw new RuntimeException("Could not read hospital WFS file '" + path
				+ "'. Download it from https://gdi.berlin.de/services/wfs/krankenhaeuser "
				+ "(GetFeature, typeNames + srsName=EPSG:25832).", e);
		}

		NodeList members = document.getElementsByTagNameNS(WFS_NS, "member");
		List<Hospital> hospitals = new ArrayList<>(members.getLength());

		for (int i = 0; i < members.getLength(); i++) {
			Element member = (Element) members.item(i);
			Element feature = firstChildElement(member);
			if (feature == null) {
				continue;
			}

			String gisId = textOfFirstChild(feature, "gisid");
			String name = textOfFirstChild(feature, nameField);
			String beds = textOfFirstChild(feature, bedsField);

			double[] coordinate = parseCoordinate(feature);
			if (coordinate == null) {
				LOG.warn("Skipping hospital '{}' from {} because its coordinate could not be parsed.",
					name, path);
				continue;
			}

			Map<String, String> attributes = new LinkedHashMap<>();
			attributes.put("name", name);
			attributes.put("beds", beds);
			attributes.put("street", textOfFirstChild(feature, "gc_strasse"));
			attributes.put("house_number", textOfFirstChild(feature, "gc_haus"));
			attributes.put("postal_code", textOfFirstChild(feature, "gc_plz"));
			attributes.put("district", textOfFirstChild(feature, "gc_ortsteil"));
			attributes.put("gisid", gisId);
			attributes.put("source_layer",
				feature.getLocalName() != null ? feature.getLocalName() : feature.getNodeName());

			String id = idPrefix + "_" + (gisId.isEmpty() ? String.valueOf(i) : gisId);
			hospitals.add(new Hospital(id, name, coordinate[0], coordinate[1], attributes));
		}

		LOG.info("Parsed {} hospitals from {}", hospitals.size(), path);
		return hospitals;
	}

	private static Element firstChildElement(Element parent) {
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) node;
			}
		}
		return null;
	}

	private static String textOfFirstChild(Element parent, String localName) {
		NodeList nodes = parent.getElementsByTagNameNS("*", localName);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
	}

	private static double[] parseCoordinate(Element feature) {
		NodeList positions = feature.getElementsByTagNameNS(GML_NS, "pos");
		String coordinates = positions.getLength() > 0
			? positions.item(0).getTextContent().trim()
			: textOfFirstChild(feature, "lowerCorner");

		String[] parts = coordinates.split("\\s+");
		if (parts.length < 2) {
			return null;
		}
		try {
			return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void addHospitals(ActivityFacilities facilities, ActivityFacilitiesFactory factory, ActivityOption option) {
		List<Hospital> plank = parseWfsFile(plankrankenhaeuserXml, "plankrankenhaus",
			"kkh_standort", "betten_insgesamt");
		List<Hospital> weitere = parseWfsFile(weitereKrankenhaeuserXml, "weitere_krankenhaus",
			"name", "betten");

		List<Hospital> all = new ArrayList<>(plank.size() + weitere.size());
		all.addAll(plank);
		all.addAll(weitere);
		hospitalLocations = all;

		for (Hospital hospital : all) {
			ActivityFacility facility = factory.createActivityFacility(
				Id.create(hospital.id(), ActivityFacility.class),
				new Coord(hospital.x(), hospital.y()));
			facility.addActivityOption(option);
			hospital.attributes().forEach((key, value) -> facility.getAttributes().putAttribute(key, value));
			facilities.addActivityFacility(facility);
		}

		LOG.info("Added {} hospitals as accessibility opportunities (plank={}, weitere={})",
			all.size(), plank.size(), weitere.size());
	}

//	== GeoJSON opportunities (baeder) ==

	/**
	 * Parses a GeoJSON FeatureCollection and adds every feature that passes {@code filter}
	 * as an activity facility, i.e. the same representation used for the WFS/XML opportunities.
	 * <p>
	 * Only point features are supported. All coordinates are transformed to EPSG:25832, so
	 * downstream code sees the same CRS as for the XML sources.
	 */
	private void addGeoJsonOpportunities(ActivityFacilities facilities,
	                                            ActivityFacilitiesFactory factory,
	                                            ActivityOption option,
	                                            String path,
	                                            Predicate<GeoJsonOpportunity> filter) {
		List<GeoJsonOpportunity> parsed = parseGeoJson(path);
		List<GeoJsonOpportunity> selected = parsed.stream().filter(filter).toList();
		geoJsonLocations = selected;

		for (GeoJsonOpportunity opportunity : selected) {
			ActivityFacility facility = factory.createActivityFacility(
				Id.create(opportunity.id(), ActivityFacility.class),
				new Coord(opportunity.x(), opportunity.y()));
			facility.addActivityOption(option);
			opportunity.attributes().forEach((key, value) -> facility.getAttributes().putAttribute(key, value));
			facilities.addActivityFacility(facility);
		}

		LOG.info("Added {} of {} GeoJSON features from {} as {} opportunities",
			selected.size(), parsed.size(), path, option.getType());
	}

	private static List<GeoJsonOpportunity> parseGeoJson(String path) {
		JsonObject root;
		try (FileReader reader = new FileReader(path)) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new RuntimeException("Could not read GeoJSON file '" + path + "'.", e);
		}

		MathTransform transform = geoJsonTransform(root, path);
		JsonArray features = root.has("features") ? root.getAsJsonArray("features") : new JsonArray();
		List<GeoJsonOpportunity> opportunities = new ArrayList<>(features.size());

		for (int i = 0; i < features.size(); i++) {
			JsonObject feature = features.get(i).getAsJsonObject();
			JsonObject properties = feature.has("properties") && feature.get("properties").isJsonObject()
				? feature.getAsJsonObject("properties")
				: new JsonObject();
			JsonObject geometry = feature.has("geometry") && feature.get("geometry").isJsonObject()
				? feature.getAsJsonObject("geometry")
				: null;

			double[] coordinate = representativePoint(geometry, transform);
			if (coordinate == null) {
				LOG.warn("Skipping GeoJSON feature {} from {} because its coordinate could not be parsed.",
					i, path);
				continue;
			}

			Map<String, String> attributes = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
				JsonElement value = entry.getValue();
				if (value != null && !value.isJsonNull()) {
					attributes.put(entry.getKey(),
						value.isJsonPrimitive() ? value.getAsString() : value.toString());
				}
			}

			String id = firstNonEmpty(attributes.get("gml_id"), attributes.get("id"), String.valueOf(i));
			String name = firstNonEmpty(
				attributes.get("name_des_schwimmbads"),
				attributes.get("name"),
				attributes.get("uebergeord"),
				id);
			attributes.putIfAbsent("name", name);

			opportunities.add(new GeoJsonOpportunity(id, name, coordinate[0], coordinate[1], attributes));
		}

		LOG.info("Parsed {} GeoJSON features from {}", opportunities.size(), path);
		return opportunities;
	}

	/**
	 * Builds the transformation from the GeoJSON's CRS (usually CRS84/WGS84 lon-lat) to EPSG:25832.
	 */
	private static MathTransform geoJsonTransform(JsonObject root, String path) {
		String sourceCode = "4326";
		if (root.has("crs") && root.get("crs").isJsonObject()) {
			JsonObject crs = root.getAsJsonObject("crs");
			if (crs.has("properties") && crs.get("properties").isJsonObject()) {
				JsonObject properties = crs.getAsJsonObject("properties");
				String name = properties.has("name") ? properties.get("name").getAsString() : "";
				sourceCode = epsgCodeOf(name, sourceCode);
			}
		}

		try {
//			Geographic CRS in GeoJSON is lon/lat; projected CRS use their natural easting/northing order.
			boolean longitudeFirst = sourceCode.equals("4326");
			CoordinateReferenceSystem source = org.geotools.referencing.CRS.decode(
				"EPSG:" + sourceCode, longitudeFirst);
			CoordinateReferenceSystem target = org.geotools.referencing.CRS.decode(CRS);
			return org.geotools.referencing.CRS.findMathTransform(source, target, true);
		} catch (FactoryException e) {
			throw new RuntimeException("Could not create CRS transformation from EPSG:" + sourceCode
				+ " to " + CRS + " for GeoJSON file '" + path + "'.", e);
		}
	}

	private static String epsgCodeOf(String crsName, String fallback) {
		if (crsName == null || crsName.isBlank()
			|| crsName.toUpperCase().contains("CRS84")
			|| crsName.contains("4326")) {
			return "4326";
		}
		Matcher matcher = Pattern.compile("EPSG[:/]{1,2}(\\d+)").matcher(crsName);
		return matcher.find() ? matcher.group(1) : fallback;
	}

	private static double[] representativePoint(JsonObject geometry, MathTransform transform) {
		if (geometry == null || !geometry.has("type") || !geometry.has("coordinates")) {
			return null;
		}

		JsonElement coordinates = geometry.get("coordinates");
		return switch (geometry.get("type").getAsString()) {
			case "Point" -> transformPosition(coordinates.getAsJsonArray(), transform);
			case "MultiPoint" -> {
				JsonArray points = coordinates.getAsJsonArray();
				yield points.isEmpty() ? null : transformPosition(points.get(0).getAsJsonArray(), transform);
			}
			default -> {
				LOG.warn("Unsupported GeoJSON geometry type '{}'", geometry.get("type").getAsString());
				yield null;
			}
		};
	}

	private static double[] transformPosition(JsonArray position, MathTransform transform) {
		if (position == null || position.size() < 2) {
			return null;
		}
		double[] coordinate = {position.get(0).getAsDouble(), position.get(1).getAsDouble()};
		try {
			transform.transform(coordinate, 0, coordinate, 0, 1);
			return coordinate;
		} catch (TransformException e) {
			LOG.warn("Could not transform coordinate ({}, {}): {}",
				coordinate[0], coordinate[1], e.getMessage());
			return null;
		}
	}

	private static String firstNonEmpty(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}
}
