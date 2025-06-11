/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.vsp.pt.ptdisturbances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.config.TransitConfigGroup.BoardingAcceptance;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;
import org.matsim.pt.router.TransitScheduleChangedEvent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

/**
* @author smueller, gleich
 *
 * TODO: Add test that first edits one trip of an agent and then later on edits another one -> notice issues with undefined travel times and similar.
*/

public class EditTripsTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();
	private static final Logger log = LogManager.getLogger(EditTripsTest.class);
	// this is messy, but DisturbanceAndReplanningEngine needs to be static and there is no
	// constructor or similar to pass the replanning time
	private static double testReplanTime = 0;
	private final URL configURL = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("ptdisturbances"),"config.xml");


	/**
	 * Case 1.1.1
	 */
	@Test
	void testAgentStaysAtStop() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils
				.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller()
				.setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 40. * 60;
		Person person = buildPerson(scenario, activityEndTime);

		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 51. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(1490.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(7 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("tr_334", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(4), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(5), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(6), "Trip element has changed");


	}


	/**
	 * Case 1.1.2 Looks awkward in otfvis, because after replanning there suddenly
	 * are 2 testAgents at different places, but events are fine :-/
	 */
	@Test
	void testAgentLeavesStop() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller().setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 15. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 30. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(1344.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(7 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("tr_333", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(4), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(5), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(6), "Trip element has changed");

	}

	/**
	 * Case 1.2.1
	 */
	@Test
	void testAgentStaysInVehicle() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils
				.loadConfig(configURL);;
		String outputDirectory = utils.getOutputDirectory();
		config.controller()
				.setOutputDirectory(outputDirectory);
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 10. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 23. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(1044.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(7 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("tr_332", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(4), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(5), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(6), "Trip element has changed");


	}

	/**
	 * Case 1.2.2
	 */
	@Test
	void testAgentLeavesVehicleAtNextStop() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils
				.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller()
				.setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 20. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 33. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(1077.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(12 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("tr_333", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt7", trip.get(4), "Trip element has changed");
		assertEquals("pt interaction@pt7", trip.get(5), "Trip element has changed");
		assertEquals("pt interaction@pt1", trip.get(6), "Trip element has changed");
		assertEquals("pt interaction@pt1", trip.get(7), "Trip element has changed");
		assertEquals("tr_45", trip.get(8), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(9), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(10), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(11), "Trip element has changed");


	}

	/**
	 * Case 2.1
	 * Does what the name suggests although waiting at the first stop for
	 * the first undisturbed departure leads to boarding the very same bus.
	 * This is due to the utility params for walk, pt_wait and pt being equal/indifferent.
	 */
	@Test
	void testAgentIsAtTeleportLegAndLeavesStop() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller().setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 22. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 25. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(2570.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(9 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(4), "Trip element has changed");
		assertEquals("tr_334", trip.get(5), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(6), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(7), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(8), "Trip element has changed");


	}

	/**
	 * Case 2.2
	 * Same as  {@link #testAgentIsAtTeleportLegAndLeavesStop()} but marginal utility of travelling worse for walk than for
	 * pt_wait and pt. Strangely only works with walk being significantly worse, does not work with small differences.
	 */
	@Test
	void testAgentIsAtTeleportLegAndWaitsAtStop_walkUnattractive() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils
				.loadConfig(configURL);;
		String outputDirectory = utils.getOutputDirectory();
		config.controller()
				.setOutputDirectory(outputDirectory);
		config.scoring().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(
				config.scoring().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - 3);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 22. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 25. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(2570.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(9 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(4), "Trip element has changed");
		assertEquals("tr_334", trip.get(5), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(6), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(7), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(8), "Trip element has changed");


	}

	/**
	 * Case 2.2
	 */
	@Test
	void testAgentIsAtTeleportLegAndWaitsAtStop() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller().setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		double activityEndTime = 7. * 3600 + 22. * 60;
		Person person = buildPerson(scenario, activityEndTime);
		scenario.getPopulation().addPerson(person);
		testReplanTime = 7. * 3600 + 25. * 60;
		run( scenario, false, trips, arrivalTimes );
		double travelTime = arrivalTimes.get(person.getId()) - activityEndTime;
		List<String> trip = trips.get(person.getId());

		assertEquals(2570.0,  travelTime, MatsimTestUtils.EPSILON, "Travel time has changed");
		assertEquals(9 ,trip.size(),"Number of trip elements has changed");

		assertEquals("dummy@car_17bOut", trip.get(0), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(1), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(2), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(3), "Trip element has changed");
		assertEquals("pt interaction@pt6c", trip.get(4), "Trip element has changed");
		assertEquals("tr_334", trip.get(5), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(6), "Trip element has changed");
		assertEquals("pt interaction@pt8", trip.get(7), "Trip element has changed");
		assertEquals("dummy@work0", trip.get(8), "Trip element has changed");


	}

	/**
	 * Case 3
	 * Simulates 900 agents so this case should include every possible state of an agent. Current and future trips are replanned.
	 */
	@Test
	void testOneAgentEveryFourSeconds() {
		HashMap<Id<Person>, Double> arrivalTimes = new HashMap<>();
		HashMap<Id<Person>, List<String>> trips = new HashMap<>();
		Config config = ConfigUtils
				.loadConfig(configURL);
		String outputDirectory = utils.getOutputDirectory();
		config.controller()
				.setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().clear();
		for (int i = 0; i<900 ; i++) {
			Person person = buildPerson(scenario, 7. * 3600 +4 * i);
			scenario.getPopulation().addPerson(person);
		}
		testReplanTime = 7. * 3600 + 33. * 60;
		run( scenario, false, trips, arrivalTimes );
		for (Id<?> personId : arrivalTimes.keySet()) {
			String[] parts = personId.toString().split("_");
			double activityEndTime = Double.parseDouble(parts[1]);
			double travelTime = arrivalTimes.get(personId) - activityEndTime;
			assertTrue(travelTime < 50. * 60);
			assertTrue(travelTime > 15. * 60);

			int numberOfUsedLines = 0;
			for (int ii = 0; ii < trips.get(personId).size(); ii++) {
				if (trips.get(personId).get(ii).startsWith("tr_")) {
					numberOfUsedLines++;
				}
			}
			assertTrue(numberOfUsedLines == 1 || numberOfUsedLines == 2, "Number of used lines ist not plausible");
		}
		System.out.println(config.controller().getOutputDirectory());
	}




	public static Person buildPerson(Scenario scenario, double endTime) {

		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Person person = populationFactory.createPerson(Id.createPersonId("testAgent_"+endTime));
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		Coord coord0 = CoordUtils.createCoord(-10, 1410);
		Activity act0 = populationFactory.createActivityFromLinkId("dummy", Id.createLinkId("car_17bOut"));
		act0.setCoord(coord0);
		act0.setStartTime(0);
		act0.setEndTime(endTime);
		plan.addActivity(act0);
		Leg leg0 = populationFactory.createLeg("pt");
		plan.addLeg(leg0);
		Coord coord1 = CoordUtils.createCoord(2010, 990);
		Activity act1 = populationFactory.createActivityFromCoord("dummy", coord1);
		act1.setEndTime(23. * 3600);
		act1.setLinkId(Id.createLinkId("work0"));
		plan.addActivity(act1);
		return person;
	}



	void run( Scenario scenario, boolean openOTFVis, HashMap<Id<Person>, List<String>> trips, HashMap<Id<Person>, Double> arrivalTimes ) {
		Config config = scenario.getConfig();

		RunExamplePtDisturbances.adaptConfig(config);

		// pt passengers board every line which goes to the desired exit stop instead of
		// only the planned line
		config.transit().setBoardingAcceptance(BoardingAcceptance.checkStopOnly);

		SignalSystemsConfigGroup signalSystemsConfigGroup = adaptConfigForSignals(config);

		buildSignals(scenario, signalSystemsConfigGroup);

		Controler controler = new Controler(scenario);

		Signals.configure( controler ) ;

		// We need to direct to our local DisturbanceAndReplanningEngine, because it is hard to access,
		// so we cannot re-use the one in RunMatsim and configure it via constructor or similar
		{
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule(config,
					QSimComponentsConfigGroup.class);

			// the following requests that a component registered under the name "...NAME"
			// will be used:
			List<String> cmps = qsimComponentsConfig.getActiveComponents();
			cmps.add(DisturbanceAndReplanningEngine.NAME);
			qsimComponentsConfig.setActiveComponents(cmps);

			controler.addOverridingQSimModule(new AbstractQSimModule() {
				@Override
				protected void configureQSim() {
					// the following registers the component under the name "...NAME":
					this.addQSimComponentBinding(DisturbanceAndReplanningEngine.NAME)
							.to(DisturbanceAndReplanningEngine.class);
//					bind(TransitStopHandlerFactory.class).to(SimpleTransitStopHandlerFactory.class);
				}
			});


			HandlerForTests handlerForTests = new HandlerForTests(trips, arrivalTimes);

			controler.addOverridingModule(new AbstractModule(){
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( handlerForTests );
				}
			});


//			controler.addOverridingModule( new SwissRailRaptorModule() );
		}

		// This will print the event onto the console/into the logfile. Sometimes useful
		// for debugging.
//		controler.addOverridingModule( new AbstractModule(){
//			@Override
//			public void install(){
//				this.addEventHandlerBinding().toInstance( new BasicEventHandler(){
//					@Override
//					public void handleEvent( Event event ){
//						log.info( event.toString() );
//					}
//				} );
//			}
//		} );

		if (openOTFVis) {
			// This will start otfvis.
			controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}

		// ---

		controler.run();
	}

	private SignalSystemsConfigGroup adaptConfigForSignals(Config config) {
		// add the signal config group to the config file
		SignalSystemsConfigGroup signalSystemsConfigGroup =
				ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);

		/* the following makes the contrib load the signal input files, but not to do anything with them
		 * (this switch will eventually go away) */
		signalSystemsConfigGroup.setUseSignalSystems(true);

		config.qsim().setUsingFastCapacityUpdate(false); // otherwise inject error message "Fast flow capacity update does not support signals"
		return signalSystemsConfigGroup;
	}

	private void buildSignals(Scenario scenario, SignalSystemsConfigGroup signalSystemsConfigGroup) {
		SignalsData signalsData = SignalUtils.createSignalsData(signalSystemsConfigGroup);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);

		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();
		SignalControlData control = signalsData.getSignalControlData();

		// create signal system pt3 (at node pt3Signal)
		Id<SignalSystem> idSignalSystem = Id.create("pt3", SignalSystem.class);
		SignalSystemData sys = systems.getFactory().createSignalSystemData(idSignalSystem);
		// add signal system pt3 to the overall signal systems container
		systems.addSignalSystemData(sys);
		// create signal 1
		SignalData signal = systems.getFactory().createSignalData(Id.create("1", Signal.class));
		// add signal 1 to signal system pt3, such that it belongs to node 3
		sys.addSignalData(signal);
		// specify the link at which signal 1 is located
		signal.setLinkId(Id.create("pt6b", Link.class));
		// create a single signal group for each signal of system pt3, i.e. for signal 1
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);

		// create a signal control for the system
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(idSignalSystem);
		// add it to the overall signal control container
		control.addSignalSystemControllerData(controller);
		// declare the control as a fixed time control
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);

		/* create a first signal plan for the system control (a signal system control (i.e. an intersection)
		 * can have different (non-overlapping) plans for different times of the day) */
		SignalPlanData plan1 = control.getFactory().createSignalPlanData(Id.create("1", SignalPlan.class));
		// add the (first) plan to the system control
		controller.addSignalPlanData(plan1);
		// fill the plan with information: cycle time, offset, signal settings
		// cycle time, start time and offset interact, wrong combinations lead to the signal remaining switched off. Better don't touch - gl jul'19
		int startTime = 7 * 3600 + 30 * 60 + 120; // let 7:30 departure pass the signal for testAgentLeavesVehicleAtNextStop()
		int endTime = 8 * 3600 - 1;
		plan1.setStartTime(startTime * 1.0);
		/* note: use start and end time as 0.0 if you want to define a signal plan that is valid all day. */
		plan1.setEndTime(endTime * 1.0);
		plan1.setCycleTime(100000);
		plan1.setOffset(startTime);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(Id.create("1", SignalGroup.class));
		plan1.addSignalGroupSettings(settings1);
		settings1.setOnset(0);
		settings1.setDropping(1);
	}

	private static class DisturbanceAndReplanningEngine implements MobsimEngine {
		public static final String NAME = "disturbanceAndReplanningEngine";

		@Inject
		private Scenario scenario;
		@Inject
		private EventsManager events;
		@Inject
		private Provider<TripRouter> tripRouterProvider;
		private InternalInterface internalInterface;

		@Override
		public void doSimStep(double now) {

			// replan after an affected bus has already departed -> pax on the bus are
			// replanned to get off earlier
			double replanTime = testReplanTime;

			if ((int) now == replanTime - 1.) { // yyyyyy this needs to come one sec earlier. :-(
				// clear transit schedule from transit router provider:
				events.processEvent(new TransitScheduleChangedEvent(now));
			}

			if ((int) now == replanTime) {

				// modify transit schedule:

				final Id<TransitLine> disturbedLineId = Id.create("2", TransitLine.class);
				TransitLine disturbedLine = scenario.getTransitSchedule().getTransitLines().get(disturbedLineId);
				Gbl.assertNotNull(disturbedLine);

				TransitRoute disturbedRoute = disturbedLine.getRoutes().get(Id.create("345", TransitRoute.class));
				Gbl.assertNotNull(disturbedRoute);

				log.warn("before removal: nDepartures=" + disturbedRoute.getDepartures().size());

				List<Departure> toRemove = new ArrayList<>();
				for (Departure departure : disturbedRoute.getDepartures().values()) {
					if (departure.getDepartureTime() >= 7.5 * 3600. && departure.getDepartureTime() < 8. * 3600.) {
						toRemove.add(departure);
					}
				}
				for (Departure departure : toRemove) {
					disturbedRoute.removeDeparture(departure);
				}

				log.warn("after removal: nDepartures=" + disturbedRoute.getDepartures().size());

				// ---

				RunExamplePtDisturbances.replanPtPassengers(now, disturbedLineId, tripRouterProvider, scenario, internalInterface);

			}
		}

		@Override
		public void onPrepareSim() {
		}

		@Override
		public void afterSim() {
		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {
			this.internalInterface = internalInterface;
		}

	}

	private static class HandlerForTests implements ActivityStartEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler
	{
		private HashMap<Id<Person>, List<String>> trips;
		private HashMap<Id<Person>, Double> arrivalTimes;

		public HandlerForTests (HashMap<Id<Person>, List<String>> trips, HashMap<Id<Person>, Double> arrivalTimes) {
			this.trips = trips;
			this.arrivalTimes = arrivalTimes;
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (event.getPersonId().toString().startsWith("testAgent")) {
				if (!trips.containsKey(event.getPersonId())) {
					List<String> trip = new ArrayList<>();
					trip.add(event.getActType()+"@"+event.getLinkId());
					trips.put(event.getPersonId(), trip);
				}

				else {
					List<String> trip = trips.get(event.getPersonId());
					trip.add(event.getActType()+"@"+event.getLinkId());
				}
			}

		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			arrivalTimes.put(event.getPersonId(), event.getTime());
			if (event.getPersonId().toString().startsWith("testAgent")) {
				List<String> trip = trips.get(event.getPersonId());
				trip.add(event.getActType()+"@"+event.getLinkId());
			}

		}


		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			if (event.getPersonId().toString().startsWith("testAgent")) {
				List<String> trip = trips.get(event.getPersonId());
				trip.add(event.getVehicleId().toString());
			}

		}

		@Override
		public void reset(int iteration) {
			arrivalTimes.clear();
			trips.clear();
		}

	}



}
