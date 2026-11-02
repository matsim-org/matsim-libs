package org.matsim.contrib.msp.capability;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.msp.capability.prepare.DefaultCapabilityPersonFilter;
import org.matsim.contrib.msp.capability.result.CapabilityWriter;
import org.matsim.contrib.msp.capability.result.FacilityRecord;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the whole pipeline on a small scenario: an input directory holding a config, a network and plans,
 * plus a csv of places, in and the three output levels out.
 */
public class RunCapabilityAnalysisTest {

	/** 15 km/h, the speed the bike vehicle type of the scenario is given. */
	private static final double BIKE_SPEED = 15 / 3.6;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runsOnAnInputDirectory() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory(), "run");
		Files.createDirectories(directory);
		writeScenario(directory);

		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			near,Shop next door,shop,Konsum,1000,0,08:00:00,20:00:00
			far,Shop far away,shop,Edeka,9000,0,08:00:00,20:00:00
			night,Night shop,shop,Spaeti,1000,0,22:00:00,23:30:00
			tiny,Twenty minute shop,shop,Kiosk,1000,0,00:00:00,00:20:00
			""");

		new RunCapabilityAnalysis().execute(
			"--input-directory", directory.toString(),
			"--places", places.toString()
		);

		Path output = output(directory);
		Path facilityLevel = output.resolve(CapabilityWriter.FACILITIES_FILE);
		assertTrue(Files.exists(facilityLevel), "the facility level should have been written");
		assertTrue(Files.exists(output.resolve(CapabilityWriter.INSERTIONS_FILE)));
		assertTrue(Files.exists(output.resolve(CapabilityWriter.PERSONS_FILE)));
		assertTrue(Files.exists(output.resolve(CapabilityWriter.ACTIVITIES_FILE)));

		List<CSVRecord> rows = read(facilityLevel);

		assertEquals(List.of(FacilityRecord.HEADER), List.of(rows.get(0).getParser().getHeaderNames().toArray()));

		// the commuter can reach the near shop by car, in the morning and in the evening
		CSVRecord commuterByCar = row(rows, "commuter", TransportMode.car, "near");
		assertEquals("shop", commuterByCar.get("act_type"));
		assertEquals("Konsum", commuterByCar.get("facility_sub_type"));
		assertEquals("Shop next door", commuterByCar.get("facility_name"));
		assertEquals(2, Integer.parseInt(commuterByCar.get("n_insertions")),
			"before and after work, because home does not pin the day down");
		assertTrue(Double.parseDouble(commuterByCar.get("u_potential_max")) > 3.0,
			"base value plus some flexibility, and the shop is on the way so the detour is free");
		assertTrue(Double.parseDouble(commuterByCar.get("best_slack")) > 0);
		assertEquals(3, Integer.parseInt(commuterByCar.get("n_mandatory_act")), "home, work, home");
		assertEquals(1, Integer.parseInt(commuterByCar.get("n_fixed_act")), "work is the only fixed one");

		// the sum over the insertions is reported, but it is not what the utility is built from
		assertTrue(Double.parseDouble(commuterByCar.get("sum_slack"))
			> Double.parseDouble(commuterByCar.get("best_slack")));

		// someone who stays home all day has more freedom in one go than the commuter has in either of
		// their two chances, and so a higher utility
		CSVRecord stayHomeByCar = row(rows, "stay-home", TransportMode.car, "near");
		assertTrue(Double.parseDouble(stayHomeByCar.get("best_slack"))
			> Double.parseDouble(commuterByCar.get("best_slack")),
			"the person who is free all day has the most flexible time");
		assertTrue(Double.parseDouble(stayHomeByCar.get("u_potential_max"))
			> Double.parseDouble(commuterByCar.get("u_potential_max")));

		// ... even though they have only one free period, which is why the utility is not based on the count
		assertEquals(1, Integer.parseInt(stayHomeByCar.get("n_insertions")));

		// the night shop is only reachable in the evening, not in the morning, so it offers one chance
		// rather than two - opening hours narrow the day down just as fixed activities do
		CSVRecord nightShop = row(rows, "commuter", TransportMode.car, "night");
		assertEquals(1, Integer.parseInt(nightShop.get("n_insertions")));
		assertEquals(1, Integer.parseInt(nightShop.get("best_insertion_id")), "the evening is the second period");

		// a shop whose opening window is shorter than the minimum duration is of no use to anybody
		assertTrue(rows.stream().noneMatch(row -> row.get("facility_id").equals("tiny")),
			"a 20 minute window cannot hold a 30 minute activity, so the shop is left out entirely");

		// walking 9 km is not on, so the far shop has no walk row - but it is reachable by car
		assertFalse(rows.stream().anyMatch(row ->
			row.get("facility_id").equals("far") && row.get("mode").equals(TransportMode.walk)),
			"the far shop is not walkable within the allowed detour");
		assertTrue(rows.stream().anyMatch(row ->
			row.get("facility_id").equals("far") && row.get("mode").equals(TransportMode.car)),
			"by car the far shop is a 1600 s detour, which is within the allowance");

		// the near shop costs no detour at all for the commuter: it is on the way to work
		assertEquals(0.0, Double.parseDouble(commuterByCar.get("c_insertion_min")), 1e-6,
			"a shop on the way to work costs no extra travel time");

		// the night-shift worker is skipped, their work reaches past the end of the day
		assertTrue(rows.stream().noneMatch(row -> row.get("person_id").equals("night-shift")));

		// so is the person whose day does not start and end at home
		assertTrue(rows.stream().noneMatch(row -> row.get("person_id").equals("not-home-based")));
	}

	@Test
	void theThreeLevelsAggregateIntoOneAnother() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory(), "run");
		Files.createDirectories(directory);
		writeScenario(directory);

		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			near,Shop next door,shop,Konsum,1000,0,08:00:00,20:00:00
			second,Second Konsum,shop,Konsum,2000,0,08:00:00,20:00:00
			other,Other brand,shop,Edeka,1000,0,08:00:00,20:00:00
			""");

		new RunCapabilityAnalysis().execute(
			"--input-directory", directory.toString(),
			"--places", places.toString()
		);

		List<CSVRecord> insertions = read(output(directory).resolve(CapabilityWriter.INSERTIONS_FILE));
		List<CSVRecord> facilities = read(output(directory).resolve(CapabilityWriter.FACILITIES_FILE));
		List<CSVRecord> persons = read(output(directory).resolve(CapabilityWriter.PERSONS_FILE));

		// level 2 is level 1 grouped by facility
		for (CSVRecord facility : facilities) {

			List<CSVRecord> own = insertions.stream()
				.filter(row -> row.get("person_id").equals(facility.get("person_id")))
				.filter(row -> row.get("mode").equals(facility.get("mode")))
				.filter(row -> row.get("facility_id").equals(facility.get("facility_id")))
				.toList();

			assertEquals(Integer.parseInt(facility.get("n_insertions")), own.size());
			assertEquals(own.stream().mapToDouble(row -> Double.parseDouble(row.get("slack"))).sum(),
				Double.parseDouble(facility.get("sum_slack")), 1.0);

			CSVRecord best = own.stream()
				.max(java.util.Comparator.comparingDouble(row -> Double.parseDouble(row.get("u_potential"))))
				.orElseThrow();
			assertEquals(Double.parseDouble(best.get("u_potential")),
				Double.parseDouble(facility.get("u_potential_max")), 1e-3,
				"the facility is worth its best insertion");
			assertEquals(Integer.parseInt(best.get("insertion_id")),
				Integer.parseInt(facility.get("best_insertion_id")));

			// the insertion level carries the parameters its own utility was computed with
			assertEquals(Double.parseDouble(best.get("u_0"))
					+ Double.parseDouble(best.get("k"))
					* Math.log(1 + Double.parseDouble(best.get("slack")) / Double.parseDouble(best.get("t_typ")))
					- Double.parseDouble(best.get("c_insertion")),
				Double.parseDouble(best.get("u_potential")), 1e-3);
		}

		// level 3 is level 2 grouped by person and mode
		assertEquals(2 * 2, persons.size(), "two analysable persons, two modes, one activity type");

		for (CSVRecord person : persons) {

			List<CSVRecord> own = facilities.stream()
				.filter(row -> row.get("person_id").equals(person.get("person_id")))
				.filter(row -> row.get("mode").equals(person.get("mode")))
				.toList();

			assertEquals(own.size(), Integer.parseInt(person.get("n_reachable_facilities")));

			if (own.isEmpty()) {
				assertEquals("", person.get("u_potential_max"), "nothing reachable, so there is no utility");
				assertEquals("", person.get("u_star"));
				continue;
			}

			assertEquals(own.stream().mapToDouble(row -> Double.parseDouble(row.get("u_potential_max")))
					.max().orElseThrow(),
				Double.parseDouble(person.get("u_potential_max")), 1e-3);
			assertTrue(Double.parseDouble(person.get("u_star"))
					>= Double.parseDouble(person.get("u_potential_max")),
				"a logsum is never below the best of its alternatives");
			assertEquals(2, Integer.parseInt(person.get("n_sub_types")), "Konsum and Edeka");
		}
	}

	@Test
	void bikeIsRoutedOnTheNetworkAtTheSpeedOfItsVehicleType() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory(), "run");
		Files.createDirectories(directory);
		writeScenario(directory, true);

		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			mid,Shop halfway out,shop,Konsum,5000,0,08:00:00,20:00:00
			far,Shop far away,shop,Edeka,9000,0,08:00:00,20:00:00
			""");

		new RunCapabilityAnalysis().execute(
			"--input-directory", directory.toString(),
			"--places", places.toString()
		);

		List<CSVRecord> insertions = read(output(directory).resolve(CapabilityWriter.INSERTIONS_FILE));

		// every link carries both modes, so car and bike take the same way to the same shop and the only
		// thing that can differ is the speed: the 10 m/s free speed of the links for car, which no vehicle
		// type caps, against the 15 km/h of the bike vehicle type
		double ratio = 10 / BIKE_SPEED;
		CSVRecord byCar = insertion(insertions, "commuter", TransportMode.car, "mid", 1);
		CSVRecord byBike = insertion(insertions, "commuter", TransportMode.bike, "mid", 1);

		assertTrue(Double.parseDouble(byCar.get("travel_in")) > 0, "the shop is not next door");
		assertEquals(ratio * Double.parseDouble(byCar.get("travel_in")),
			Double.parseDouble(byBike.get("travel_in")), 1.0,
			"the bike is capped at 15 km/h, so it takes 2.4 times as long over the same links");
		assertEquals(ratio * Double.parseDouble(byCar.get("travel_out")),
			Double.parseDouble(byBike.get("travel_out")), 1.0);
		assertEquals(ratio * Double.parseDouble(byCar.get("t_extra")),
			Double.parseDouble(byBike.get("t_extra")), 1.0, "and so is the detour it costs");

		// which is what makes the capability of a cyclist smaller. The morning is wide enough for the
		// shop by car - it opens at 08:00 and work starts at 09:30 - but not at 15 km/h, so the bike is
		// left with the evening alone.
		assertEquals(2, count(insertions, "commuter", TransportMode.car, "mid"), "before and after work");
		assertEquals(1, count(insertions, "commuter", TransportMode.bike, "mid"),
			"the morning is too tight for a 5 km ride there and back");

		// and 9 km each way is within the allowed detour by car, but not at 15 km/h at all
		assertTrue(insertions.stream().anyMatch(insertion -> insertion.get("facility_id").equals("far")
			&& insertion.get("mode").equals(TransportMode.car)));
		assertFalse(insertions.stream().anyMatch(insertion -> insertion.get("facility_id").equals("far")
			&& insertion.get("mode").equals(TransportMode.bike)));
	}

	@Test
	void theInsertionLevelCanBeTurnedOff() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory(), "run");
		Files.createDirectories(directory);
		writeScenario(directory);

		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			near,Shop next door,shop,Konsum,1000,0,08:00:00,20:00:00
			""");

		new RunCapabilityAnalysis().execute(
			"--input-directory", directory.toString(),
			"--places", places.toString(),
			"--skip-insertions"
		);

		assertFalse(Files.exists(output(directory).resolve(CapabilityWriter.INSERTIONS_FILE)),
			"the largest table is the one a big run can do without");
		assertTrue(Files.exists(output(directory).resolve(CapabilityWriter.FACILITIES_FILE)));
		assertTrue(Files.exists(output(directory).resolve(CapabilityWriter.PERSONS_FILE)));
	}

	@Test
	void writesHowEachPlanWasReducedToItsFundamentalPlan() throws IOException {

		Path directory = Path.of(utils.getOutputDirectory(), "run");
		Files.createDirectories(directory);

		writeScenario(directory, false, population -> {

			// a day with two discretionary activities in it, and types spelled the way a real scenario
			// spells them: a typical duration appended, and home split into a morning and an evening stay
			Plan errands = plan(population, "errand-runner");
			activity(errands, "home_morning_3600", 0, -1, 8 * 3600);
			leg(errands);
			activity(errands, "work_28800", 2000, 8.5 * 3600, 16 * 3600);
			leg(errands);
			activity(errands, "shop_other_1800", 1000, 16.5 * 3600, 17 * 3600);
			leg(errands);
			activity(errands, "leisure_7200", 1000, 17.5 * 3600, 19 * 3600);
			leg(errands);
			activity(errands, "home_evening", 0, 19.5 * 3600, -1);

			// nothing to drop: the fundamental plan is the whole plan
			Plan commuter = plan(population, "plain-commuter");
			activity(commuter, "home", 0, -1, 8 * 3600);
			leg(commuter);
			activity(commuter, "educ_primary", 2000, 8.5 * 3600, 16 * 3600);
			leg(commuter);
			activity(commuter, "home", 0, 16.5 * 3600, -1);

			// only a discretionary activity, so the fundamental plan is the two home stays either side
			// of it - which are one stay once it is gone
			Plan shopper = plan(population, "shopper");
			activity(shopper, "home_morning", 0, -1, 10 * 3600);
			leg(shopper);
			activity(shopper, "shop_other_1800", 1000, 10.5 * 3600, 11 * 3600);
			leg(shopper);
			activity(shopper, "home_evening", 0, 11.5 * 3600, -1);
		});

		Path places = Path.of(utils.getOutputDirectory(), "places.csv");
		Files.writeString(places, """
			id,name,type,sub_type,x,y,opening_time,closing_time
			near,Shop next door,shop,Konsum,1000,0,08:00:00,20:00:00
			""");

		new RunCapabilityAnalysis().execute(
			"--input-directory", directory.toString(),
			"--places", places.toString()
		);

		List<CSVRecord> plans = read(output(directory).resolve(CapabilityWriter.FUNDAMENTAL_PLANS_FILE));
		Map<String, CSVRecord> byPerson = plans.stream()
			.collect(Collectors.toMap(row -> row.get("person_id"), row -> row));

		assertEquals(Set.of("errand-runner", "plain-commuter", "shopper"), byPerson.keySet(),
			"every person the filter accepted has a row");

		CSVRecord reduced = byPerson.get("errand-runner");
		assertEquals("home-mandatory-home", reduced.get("fundamental_activity_chain_class"));
		assertEquals("home-work-home", reduced.get("fundamental_activity_chain"),
			"shop and leisure are discretionary, so they are dropped");
		assertEquals("home-mandatory-discretionary-discretionary-home",
			reduced.get("original_activity_chain_class"));
		assertEquals("home-work-shop_other-leisure-home", reduced.get("original_activity_chain"),
			"the typical duration is stripped and both home stays are plain 'home'");

		CSVRecord untouched = byPerson.get("plain-commuter");
		assertEquals("home-mandatory-home", untouched.get("fundamental_activity_chain_class"),
			"a sub-type of a mandatory type is still mandatory");
		assertEquals("home-educ_primary-home", untouched.get("fundamental_activity_chain"),
			"a sub-type of a mandatory type is kept as it is");
		assertEquals("home-mandatory-home", untouched.get("original_activity_chain_class"));
		assertEquals(untouched.get("fundamental_activity_chain"), untouched.get("original_activity_chain"),
			"nothing was discretionary, so the two chains are the same");

		// nothing mandatory outside home: the two home stays are one, so the chain is not 'home-home'
		CSVRecord shopper = byPerson.get("shopper");
		assertEquals("home", shopper.get("fundamental_activity_chain_class"));
		assertEquals("home", shopper.get("fundamental_activity_chain"),
			"the discretionary activity between the two home stays is gone, so the person never left");
		assertEquals("home-discretionary-home", shopper.get("original_activity_chain_class"));
		assertEquals("home-shop_other-home", shopper.get("original_activity_chain"));
	}

	@Test
	void failsWithoutAnInputDirectory() throws Exception {
		Integer code = new RunCapabilityAnalysis().withArgs(
			"--input-directory", Path.of(utils.getOutputDirectory(), "nope").toString(),
			"--places", "places.csv"
		).call();
		assertEquals(2, code, "a missing input directory is a user error");
	}

	/**
	 * Writes a config, a network and plans the way a run's output directory holds them.
	 */
	private void writeScenario(Path directory) {
		writeScenario(directory, false);
	}

	private void writeScenario(Path directory, boolean withBike) {
		writeScenario(directory, withBike, RunCapabilityAnalysisTest::buildPopulation);
	}

	/**
	 * @param withBike whether the run routes bike on the network and the capability analysis reports on
	 * it, which also puts a vehicles file into the directory: that is where the speed of a bike lives
	 */
	private void writeScenario(Path directory, boolean withBike, Consumer<Population> populationBuilder) {

		Config config = ConfigUtils.createConfig(new CapabilityConfigGroup());
		// walk teleported at 1 m/s on a straight line, so the test numbers are easy to reason about
		config.routing().getOrCreateModeRoutingParams(TransportMode.walk).setTeleportedModeSpeed(1.0);
		config.routing().getOrCreateModeRoutingParams(TransportMode.walk).setBeelineDistanceFactor(1.0);

		// the type the places csv of these tests offers. There is no default type, so it has to be
		// declared: the types that have a parameter set are exactly the types the analysis reports on
		ConfigUtils.addOrGetModule(config, CapabilityConfigGroup.class)
			.getOrCreateDiscretionaryActivityParams("shop");

		if (withBike) {
			config.routing().setNetworkModes(List.of(TransportMode.car, TransportMode.bike));
			ConfigUtils.addOrGetModule(config, CapabilityConfigGroup.class)
				.setModes(Set.of(TransportMode.car, TransportMode.walk, TransportMode.bike));
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		buildNetwork(scenario.getNetwork(), withBike);
		populationBuilder.accept(scenario.getPopulation());

		new ConfigWriter(config).write(directory.resolve("output_config.xml").toString());
		new NetworkWriter(scenario.getNetwork()).write(directory.resolve("output_network.xml.gz").toString());
		PopulationUtils.writePopulation(scenario.getPopulation(),
			directory.resolve("output_plans.xml.gz").toString());

		if (withBike) {
			scenario.getVehicles().addVehicleType(VehicleUtils
				.createVehicleType(Id.create(TransportMode.bike, VehicleType.class))
				.setMaximumVelocity(BIKE_SPEED));
			new MatsimVehicleWriter(scenario.getVehicles())
				.writeFile(directory.resolve("output_vehicles.xml.gz").toString());
		}
	}

	/**
	 * A straight line of links from x=0 to x=10000, 10 m/s, so car travel time is distance / 10.
	 *
	 * @param withBike whether the links carry bike next to car. They all do: what the analysis is asked
	 * about here is the speed of the mode, not which links it may use - that is
	 * {@link org.matsim.contrib.msp.capability.measure.CapabilityTravelTimesTest}.
	 */
	private static void buildNetwork(Network network, boolean withBike) {
		Set<String> modes = withBike ? Set.of(TransportMode.car, TransportMode.bike) : Set.of(TransportMode.car);
		Node previous = NetworkUtils.createAndAddNode(network, Id.createNodeId(0), new Coord(0, 0));
		for (int x = 1000; x <= 10000; x += 1000) {
			Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId(x), new Coord(x, 0));
			NetworkUtils.createAndAddLink(network, Id.createLinkId(previous.getId() + "-" + node.getId()),
				previous, node, 1000, 10, 1800, 1).setAllowedModes(modes);
			NetworkUtils.createAndAddLink(network, Id.createLinkId(node.getId() + "-" + previous.getId()),
				node, previous, 1000, 10, 1800, 1).setAllowedModes(modes);
			previous = node;
		}
	}

	private static void buildPopulation(Population population) {

		// home at x=0, work at x=2000: a plain commuter, free before and after work
		Plan commuter = plan(population, "commuter");
		activity(commuter, "home", 0, -1, 8 * 3600);
		leg(commuter);
		activity(commuter, "work", 2000, 9.5 * 3600, 17 * 3600);
		leg(commuter);
		activity(commuter, "home", 0, 17.5 * 3600, -1);

		// never leaves home, so the whole day is free
		Plan stayHome = plan(population, "stay-home");
		activity(stayHome, "home", 0, -1, -1);

		// works through the night, so the day cannot be analysed without wrap around
		Plan nightShift = plan(population, "night-shift");
		activity(nightShift, "home", 0, -1, 20 * 3600);
		leg(nightShift);
		activity(nightShift, "work", 2000, 21 * 3600, 29 * 3600);
		leg(nightShift);
		activity(nightShift, "home", 0, 29.5 * 3600, -1);

		// starts the day at work rather than at home
		Plan notHomeBased = plan(population, "not-home-based");
		activity(notHomeBased, "work", 2000, -1, 17 * 3600);
		leg(notHomeBased);
		activity(notHomeBased, "home", 0, 17.5 * 3600, -1);
	}

	private static Plan plan(Population population, String id) {
		Person person = population.getFactory().createPerson(Id.createPersonId(id));
		// the filter only speaks for adults, so the test population has to carry an age
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 30);
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		population.addPerson(person);
		return plan;
	}

	private static void activity(Plan plan, String type, double x, double startTime, double endTime) {
		Activity activity = PopulationUtils.createActivityFromCoord(type, new Coord(x, 0));
		if (startTime >= 0) {
			activity.setStartTime(startTime);
		}
		if (endTime >= 0) {
			activity.setEndTime(endTime);
		}
		plan.addActivity(activity);
	}

	private static void leg(Plan plan) {
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
	}

	/**
	 * @return where the levels end up: a directory of its own inside the input directory
	 */
	private static Path output(Path directory) {
		return directory.resolve(RunCapabilityAnalysis.OUTPUT_DIRECTORY);
	}

	private static List<CSVRecord> read(Path file) throws IOException {
		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(file.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
			return parser.getRecords();
		}
	}

	private static long count(List<CSVRecord> rows, String person, String mode, String facility) {
		return rows.stream()
			.filter(row -> row.get("person_id").equals(person))
			.filter(row -> row.get("mode").equals(mode))
			.filter(row -> row.get("facility_id").equals(facility))
			.count();
	}

	/**
	 * Like {@link #row} but for one stretch of the day in particular, which the insertion level needs:
	 * the modes do not have to have the same ones.
	 */
	private static CSVRecord insertion(List<CSVRecord> rows, String person, String mode, String facility, int id) {
		return rows.stream()
			.filter(row -> row.get("person_id").equals(person))
			.filter(row -> row.get("mode").equals(mode))
			.filter(row -> row.get("facility_id").equals(facility))
			.filter(row -> Integer.parseInt(row.get("insertion_id")) == id)
			.findFirst()
			.orElseThrow(() -> new AssertionError("No insertion " + id + " for " + person + " / " + mode + " / "
				+ facility));
	}

	private static CSVRecord row(List<CSVRecord> rows, String person, String mode, String facility) {
		return rows.stream()
			.filter(row -> row.get("person_id").equals(person))
			.filter(row -> row.get("mode").equals(mode))
			.filter(row -> row.get("facility_id").equals(facility))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No row for " + person + " / " + mode + " / " + facility
				+ " in " + rows.stream().collect(Collectors.toMap(
				r -> r.get("person_id") + "/" + r.get("mode") + "/" + r.get("facility_id"),
				Function.identity(), (a, b) -> a)).keySet()));
	}
}
