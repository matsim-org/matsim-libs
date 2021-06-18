package org.matsim.urbanEV;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.urbanEV.EVUtils;


import org.matsim.vehicles.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UrbanEVIT {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void run() {
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		overridePopulation(scenario);
		scenario.getVehicles().getVehicles().keySet().forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
		scenario.getConfig().controler().setOutputDirectory("test/output/urbanEV/UrbanEVRun");
		CreateUrbanEVTestScenario.createAndRegisterPersonalCarAndBikeVehicles(scenario);
		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.run();


	}

	private static void overridePopulation(Scenario scenario) {

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("Charge during leisure + bike"));


		Plan plan = factory.createPlan();

		Activity home1 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home1.setEndTime(8 * 3600);
		plan.addActivity(home1);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work1 = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
		work1.setEndTime(10 * 3600);
		plan.addActivity(work1);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work12 = factory.createActivityFromLinkId("work", Id.createLinkId("172"));
		work12.setEndTime(11 * 3600);
		plan.addActivity(work12);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity leisure1 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure1.setEndTime(12 * 3600);
		plan.addActivity(leisure1);

		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity leisure12 = factory.createActivityFromLinkId("leisure", Id.createLinkId("89"));
		leisure12.setEndTime(13 * 3600);
		plan.addActivity(leisure12);

		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity leisure13 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure13.setEndTime(14 * 3600);
		plan.addActivity(leisure13);

		plan.addLeg(factory.createLeg(TransportMode.car));


		Activity home12 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home12.setEndTime(15 * 3600);
		plan.addActivity(home12);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity leisure14 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure14.setEndTime(16 * 3600);
		plan.addActivity(leisure14);


		scenario.getPopulation().addPerson(person);

	}
}