package org.matsim.contrib.accessibility.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunPersonBasedAccBerlin {

	private static final String OUTPUT_ROOT = "../public-svn/matsim/scenarios/countries/de/berlin/projects/fabilut/output-1pct/";
	private static final String OUTPUT_BASE = OUTPUT_ROOT + "base/";
	private static final String OUTPUT_POLICY = OUTPUT_ROOT + "policy/";
	private static String OUTPUT_DIR;

	private static final String HOSPITAL_INPUT_DIR = OUTPUT_ROOT + "input/";
	private static final String PLANKRANKENHAEUSER_XML = HOSPITAL_INPUT_DIR + "plankrankenhaeuser.xml";
	private static final String WEITERE_KRANKENHAEUSER_XML = HOSPITAL_INPUT_DIR + "weitere_krankenhaeuser.xml";
	private static final String HOSPITAL_ACTIVITY_TYPE = "hospital";

//	Hospital accessibility results are written into a dedicated folder of the R analysis input directory.
	private static final String HOSPITAL_OUTPUT_DIR = "contribs/accessibility/src/main/R/THESIS/input/hospital/";

	private static final String CRS = "EPSG:25832";

	private static Scenario scenario;

	private static final Logger LOG = LogManager.getLogger(RunPersonBasedAccBerlin.class);

	private enum RunScenario {base, policy}

	public static void main(String[] args) {
//		== Run options ==
		boolean personBased = true; //todo select accessibility measure (true for person-based/false for location-based)
		RunScenario runScenario = RunScenario.base; //todo select scenario (base/policy)
		Modes4Accessibility targetMode = Modes4Accessibility.pt; //todo select transport mode (car/pt/teleportedWalk)
		AccessibilityConfigGroup.AccessibilityMeasureType measureType = AccessibilityConfigGroup.AccessibilityMeasureType.rawSum; //todo select measure type (logSum/rawSum, rawSum for multimodal R analysis)

		OUTPUT_DIR = OUTPUT_ROOT + runScenario + "/";
		loadFiles(personBased, targetMode, measureType);


//		== Calculate accessibility ==
		String eventsFile = OUTPUT_DIR + "berlin-v6.3.output_events.xml.gz";
		long start = System.currentTimeMillis();
		String accessibilityType = personBased ? "person-based" : "location-based";
		LOG.info(
			"Calculating {} {} accessibility for {} scenario using {}...",
			accessibilityType,
			targetMode,
			runScenario,
			measureType
		);

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, List.of(HOSPITAL_ACTIVITY_TYPE));
//		Spa-based reference run:
//		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, List.of("spa"));

		PersonBasedResultsComparator dataListener = null;
		if (personBased) {
			dataListener = new PersonBasedResultsComparator();
			builder.addDataListener(dataListener);
		}
		builder.build().run() ;

		long ms = System.currentTimeMillis() - start;
		long seconds = ms / 1000;
		long minutes = seconds / 60;
		LOG.info("Accessibility computation finished in {} minutes (≈ {} seconds)", minutes, seconds);

		if (personBased){
			Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();
			writeCSV(accessibilitiesMap, HOSPITAL_OUTPUT_DIR + "pbAcc_" + targetMode + "_" + runScenario + "_" + measureType + ".csv");
		}
	}

	private static void loadFiles(boolean personBased, Modes4Accessibility targetMode, AccessibilityConfigGroup.AccessibilityMeasureType measureType) {

//		== Input Files ==
		String networkFile 			= OUTPUT_DIR + "berlin-v6.3.output_network.xml.gz";
		//String facilitiesFile 	= OUTPUT_DIR + "berlin-v6.3.output_facilities.xml.gz";//manually set facilities
		String plansFile			= OUTPUT_BASE + "berlin-v6.3.output_plans.xml.gz";
		String transitScheduleFile 	= OUTPUT_DIR + "berlin-v6.3.output_transitSchedule.xml.gz";

//		== Create config ==
		final Config config = ConfigUtils.createConfig();

//		== Adjust config ==
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(OUTPUT_DIR);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(CRS);
		config.routing().setRoutingRandomness(0.);

		// add monetary distance rate for car
		config.scoring().getModes().get(TransportMode.car).setMonetaryDistanceRate(-0.0002);

		config.network().setInputFile(networkFile);
		//config.facilities().setInputFile(facilitiesFile);
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);
		config.plans().setInputFile(plansFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);


