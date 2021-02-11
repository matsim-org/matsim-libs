package org.matsim.urbanEV;

import com.sun.xml.bind.v2.TODO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

// TODO translate and complete

/** <b>für (jeden) Agenten:</b>		</b>
		(*) wann wird geladen?	<br>
		(*) wo wird geladen?	<br>
		(*) wird geladen?	<br>
		(*) wie lange wird geladen?	<br>
		(*) wie oft wird geladen?	<br>
		? wird auch bei Fahrzeugwechsel (anderer Mode) geladen?	<br>
		? wird auch 3x geladen?	<br>
		? gleichzeitiges Laden: werden die Fahrzeuge in der richtigen Reihenfolge ein- und ausgestöpselt? (chargingStart und chargingEndEvents) <br>
 	<br>
	<b>für jedes Fahrzeug</b>	<br>
		(*) wird am richtigen charger geladen (charger type / leistung)?	<br>

	<b>generell:</b>	<br>
		Konsistenz zw Plugin and Plugout bzgl <br>
		((*) Ort = Link <br>
		(*) Häufigkeit <br>
		(*) .. <br>
**/
public class UrbanEVTests {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();
	private UrbanEVTestHandler handler;
	private Map<Id<Person>, List<Activity>> plannedActivitiesPerPerson;

	@Before
	public void run(){
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		///controler with Urban EV module

		plannedActivitiesPerPerson = scenario.getPopulation().getPersons().values().stream()
				.collect(Collectors.toMap(p -> p.getId(),
						p -> TripStructureUtils.getActivities(p.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)));
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		handler = new UrbanEVTestHandler();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
		controler.run();
	}
	
	@Test
	public void testAgentsExecuteSameNumberOfActs(){

		boolean fail = false;
		String personsWithDifferingActCount = "";
		for (Map.Entry<Id<Person>, List<Activity>> person2Acts : plannedActivitiesPerPerson.entrySet()) {

			List<ActivityStartEvent> executedActs = handler.normalActStarts.get(person2Acts.getKey());
			if(executedActs.size() != person2Acts.getValue().size() - 1 ){ //first act of the day is not started
				fail = true;
				personsWithDifferingActCount += "\n" + person2Acts.getKey() + " plans " + person2Acts.getValue().size() + " activities and executes " + executedActs.size() + " activities";
			}
		}
		Assert.assertFalse("the following persons do not execute the same amount of activities as they plan to:" + personsWithDifferingActCount, fail);
	}

	@Test
	public void testCarAndBikeAgent(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Charge during leisure + bike"));
		Assert.assertEquals(plugins.size(), 1, 0);

		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 40940d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "113");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Charge during leisure + bike"));
		Assert.assertEquals(plugouts.size(), 1, 0);

		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 61540d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "113");
	}



//	-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	/**
	 *
	 * tests (every iteration) <br>
	 * * location consistency between plugin and plugout <br>
	 * * nrOfPlugins == nrOfPlugouts for each person <br>
	 * * nr of all charging persons <br>
	 * * consistency of chargers between charging start and charging end <br>
	 *
	 */
	private class UrbanEVTestHandler implements ActivityStartEventHandler, ActivityEndEventHandler , ChargingStartEventHandler, ChargingEndEventHandler {

		private Map<Id<Person>, List<ActivityEndEvent>> plugOutCntPerPerson = new HashMap<>();
		private Map<Id<Person>, List<ActivityStartEvent>> plugInCntPerPerson = new HashMap<>();
		private Map<Id<Person>, List<ActivityStartEvent>> normalActStarts = new HashMap<>();
		private Map<Id<ElectricVehicle>, List<ChargingStartEvent>> chargingStarts = new HashMap<>();
		private Map<Id<ElectricVehicle>, List<ChargingEndEvent>> chargingEnds = new HashMap<>();


		@Override
		public void handleEvent(ActivityStartEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION) ){
				compute(plugInCntPerPerson, event);
			} else if (! TripStructureUtils.isStageActivityType(event.getActType())){
				compute(normalActStarts, event);
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION) ){
				plugOutCntPerPerson.compute(event.getPersonId(), (person,list) ->{
					if (list == null) list = new ArrayList<>();
					list.add(event);
					return list;
				});

				ActivityStartEvent correspondingPlugin = this.plugInCntPerPerson.get(event.getPersonId()).get(this.plugInCntPerPerson.get(event.getPersonId()).size() - 1);
				Assert.assertEquals("plugin and plugout location seem not to match. event=" + event, correspondingPlugin.getLinkId(), event.getLinkId());
			}
		}

		@Override
		public void reset(int iteration) {
			if(iteration > 0){
				System.out.println("ITERATION = " + iteration);

				//TODO move the following assert statements out of the simulation loop? Or do we want to explicitly check these _every_ iteration?

				Assert.assertEquals("there should be 4 people plugging in in this test", 4, plugInCntPerPerson.size(), 0);
				Assert.assertEquals("there should be 4 people plugging out this test", 4, plugOutCntPerPerson.size(), 0);
				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size()); //not necessary

				Assert.assertTrue(plugInCntPerPerson.containsKey(Id.createPersonId("Charger Selection long distance leg")));

				for (Id<Person> personId : plugInCntPerPerson.keySet()) {
					Assert.assertTrue("in this test, each agent should only plugin once. agent=" + personId,
							plugInCntPerPerson.get(personId).size() == 1);
					Assert.assertTrue( "each agent should plug in just as often as it plugs out. agent = " + personId,
							plugInCntPerPerson.get(personId).size() == plugOutCntPerPerson.get(personId).size());
				}
			}

			this.plugInCntPerPerson.clear();
			this.plugOutCntPerPerson.clear();
			this.normalActStarts.clear();
		}

		@Override
		public void handleEvent(ChargingEndEvent event) {
			this.chargingEnds.compute(event.getVehicleId(), (person,list) ->{
				if (list == null) list = new ArrayList<>();
				list.add(event);
				return list;
			});

			ChargingStartEvent correspondingStart = this.chargingStarts.get(event.getVehicleId()).get(this.chargingStarts.get(event.getVehicleId()).size() - 1);
			Assert.assertEquals("chargingEnd and chargingStart do not seem not to take place at the same charger. event=" + event, correspondingStart.getChargerId(), event.getChargerId());
		}

		@Override
		public void handleEvent(ChargingStartEvent event) {
			this.chargingStarts.compute(event.getVehicleId(), (person,list) ->{
				if (list == null) list = new ArrayList<>();
				list.add(event);
				return list;
			});
		}
	}

	private void compute(Map<Id<Person>, List<ActivityStartEvent>> map, ActivityStartEvent event) {
		map.compute(event.getPersonId(), (person,list) ->{
			if (list == null) list = new ArrayList<>();
			list.add(event);
			return list;
		});
	}

}