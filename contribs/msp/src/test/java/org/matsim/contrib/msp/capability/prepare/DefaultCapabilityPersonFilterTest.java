package org.matsim.contrib.msp.capability.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the default person filter: subpopulation, minimum age, home inside the study area, and a home
 * based day that fits into the analysed day.
 * <p>
 * Every person these tests build is an adult, so that the criterion under test is the only one that can
 * reject them; {@link #dropsMinors()} and its neighbours are where the age criterion itself is tested.
 */
public class DefaultCapabilityPersonFilterTest {

	/**
	 * The study area of the tests: a square from (0,0) to (1000,1000).
	 */
	private static final String CRS = "EPSG:25832";

	/**
	 * The age of the test persons: old enough to pass the age criterion without being near its edge.
	 */
	private static final int ADULT_AGE = 30;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void keepsTheHomeBasedResident() {
		Person commuter = commuter("commuter", new Coord(100, 100), 8 * 3600, 17 * 3600);
		PopulationUtils.putSubpopulation(commuter, "person");

		assertTrue(filter().accept(commuter));
	}

	@Test
	void dropsSubpopulationsOtherThanPerson() {

		Person freight = commuter("freight", new Coord(100, 100), 8 * 3600, 17 * 3600);
		PopulationUtils.putSubpopulation(freight, "freight");
		assertFalse(filter().accept(freight), "freight agents have no capability in the sense of the analysis");

		// a population that does not use subpopulations at all is not filtered on this criterion
		Person withoutSubpopulation = commuter("plain", new Coord(100, 100), 8 * 3600, 17 * 3600);
		assertNull(PopulationUtils.getSubpopulation(withoutSubpopulation));
		assertTrue(filter().accept(withoutSubpopulation));
	}

	@Test
	void dropsPersonsLivingOutsideTheStudyArea() {
		Person outside = commuter("outside", new Coord(5000, 5000), 8 * 3600, 17 * 3600);
		assertFalse(filter().accept(outside));
	}

	@Test
	void takesTheHomeCoordinateFromTheAttributesIfThereIsOne() {

		// the plan says the person lives outside, the attributes say inside - the attributes win
		Person person = commuter("attributes", new Coord(5000, 5000), 8 * 3600, 17 * 3600);
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.HOME_X, 100.0);
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.HOME_Y, 100.0);

		DefaultCapabilityPersonFilter filter = filter();
		assertTrue(filter.accept(person));
		assertEqualCoord(new Coord(100, 100), filter.homeCoordinate(person));

		// ... and the other way round, so that a home attribute outside the area is not overruled by a
		// plan that happens to hold an activity inside it
		Person reverse = commuter("reverse", new Coord(100, 100), 8 * 3600, 17 * 3600);
		reverse.getAttributes().putAttribute(DefaultCapabilityPersonFilter.HOME_X, "5000");
		reverse.getAttributes().putAttribute(DefaultCapabilityPersonFilter.HOME_Y, "5000");
		assertFalse(filter().accept(reverse), "a string attribute is read as a coordinate as well");
	}

	@Test
	void dropsPersonsWhoseHomeCannotBeLocated() {

		// a plan that only references links: there is no telling where this person lives
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("no-coord"));
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, ADULT_AGE);
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("1")));

		DefaultCapabilityPersonFilter filter = filter();
		assertNull(filter.homeCoordinate(person));
		assertFalse(filter.accept(person));

		// without a study area the home location does not matter, so the person is kept
		assertTrue(withoutStudyArea().accept(person));
	}

	@Test
	void dropsDaysThatDoNotStartAndEndAtHome() {

		Plan plan = plan("not-home-based");
		activity(plan, "work", new Coord(500, 500), -1, 17 * 3600);
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
		activity(plan, "home", new Coord(100, 100), 17.5 * 3600, -1);

		assertFalse(filter().accept(plan.getPerson()));
	}

	@Test
	void dropsDaysWhoseLastActivityReachesPastMidnight() {

		assertFalse(filter().accept(commuter("night-shift", new Coord(100, 100), 20 * 3600, 25 * 3600)),
			"work that ends at 25:00 cannot be analysed within the day");

		assertTrue(filter().accept(commuter("late", new Coord(100, 100), 20 * 3600, 24 * 3600)),
			"coming home right at the end of the day is still within it");
	}

	@Test
	void keepsPersonsWhoStayHomeAllDay() {
		Plan plan = plan("stay-home");
		activity(plan, "home", new Coord(100, 100), -1, -1);

		assertTrue(filter().accept(plan.getPerson()), "there is no non-home activity to end too late");
	}

	@Test
	void dropsMinors() {

		Person child = commuter("child", new Coord(100, 100), 8 * 3600, 15 * 3600);
		child.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 12);
		assertFalse(filter().accept(child), "a child's day is not theirs to rearrange");

		Person almost = commuter("almost", new Coord(100, 100), 8 * 3600, 15 * 3600);
		almost.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 17);
		assertFalse(filter().accept(almost), "17 is below the default minimum of 18");

		Person adult = commuter("just-adult", new Coord(100, 100), 8 * 3600, 15 * 3600);
		adult.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 18);
		assertTrue(filter().accept(adult), "the minimum age itself is kept");
	}

	@Test
	void dropsPersonsWhoseAgeIsUnknown() {

		Person person = commuter("no-age", new Coord(100, 100), 8 * 3600, 17 * 3600);
		person.getAttributes().removeAttribute(DefaultCapabilityPersonFilter.AGE);

		assertNull(DefaultCapabilityPersonFilter.age(person));
		assertFalse(filter().accept(person), "there is no telling whether this person qualifies");
	}

	@Test
	void readsTheAgeWhateverTypeItWasWrittenAs() {

		Person asString = commuter("string-age", new Coord(100, 100), 8 * 3600, 17 * 3600);
		asString.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, "42");
		assertEquals(42, DefaultCapabilityPersonFilter.age(asString));
		assertTrue(filter().accept(asString), "a plans file may write the age as a string");

		Person asDouble = commuter("double-age", new Coord(100, 100), 8 * 3600, 17 * 3600);
		asDouble.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 42.0);
		assertEquals(42, DefaultCapabilityPersonFilter.age(asDouble));
		assertTrue(filter().accept(asDouble));
	}

	@Test
	void aMinimumAgeOfZeroSkipsTheAgeCriterion() {

		Person child = commuter("child", new Coord(100, 100), 8 * 3600, 15 * 3600);
		child.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 12);

		Person unknown = commuter("no-age", new Coord(100, 100), 8 * 3600, 17 * 3600);
		unknown.getAttributes().removeAttribute(DefaultCapabilityPersonFilter.AGE);

		DefaultCapabilityPersonFilter filter = new DefaultCapabilityPersonFilter(
			new CapabilityConfigGroup(), null, Set.of(), 0);

		assertTrue(filter.accept(child), "a population without ages has to be analysable too");
		assertTrue(filter.accept(unknown));
	}

	@Test
	void takesTheMinimumAgeFromTheConfig() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setMinAge(65);

		Person person = commuter("retiree", new Coord(100, 100), 8 * 3600, 17 * 3600);
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, 30);

		assertFalse(new DefaultCapabilityPersonFilter(config, null).accept(person),
			"the filter should use the minAge of the config it was given");
	}

	@Test
	void everySubpopulationIsKeptIfNoneIsGiven() {
		Person freight = commuter("freight", new Coord(100, 100), 8 * 3600, 17 * 3600);
		PopulationUtils.putSubpopulation(freight, "freight");

		assertTrue(new DefaultCapabilityPersonFilter(new CapabilityConfigGroup(), null, Set.of()).accept(freight));
	}

	/**
	 * A commuter living at the given coordinate and working at (500,500), home before and after.
	 */
	private Person commuter(String id, Coord home, double workStart, double workEnd) {
		Plan plan = plan(id);
		activity(plan, "home", home, -1, workStart - 1800);
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
		activity(plan, "work", new Coord(500, 500), workStart, workEnd);
		plan.addLeg(PopulationUtils.createLeg(TransportMode.car));
		activity(plan, "home", home, workEnd + 1800, -1);
		return plan.getPerson();
	}

	private static Plan plan(String id) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
		person.getAttributes().putAttribute(DefaultCapabilityPersonFilter.AGE, ADULT_AGE);
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		return plan;
	}

	private static void activity(Plan plan, String type, Coord coord, double startTime, double endTime) {
		Activity activity = PopulationUtils.createActivityFromCoord(type, coord);
		if (startTime >= 0) {
			activity.setStartTime(startTime);
		}
		if (endTime >= 0) {
			activity.setEndTime(endTime);
		}
		plan.addActivity(activity);
	}

	private DefaultCapabilityPersonFilter filter() {
		return new DefaultCapabilityPersonFilter(new CapabilityConfigGroup(), studyArea());
	}

	private static DefaultCapabilityPersonFilter withoutStudyArea() {
		return new DefaultCapabilityPersonFilter(new CapabilityConfigGroup(), null);
	}

	/**
	 * Writes the square study area to a shape file and indexes it, the way the run command does.
	 */
	private ShpOptions.Index studyArea() {

		Path file = Path.of(utils.getOutputDirectory(), "study-area.shp");

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder()
			.setName("study-area")
			.setCrs(MGC.getCRS(CRS))
			.addAttribute("name", String.class)
			.create();

		GeoFileWriter.writeGeometries(List.of(factory.createPolygon(new Coordinate[]{
			new Coordinate(0, 0), new Coordinate(1000, 0), new Coordinate(1000, 1000), new Coordinate(0, 1000)
		}, Map.of("name", "study-area"), "study-area")), file.toString());

		return new ShpOptions(file, CRS, null).createIndex(ShpOptions.SAME_CRS, "name");
	}

	private static void assertEqualCoord(Coord expected, Coord actual) {
		assertNotNull(actual);
		assertEquals(expected.getX(), actual.getX(), 1e-9);
		assertEquals(expected.getY(), actual.getY(), 1e-9);
	}
}