//		== Accessibility config ==
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setPersonBased(personBased);
		acg.setAccessibilityMeasureType(measureType);
		acg.setTimeOfDay(8*60*60.);
		acg.setTileSize_m(1000);
		if (personBased) {
			acg.setAreaOfAccessibilityComputation(
				AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromPopulation
			);
		} else {
			acg.setAreaOfAccessibilityComputation(
				AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox
			);
		}

//		== Mode selection ==
		List<Modes4Accessibility> accModes = List.of(targetMode);
		for(Modes4Accessibility mode : Modes4Accessibility.values()) {
			acg.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}


//		== Load scenario ==
		scenario = ScenarioUtils.loadScenario(config);

//		== Adding facilities ==
		ActivityFacilities activityFacilities = scenario.getActivityFacilities();
		ActivityFacilitiesFactory af = activityFacilities.getFactory();

//		--- Hospital opportunities (active) ---
		ActivityOption aoHospital = af.createActivityOption(HOSPITAL_ACTIVITY_TYPE);
		addHospitals(activityFacilities, af, aoHospital);

//		--- Spa opportunities (kept for later refactoring) ---
//		ActivityOption aoSpa = af.createActivityOption("spa");
//
//		addFacility(activityFacilities, af, aoSpa, "vabali", 795634.64, 5828763.74);//001_Mitte
//		addFacility(activityFacilities, af, aoSpa, "liquidrom", 797312.78, 5825825.94);//002_Friedrichshain-Kreuzberg
//		addFacility(activityFacilities, af, aoSpa, "meridian", 784623.35, 5828694.12);//005_Spandau
//		addFacility(activityFacilities, af, aoSpa, "aspria", 791428.73, 5825366.54);//004_Charlottenburg-Wilmersdorf
//		addFacility(activityFacilities, af, aoSpa, "hamam", 800182.05, 5825953.76);//002_Friedrichshain-Kreuzberg
//		addFacility(activityFacilities, af, aoSpa, "saunabad", 800320.0, 5829910.0);//003_Pankow
//		addFacility(activityFacilities, af, aoSpa, "kristall", 786190.0, 5791400.0);//NA (Ludwigsfelde)


//		== Assigning home coords for each person ==
		int[] kept = {0};
		int[] skipped = {0};
		int[] limit = {0};
		scenario.getPopulation().getPersons().values().removeIf(person -> {
//			if (limit[0] >= 1000){	//todo set limit how many person(s) get parsed
//				skipped[0]++;
//				return true;
//			}

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
			limit[0]++;
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

//		== Run ==
		LOG.info("Running person-based accessibility computation for Berlin...");

	}

//	Kept for the spa-based reference run; uncomment together with the spa block in loadFiles().
//	private static void addFacility(
//		ActivityFacilities facilities,
//		ActivityFacilitiesFactory factory,
//		ActivityOption activityOption,
//		String id,
//		double x,
//		double y){
//		ActivityFacility facility = factory.createActivityFacility(
//			Id.create(id, ActivityFacility.class),
//			new Coord(x, y)
//		);
//		facility.addActivityOption(activityOption);
//		facilities.addActivityFacility(facility);
//	}

	private static class PersonBasedResultsComparator implements PersonDataExchangeInterface {
		private final Map<Tuple<Person, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;

		@Override
		public synchronized void setPersonAccessibilities(Person person, Double timeOfDay, String mode, double accessibility) {
			Tuple<Person, Double> key = new Tuple<>(person, timeOfDay);
//			if (!accessibilitiesMap.containsKey(key)) {
//				Map<String,Double> accessibilitiesByMode = new HashMap<>();
//				accessibilitiesMap.put(key, accessibilitiesByMode);
//			}
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

	private record Hospital(String id, String name, double x, double y,
	                        Map<String, String> attributes) {}

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

	private static void addHospitals(ActivityFacilities facilities,
	                                 ActivityFacilitiesFactory factory,
	                                 ActivityOption option) {
		List<Hospital> plank = parseWfsFile(PLANKRANKENHAEUSER_XML, "plankrankenhaus",
			"kkh_standort", "betten_insgesamt");
		List<Hospital> weitere = parseWfsFile(WEITERE_KRANKENHAEUSER_XML, "weitere_krankenhaus",
			"name", "betten");

		List<Hospital> all = new ArrayList<>(plank.size() + weitere.size());
		all.addAll(plank);
		all.addAll(weitere);

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

}
