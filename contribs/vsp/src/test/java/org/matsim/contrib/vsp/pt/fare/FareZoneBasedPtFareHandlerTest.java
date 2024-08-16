package org.matsim.contrib.vsp.pt.fare;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FareZoneBasedPtFareHandlerTest {

	private final static String FARE_ZONE_TRANSACTION_PARTNER = "fare zone transaction partner";
	private final static String DISTANCE_BASED_TRANSACTION_PARTNER = "distance based transaction partner";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testFareZoneBasedPtFareHandler() {
		//Prepare
		Config config = getConfig();

		//adapt pt fare
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);

		FareZoneBasedPtFareParams fareZoneBased = new FareZoneBasedPtFareParams();
		fareZoneBased.setDescription("simple fare zone based");
		fareZoneBased.setFareZoneShp(IOUtils.extendUrl(config.getContext(), "ptTestArea/pt-area.shp").toString());
		fareZoneBased.setOrder(1);
		fareZoneBased.setTransactionPartner(FARE_ZONE_TRANSACTION_PARTNER);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassFareParams =
			distanceBased.getOrCreateDistanceClassFareParams(999_999_999.);
		distanceClassFareParams.setFareSlope(0.00017);
		distanceClassFareParams.setFareIntercept(1.6);
		distanceBased.addParameterSet(distanceClassFareParams);
		distanceBased.setOrder(2);
		distanceBased.setTransactionPartner(DISTANCE_BASED_TRANSACTION_PARTNER);

		ptFareConfigGroup.addParameterSet(fareZoneBased);
		ptFareConfigGroup.addParameterSet(distanceBased);

		MutableScenario scenario = setUpScenario(config);

		//Run
		var fareAnalysis = new FareAnalysis();

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new PtFareModule());
				addEventHandlerBinding().toInstance(fareAnalysis);
			}
		});
		controler.run();

		//Check
		List<PersonMoneyEvent> events = fareAnalysis.getEvents();
		Assertions.assertEquals(2, events.size());

		final String FARE_TEST_PERSON = "fareTestPerson";

		//first event is the fare zone based event
		Assertions.assertEquals(new PersonMoneyEvent(33264, Id.createPersonId(FARE_TEST_PERSON), -1.5, "pt fare", FARE_ZONE_TRANSACTION_PARTNER,
			FARE_TEST_PERSON), events.get(0));

		//second event is the distance based event
		Assertions.assertEquals(new PersonMoneyEvent(52056, Id.createPersonId(FARE_TEST_PERSON), -4.526183060514956, "pt fare",
			DISTANCE_BASED_TRANSACTION_PARTNER, FARE_TEST_PERSON), events.get(1));
	}

	private @NotNull MutableScenario setUpScenario(Config config) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		PopulationFactory fac = population.getFactory();

		Person person = fac.createPerson(Id.createPersonId("fareTestPerson"));
		Plan plan = fac.createPlan();

		Activity home = fac.createActivityFromCoord("home", new Coord(710300.624, 5422165.737));
//		bus to Saal (Donau) work location departs at 09:14
		home.setEndTime(9 * 3600.);
		Activity work = fac.createActivityFromCoord("work", new Coord(714940.65, 5420707.78));
//		rb17 to regensburg 2nd home location departs at 13:59
		work.setEndTime(13 * 3600. + 45 * 60);
		Activity home2 = fac.createActivityFromCoord("home", new Coord(726634.40, 5433508.07));

		Leg leg = fac.createLeg(TransportMode.pt);

		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);
		plan.addLeg(leg);
		plan.addActivity(home2);

		person.addPlan(plan);
		population.addPerson(person);
		scenario.setPopulation(population);
		return scenario;
	}

	private @NotNull Config getConfig() {
		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		ScoringConfigGroup scoring = ConfigUtils.addOrGetModule(config, ScoringConfigGroup.class);

		ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
		ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work");
		homeParams.setTypicalDuration(8 * 3600.);
		workParams.setTypicalDuration(8 * 3600.);
		scoring.addActivityParams(homeParams);
		scoring.addActivityParams(workParams);
		return config;
	}

	private static class FareAnalysis implements PersonMoneyEventHandler {
		private final List<PersonMoneyEvent> events = new ArrayList<>();

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}

		public List<PersonMoneyEvent> getEvents() {
			return events;
		}
	}
}
