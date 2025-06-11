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

public class ChainedPtFareHandlerTest {

	private final static String FARE_ZONE_TRANSACTION_PARTNER = "fare zone transaction partner";
	private final static String DISTANCE_BASED_TRANSACTION_PARTNER = "distance based transaction partner";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testChainedPtFareHandler() {
		//Prepare
		Config config = getConfig();

		//adapt pt fare
		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);

		FareZoneBasedPtFareParams fareZoneBased = new FareZoneBasedPtFareParams();
		fareZoneBased.setDescription("simple fare zone based");
		// smallest area (Kelheim and villages)
		fareZoneBased.setFareZoneShp(IOUtils.extendUrl(config.getContext(), "ptTestArea/pt-area.shp").toString());
		fareZoneBased.setOrder(1);
		fareZoneBased.setTransactionPartner(FARE_ZONE_TRANSACTION_PARTNER);

		DistanceBasedPtFareParams distanceBased = new DistanceBasedPtFareParams();
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassFareParams =
			distanceBased.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		distanceClassFareParams.setFareSlope(0.00017);
		distanceClassFareParams.setFareIntercept(1.6);
		distanceBased.setOrder(2);
		distanceBased.setTransactionPartner(DISTANCE_BASED_TRANSACTION_PARTNER);
		// larger area Kelheim - Regensburg
		distanceBased.setFareZoneShp(IOUtils.extendUrl(config.getContext(), "pt-area_Kelheim-BadAbbach/pt-area_Kelheim-BadAbbach.shp").toString());
		distanceBased.setMinFare(2.0);

		// second DistanceBasedPtFareParams, e.g. applicable in a different shape file
		DistanceBasedPtFareParams distanceBased2 = new DistanceBasedPtFareParams();
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassFareParams2 =
			distanceBased2.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		distanceClassFareParams2.setFareSlope(0.0003);
		distanceClassFareParams2.setFareIntercept(2.0);
		distanceBased2.setOrder(3);
		distanceBased2.setTransactionPartner(DISTANCE_BASED_TRANSACTION_PARTNER);
		distanceBased2.setMinFare(7.0);

		ptFareConfigGroup.addPtFareParameterSet(fareZoneBased);
		ptFareConfigGroup.addPtFareParameterSet(distanceBased);
		ptFareConfigGroup.addPtFareParameterSet(distanceBased2);

		ptFareConfigGroup.setUpperBoundFactor(2.0);

		// write config to file and read in again to check config reading and writing
		ConfigUtils.writeConfig(config, utils.getOutputDirectory() + "configFromCode.xml");
		Config configFromFile = ConfigUtils.loadConfig(utils.getOutputDirectory() + "configFromCode.xml");
		// re-direct to correct input directory
		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		configFromFile.setContext(context);

		MutableScenario scenario = setUpScenario(configFromFile);

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
		// 1 event per leg plus 1 event fare capping at upper bound factor
		Assertions.assertEquals(5, events.size());

		final String FARE_TEST_PERSON = "fareTestPerson";

		//first event is the fare zone based event
		Assertions.assertEquals(new PersonMoneyEvent(33264, Id.createPersonId(FARE_TEST_PERSON), -1.5, "pt fare", FARE_ZONE_TRANSACTION_PARTNER,
			FARE_TEST_PERSON), events.get(0));

		//second event is the distance based fare of the first DistanceBasedPtFareParams (in area.shp)
		Assertions.assertEquals(new PersonMoneyEvent(51062, Id.createPersonId(FARE_TEST_PERSON), -2.788776056323089, "pt fare",
			DISTANCE_BASED_TRANSACTION_PARTNER, FARE_TEST_PERSON), events.get(1));

		//third event is the distance based fare of the second DistanceBasedPtFareParams (outside area.shp), ca. 5.069 < min fare 7.0
		Assertions.assertEquals(new PersonMoneyEvent(61622, Id.createPersonId(FARE_TEST_PERSON), -7.0, "pt fare",
			DISTANCE_BASED_TRANSACTION_PARTNER, FARE_TEST_PERSON), events.get(2));

		//fourth event is the distance based fare of the second DistanceBasedPtFareParams (outside area.shp)
		Assertions.assertEquals(new PersonMoneyEvent(70354, Id.createPersonId(FARE_TEST_PERSON), -7.961415135189197, "pt fare",
			DISTANCE_BASED_TRANSACTION_PARTNER, FARE_TEST_PERSON), events.get(3));

		//fifth event is the upper bound fare capping compensation
		Assertions.assertEquals(new PersonMoneyEvent(129600, Id.createPersonId(FARE_TEST_PERSON), 3.3273609211338915, "pt fare refund",
			"pt", "Refund for person fareTestPerson"), events.get(4));
	}

	private @NotNull MutableScenario setUpScenario(Config config) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		PopulationFactory fac = population.getFactory();

		Person person = fac.createPerson(Id.createPersonId("fareTestPerson"));
		Plan plan = fac.createPlan();

		Activity home = fac.createActivityFromCoord("home", new Coord(710300.624, 5422165.737));
		// bus to Saal (Donau) work location departs at 09:14
		home.setEndTime(9 * 3600.);
		Activity work = fac.createActivityFromCoord("work", new Coord(714940.65, 5420707.78));
		// rb17 to Gundelshausen 2nd work location departs at 13:59 (in distance fare 1 zone, 6992.797m)
		work.setEndTime(13 * 3600. + 45 * 60);
		Activity work2 = fac.createActivityFromCoord("work", new Coord(719916.02, 5425676.91));
		// rb17 to Regensburg 2nd home location departs at 16:52 (beyond distance fare 1 zone, distance fare 2 applies, 10230.93m)
		work2.setEndTime(16 * 3600. + 40 * 60);
		Activity work3 = fac.createActivityFromCoord("work", new Coord(726645.48, 5433383.16));
		// rb17 to Kelheim home location departs at 18:46, then transfer to bus 6022 (beyond distance fare 1 zone, distance fare 2 applies, 19871
		// .38m)
		work3.setEndTime(18 * 3600. + 40 * 60);
		Activity home2 = fac.createActivityFromCoord("home", new Coord(710300.624, 5422165.737));

		Leg leg = fac.createLeg(TransportMode.pt);

		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);
		plan.addLeg(leg);
		plan.addActivity(work2);
		plan.addLeg(leg);
		plan.addActivity(work3);
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
