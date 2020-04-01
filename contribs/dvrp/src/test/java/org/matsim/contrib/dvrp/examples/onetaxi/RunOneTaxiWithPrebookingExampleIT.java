/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.core.mobsim.qsim.ActivityEngineWithWakeup.AgentWakeupEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.ActivityEngineWithWakeup;
import org.matsim.core.mobsim.qsim.PreplanningEngine;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class RunOneTaxiWithPrebookingExampleIT {

	private static final Logger log = Logger.getLogger(RunOneTaxiWithPrebookingExampleIT.class );

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRun() {
		// load config
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "generic_dvrp_one_taxi_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.controler().setLastIteration(0);

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		{
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
			List<String> components = qsimComponentsConfig.getActiveComponents();
			components.remove( ActivityEngineModule.COMPONENT_NAME );
			components.add( ActivityEngineWithWakeup.COMPONENT_NAME );
//			components.add(  PreplanningEngineQSimModule.COMPONENT_NAME ); // is in dvrp defaults
			qsimComponentsConfig.setActiveComponents( components );
		}

		// ---
		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			person.getSelectedPlan().getAttributes().putAttribute(PreplanningEngine.PREBOOKING_OFFSET_ATTRIBUTE_NAME, 900.);
		}

//		PopulationUtils.writePopulation(scenario.getPopulation(), utils.getOutputDirectory() + "/../pop.xml");

		// ---
		// setup controler
		Controler controler = new Controler(scenario);

		// this is essentially config:
		controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.taxi ) ) ;

		// add bindings:
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				install( new DvrpModule() );
				install( new OneTaxiModule(ConfigGroup.getInputFileURL(config.getContext(), "one_taxi_vehicles.xml") ) );
				installQSimModule( new AbstractQSimModule(){
					@Override protected void configureQSim(){
						this.addQSimComponentBinding( ActivityEngineWithWakeup.COMPONENT_NAME ).to(ActivityEngineWithWakeup.class ).in( Singleton.class );
//						this.addQSimComponentBinding(DynActivityEngine.COMPONENT_NAME).to(DynActivityEngine.class);
					}
				} );
			}
		} ) ;

		// add observers for the test:
		List<AgentWakeupEvent> wakeupEvents = Collections.synchronizedList(new ArrayList<>());
		List<PassengerRequestScheduledEvent> requestScheduledEvents = Collections.synchronizedList(new ArrayList<>());
		List<ActivityEndEvent> activityEndEvents = Collections.synchronizedList(new ArrayList<>());
		Multimap<Id<Person>, Event> eventsByPassenger = ArrayListMultimap.create();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance((BasicEventHandler)event -> {
					if (event instanceof AgentWakeupEvent) {
						wakeupEvents.add((AgentWakeupEvent)event);
					} else if (event instanceof PassengerRequestScheduledEvent) {
						requestScheduledEvents.add((PassengerRequestScheduledEvent)event);
					} else if (event instanceof ActivityEndEvent && ((ActivityEndEvent)event).getActType().equals("dummy")) {
						activityEndEvents.add((ActivityEndEvent)event);
					}
					if (event instanceof HasPersonId) {
						Id<Person> personId = ((HasPersonId)event).getPersonId();
						if (personId.toString().contains("passenger")) {
							eventsByPassenger.put(personId, event);
						}
					}
				});
			}
		});

		// run simulation
		controler.run();

		// check results
		eventsByPassenger.keySet()
				.stream()
				.sorted()
				.flatMap(personId -> eventsByPassenger.get(personId).stream())
				.forEach(System.err::println);

		//<leg mode="taxi" dep_time="00:00:00" trav_time="00:01:00" arr_time="00:01:00">
		assertWakeupEvent(wakeupEvents.get(0), 0, "passenger_0");
		assertRequestScheduledEvent(requestScheduledEvents.get(0), "passenger_0", 61.66, "taxi_0");
		assertActivityEndEvent(activityEndEvents.get(0), 2, "passenger_0");

		//<leg mode="taxi" dep_time="00:05:00" trav_time="00:01:00" arr_time="00:06:00">
		assertWakeupEvent(wakeupEvents.get(1), 0, "passenger_1");
		assertRequestScheduledEvent(requestScheduledEvents.get(1), "passenger_1", 567.0, "taxi_1");
		assertActivityEndEvent(activityEndEvents.get(1), 300, "passenger_1");

		//<leg mode="taxi" dep_time="00:10:00" trav_time="00:01:00" arr_time="00:11:00">
		assertWakeupEvent(wakeupEvents.get(2), 0, "passenger_2");
		assertRequestScheduledEvent(requestScheduledEvents.get(2), "passenger_2", 954.33, "taxi_2");
		assertActivityEndEvent(activityEndEvents.get(2), 600, "passenger_2");

		//<leg mode="taxi" dep_time="00:15:00" trav_time="00:01:00" arr_time="00:16:00">
		assertWakeupEvent(wakeupEvents.get(3), 1, "passenger_3");
		assertRequestScheduledEvent(requestScheduledEvents.get(3), "passenger_3", 1402.66, "taxi_3");
