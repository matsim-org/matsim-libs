package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_OFF_STREET_SPOTS;
import static org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingUtils.LINK_ON_STREET_SPOTS;

class PlanBasedParkingCapacityInitializerTest {
	@Test
	void test() {
		Scenario scenario = getScenario();
		Network network = scenario.getNetwork();

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		{
			Plan plan = pf.createPlan();
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			Activity w = pf.createActivityFromLinkId("w", Id.createLinkId("2"));
			plan.addActivity(w);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Person person = pf.createPerson(Id.createPersonId("1"));
			person.addPlan(plan);
			population.addPerson(person);
		}

		{
			Plan plan = pf.createPlan();
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));
			plan.addLeg(pf.createLeg(TransportMode.car));
			Activity w = pf.createActivityFromLinkId("w", Id.createLinkId("2"));
			plan.addActivity(w);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Person person = pf.createPerson(Id.createPersonId("2"));
			person.addPlan(plan);
			population.addPerson(person);
		}

		{
			Plan plan = pf.createPlan();
			Activity h = pf.createActivityFromLinkId("h", Id.createLinkId("1"));
			plan.addActivity(h);
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("w", Id.createLinkId("2")));
			plan.addLeg(pf.createLeg(TransportMode.pt));
			plan.addActivity(pf.createActivityFromLinkId("l", Id.createLinkId("3")));
			plan.addLeg(pf.createLeg(TransportMode.car));
			plan.addActivity(pf.createActivityFromLinkId("h", Id.createLinkId("1")));

			Person person = pf.createPerson(Id.createPersonId("3"));
			person.addPlan(plan);
			population.addPerson(person);
		}

		PlanBasedParkingCapacityInitializer initializer = new PlanBasedParkingCapacityInitializer(network, population, ConfigUtils.createConfig());
		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize = initializer.initialize();

		assertEquals(23, initialize.size());
		assertEquals(new ParkingCapacityInitializer.ParkingInitialCapacity(1, 2), initialize.get(Id.createLinkId("1")));
		assertEquals(new ParkingCapacityInitializer.ParkingInitialCapacity(1, 1), initialize.get(Id.createLinkId("2")));

		for (int i = 3; i < 24; i++) {
			assertEquals(new ParkingCapacityInitializer.ParkingInitialCapacity(1, 0), initialize.get(Id.createLinkId(Integer.toString(i))));
		}

	}

	private static Scenario getScenario() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Link value : scenario.getNetwork().getLinks().values()) {
			value.getAttributes().putAttribute(LINK_ON_STREET_SPOTS, 1);
			value.getAttributes().putAttribute(LINK_OFF_STREET_SPOTS, 0);
		}

		Population pop = scenario.getPopulation();
		pop.removePerson(Id.createPersonId("1"));
		Assertions.assertTrue(pop.getPersons().isEmpty());
		return scenario;
	}

}
