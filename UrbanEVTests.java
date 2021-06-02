package org.matsim.urbanEV;

import com.sun.xml.bind.v2.TODO;
import org.junit.*;
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
 		? nicht Lader <br>
 		? zu kurze Ladezeit/falsche Aktivitätentypen <br>
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
	private static UrbanEVTestHandler handler;
	private static Map<Id<Person>, List<Activity>> plannedActivitiesPerPerson;

	@BeforeClass
	public static void run(){
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		///controler with Urban EV module

		plannedActivitiesPerPerson = scenario.getPopulation().getPersons().values().stream()
				.collect(Collectors.toMap(p -> p.getId(),
						p -> TripStructureUtils.getActivities(p.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)));
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		handler = new UrbanEVTestHandler();
		controler.addOverridingModule(new AbstractModule() {
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
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 40491d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "90");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Charge during leisure + bike"));
		Assert.assertEquals(plugouts.size(), 1, 0);

		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 50400d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "90");
	}

	@Test
	public void testTripleCharger(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Triple Charger"));
		Assert.assertEquals(plugins.size(), 3, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 2982d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "90");

		ActivityStartEvent pluginActStart2 = plugins.get(1);
		Assert.assertEquals("wrong charging start time", pluginActStart2.getTime(), 8719d, MatsimTestUtils.EPSILON );
		Assert.assertEquals("wrong charging start location", pluginActStart2.getLinkId().toString(), "90");

		ActivityStartEvent pluginActStart3 = plugins.get(2);
		Assert.assertEquals("wrong charging start time", pluginActStart3.getTime(), 14456d, MatsimTestUtils.EPSILON );
		Assert.assertEquals("wrong charging start location", pluginActStart3.getLinkId().toString(), "90");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Triple Charger"));
		Assert.assertEquals(plugouts.size(), 3, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 4069d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "90");

		ActivityEndEvent plugoutActStart2 = plugouts.get(1);
		Assert.assertEquals("wrong charging end time", plugoutActStart2.getTime(), 9805d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart2.getLinkId().toString(), "90");

		ActivityEndEvent plugoutActStart3 = plugouts.get(2);
		Assert.assertEquals("wrong charging end time", plugoutActStart3.getTime(), 15542d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart3.getLinkId().toString(), "90");



	}

	@Test
	public void testChargerSelectionShopping(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Charging during shopping"));
		Assert.assertEquals(plugins.size(), 1, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 44309d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "172");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Charging during shopping"));
		Assert.assertEquals(plugouts.size(), 1, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 48404d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "172");


	}
	@Test
	public void testLongDistance(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Charger Selection long distance leg"));
		Assert.assertEquals(plugins.size(), 1, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 36459d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "89");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Charger Selection long distance leg"));
		Assert.assertEquals(plugouts.size(), 1, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 58519d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "89");
	}
	@Test
	public void testTwin(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Charger Selection long distance twin"));
		Assert.assertEquals(plugins.size(), 1, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 36099, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "89");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Charger Selection long distance twin"));
		Assert.assertEquals(plugouts.size(), 1, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 58159d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "89");
	}
	@Test
	public void testDoubleCharger(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Double Charger"));
		Assert.assertEquals(plugins.size(), 2, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 23283d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "90");

		ActivityStartEvent pluginActStart2 = plugins.get(1);
		Assert.assertEquals("wrong charging start time", pluginActStart2.getTime(), 40004d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart2.getLinkId().toString(), "90");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Double Charger"));
		Assert.assertEquals(plugouts.size(), 2, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 28783d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "90");

		ActivityEndEvent plugoutActStart2 = plugouts.get(1);
		Assert.assertEquals("wrong charging end time", plugoutActStart2.getTime(), 44668d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart2.getLinkId().toString(), "90");
	}

	@Test
	public void testNotEnoughTimeCharger(){
	List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Not enough time Charger"));
	Assert.assertEquals(plugins.size(),1,0);
	ActivityStartEvent pluginActStart = plugins.get(0);
	Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 24942d, MatsimTestUtils.EPSILON);
	Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "89");

	List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Not enough time Charger"));
	Assert.assertEquals(plugouts.size(), 1, 0);
	ActivityEndEvent plugoutActStart = plugouts.get(0);
	Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 47729d, MatsimTestUtils.EPSILON);
	Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "89");
	}

	@Test
	public void testHomeCharger(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Home Charger"));
		Assert.assertEquals(plugins.size(),1,0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 6225d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "91");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Home Charger"));
		Assert.assertEquals("Home charge should not have a plug out interaction", plugouts, null);
	}

	@Test
	public void testDoubleChargerHomeCharger(){
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.get(Id.createPersonId("Double Charger Home Charger"));
		Assert.assertEquals(plugins.size(),2,0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assert.assertEquals("wrong charging start time", pluginActStart.getTime(), 19683d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart.getLinkId().toString(), "91");

		ActivityStartEvent pluginActStart2 = plugins.get(1);
		Assert.assertEquals("wrong charging start time", pluginActStart2.getTime(), 38444d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging start location", pluginActStart2.getLinkId().toString(), "90");


		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.get(Id.createPersonId("Double Charger Home Charger"));
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assert.assertEquals("wrong charging end time", plugoutActStart.getTime(), 23891d, MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong charging end location", plugoutActStart.getLinkId().toString(), "91");
		Assert.assertEquals("Should only plug out once ", plugouts.size(),1);
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
	private static class UrbanEVTestHandler implements ActivityStartEventHandler, ActivityEndEventHandler , ChargingStartEventHandler, ChargingEndEventHandler {

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

				Assert.assertEquals("there should be 9 people plugging in in this test", 9, plugInCntPerPerson.size(), 0);
				Assert.assertEquals("there should be 8 people plugging out this test", 8, plugOutCntPerPerson.size(), 0);
//				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size()); //not necessary

				Assert.assertTrue(plugInCntPerPerson.containsKey(Id.createPersonId("Charger Selection long distance leg")));

				//	The number of plug in and outs is not equal anymore since we added homecharging

//				for (Id<Person> personId : plugInCntPerPerson.keySet()) {
//					Assert.assertTrue("in this test, each agent should only plugin once. agent=" + personId,
//							plugInCntPerPerson.get(personId).size() >= 1);
//					Assert.assertTrue( "each agent should plug in just as often as it plugs out. agent = " + personId,
//							plugInCntPerPerson.get(personId).size() == plugOutCntPerPerson.get(personId).size());
//				}
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

	private static void compute(Map<Id<Person>, List<ActivityStartEvent>> map, ActivityStartEvent event) {
		map.compute(event.getPersonId(), (person,list) ->{
			if (list == null) list = new ArrayList<>();
			list.add(event);
			return list;
		});
	}

}