package org.matsim.core.scenario.checkers;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VspScenarioCheckerImplTest {

	@Test
	void doesNotComplainWhenNoFacilitiesPresent() {
		Scenario scenario = createScenarioWithAbortLevel();
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void doesNotComplainWhenActivityMatchesFacilityLinkAndCoord() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<Link> linkId = Id.createLinkId("link1");
		Coord coord = new Coord(100, 200);
		Id<ActivityFacility> facilityId = Id.create("fac1", ActivityFacility.class);

		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		ActivityFacility facility = factory.createActivityFacility(facilityId, coord, linkId);
		scenario.getActivityFacilities().addActivityFacility(facility);

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", coord);
		act.setLinkId(linkId);
		act.setFacilityId(facilityId);
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertDoesNotThrow(() -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenActivityLinkDiffersFromFacilityLink() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<Link> facilityLinkId = Id.createLinkId("link1");
		Id<Link> activityLinkId = Id.createLinkId("link2");
		Coord coord = new Coord(100, 200);
		Id<ActivityFacility> facilityId = Id.create("fac1", ActivityFacility.class);

		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		ActivityFacility facility = factory.createActivityFacility(facilityId, coord, facilityLinkId);
		scenario.getActivityFacilities().addActivityFacility(facility);

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", coord);
		act.setLinkId(activityLinkId);
		act.setFacilityId(facilityId);
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenActivityCoordDiffersFromFacilityCoord() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<Link> linkId = Id.createLinkId("link1");
		Coord facilityCoord = new Coord(100, 200);
		Coord activityCoord = new Coord(999, 888);
		Id<ActivityFacility> facilityId = Id.create("fac1", ActivityFacility.class);

		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		ActivityFacility facility = factory.createActivityFacility(facilityId, facilityCoord, linkId);
		scenario.getActivityFacilities().addActivityFacility(facility);

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", activityCoord);
		act.setLinkId(linkId);
		act.setFacilityId(facilityId);
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenActivityHasNoFacilityIdButFacilitiesAreLoaded() {
		Scenario scenario = createScenarioWithAbortLevel();
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		scenario.getActivityFacilities().addActivityFacility(
			factory.createActivityFacility(Id.create("fac1", ActivityFacility.class), new Coord(0, 0), Id.createLinkId("l")));

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
		// no facilityId set
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenActivityHasNoLinkId() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<ActivityFacility> facilityId = Id.create("fac1", ActivityFacility.class);
		Coord coord = new Coord(100, 200);

		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		scenario.getActivityFacilities().addActivityFacility(
			factory.createActivityFacility(facilityId, coord, Id.createLinkId("link1")));

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", coord);
		act.setFacilityId(facilityId);
		// no linkId set on activity
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenFacilityHasNoLinkId() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<ActivityFacility> facilityId = Id.create("fac1", ActivityFacility.class);
		Coord coord = new Coord(100, 200);

		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		// create facility without linkId
		scenario.getActivityFacilities().addActivityFacility(
			factory.createActivityFacility(facilityId, coord));

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", coord);
		act.setLinkId(Id.createLinkId("link1"));
		act.setFacilityId(facilityId);
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	@Test
	void abortsWhenActivityReferencesMissingFacility() {
		Scenario scenario = createScenarioWithAbortLevel();
		Id<ActivityFacility> facilityId = Id.create("nonexistent", ActivityFacility.class);

		// add a different facility so the facilities container is non-empty
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		scenario.getActivityFacilities().addActivityFacility(
			factory.createActivityFacility(Id.create("other", ActivityFacility.class), new Coord(0, 0), Id.createLinkId("l")));

		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p1"));
		PopulationUtils.putSubpopulation(person, "person");
		Plan plan = PopulationUtils.createPlan();
		Activity act = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
		act.setFacilityId(facilityId);
		plan.addActivity(act);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);

		VspScenarioCheckerImpl checker = new VspScenarioCheckerImpl();
		assertThrows(RuntimeException.class, () -> checker.checkConsistencyBeforeRun(scenario));
	}

	private static Scenario createScenarioWithAbortLevel() {
		Config config = ConfigUtils.createConfig();
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.scoring().getOrCreateScoringParameters("person").getOrCreateActivityParams("home");
		return ScenarioUtils.createScenario(config);
	}
}