//		assertRequestScheduledEvent(requestScheduledEvents.get(3), "passenger_3", /*1402.66*/ 1401.66, "taxi_3");
		// (exchanging the sequence of binding of PreplanningEngineQSimModule and DynActivityEngine changes the results :-(. kai, jan'20 (*))
		assertActivityEndEvent(activityEndEvents.get(3), 900, "passenger_3");

		//<leg mode="taxi" dep_time="00:20:00" trav_time="00:01:00" arr_time="00:21:00">
		assertWakeupEvent(wakeupEvents.get(4), 301, "passenger_4");
		assertRequestScheduledEvent(requestScheduledEvents.get(4), "passenger_4", 1977.8, "taxi_4"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(4), "passenger_4", /*1977.8*/ 1976.8, "taxi_4"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(4), 1200, "passenger_4");

		//<leg mode="taxi" dep_time="00:25:00" trav_time="00:01:00" arr_time="00:26:00">
		assertWakeupEvent(wakeupEvents.get(5), 601, "passenger_5");
		assertRequestScheduledEvent(requestScheduledEvents.get(5), "passenger_5", 2503.46, "taxi_5"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(5), "passenger_5", /*2503.46*/ 2502.46, "taxi_5"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(5), 1500, "passenger_5");

		//<leg mode="taxi" dep_time="00:30:00" trav_time="00:01:00" arr_time="00:31:00">
		assertWakeupEvent(wakeupEvents.get(6), 901, "passenger_6");
		assertRequestScheduledEvent(requestScheduledEvents.get(6), "passenger_6", 2932.46, "taxi_6"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(6), "passenger_6", /*2932.46*/ 2931.46, "taxi_6"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(6), 1800, "passenger_6");

		//<leg mode="taxi" dep_time="00:35:00" trav_time="00:01:00" arr_time="00:36:00">
		assertWakeupEvent(wakeupEvents.get(7), 1201, "passenger_7");
		assertRequestScheduledEvent(requestScheduledEvents.get(7), "passenger_7", 3317.46, "taxi_7"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(7), "passenger_7", /*3317.46*/ 3316.46, "taxi_7"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(7), 2100, "passenger_7");

		//<leg mode="taxi" dep_time="00:40:00" trav_time="00:01:00" arr_time="00:41:00">
		assertWakeupEvent(wakeupEvents.get(8), 1501, "passenger_8");
		assertRequestScheduledEvent(requestScheduledEvents.get(8), "passenger_8", 3944.86, "taxi_8"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(8), "passenger_8", /*3944.86*/ 3943.86, "taxi_8"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(8), 2400, "passenger_8");

		//<leg mode="taxi" dep_time="00:45:00" trav_time="00:01:00" arr_time="00:46:00">
		assertWakeupEvent(wakeupEvents.get(9), 1801, "passenger_9");
		assertRequestScheduledEvent(requestScheduledEvents.get(9), "passenger_9", 4333.53, "taxi_9"); // see (*)
//		assertRequestScheduledEvent(requestScheduledEvents.get(9), "passenger_9", /*4333.53*/ 4332.53, "taxi_9"); // see (*)
		assertActivityEndEvent(activityEndEvents.get(9), 2700, "passenger_9");
	}

	private static void assertWakeupEvent( AgentWakeupEvent event, double time, String personId ) {
		assertThat(event.getTime()).isEqualTo(time);
		assertThat(event.getPersonId().toString()).isEqualTo(personId);
	}

	private static void assertRequestScheduledEvent( PassengerRequestScheduledEvent event, String personId, double pickupTime, String requestId ) {
		assertThat(event.getVehicleId().toString()).isEqualTo("taxi_one");
		assertThat(event.getPickupTime()).isCloseTo(pickupTime, Offset.offset(0.01));
		assertThat(event.getPersonId().toString()).isEqualTo(personId);
		assertThat(event.getRequestId().toString()).isEqualTo(requestId);
	}

	private static void assertActivityEndEvent( ActivityEndEvent event, double time, String personId ) {
		assertThat(event.getTime()).isEqualTo(time);
		assertThat(event.getPersonId().toString()).isEqualTo(personId);
	}
}
