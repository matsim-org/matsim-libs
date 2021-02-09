package org.matsim.urbanEV;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChargerSelectionTest {

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
	public void testSameNumberOfActs(){

		boolean fail = false;
		String personsWithDifferingActCound = "";
		for (Map.Entry<Id<Person>, List<Activity>> person2Acts : plannedActivitiesPerPerson.entrySet()) {

			List<ActivityStartEvent> executedActs = handler.actStarts.get(person2Acts.getKey());
			if(executedActs.size() != person2Acts.getValue().size() - 1 ){ //first act of the day is not started
				fail = true;
				personsWithDifferingActCound += "\n" + person2Acts.getKey() + " plans " + person2Acts.getValue().size() + " activities and executes " + executedActs.size() + " activities";
			}
		}
		Assert.assertFalse("the following persons do not execute the same amount of activities as they plan to:" + personsWithDifferingActCound, fail);


	}



	private class UrbanEVTestHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

		private Map<Id<Person>, Double> plugOutCntPerPerson = new HashMap<>();
		private Map<Id<Person>, Double> plugInCntPerPerson = new HashMap<>();
		private Map<Id<Person>, List<ActivityStartEvent>> actStarts = new HashMap<>();

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION) ){
				this.plugOutCntPerPerson.compute(event.getPersonId(), (person,count) -> count == null ? 1 : count + 1);
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION) ){
				this.plugInCntPerPerson.compute(event.getPersonId(), (person,count) -> count == null ? 1 : count + 1);
			} else if (! TripStructureUtils.isStageActivityType(event.getActType())){
				this.actStarts.compute(event.getPersonId(), (person,list) ->{
					if (list == null) list = new ArrayList<>();
					list.add(event);
					return list;
				});
			}
		}

		@Override
		public void reset(int iteration) {
			if(iteration > 0){
				System.out.println("ITERATION = " + iteration);

				Assert.assertEquals("there should be 4 people plugging in in this test", 4, plugInCntPerPerson.size(), 0);
				Assert.assertEquals("there should be 4 people plugging out this test", 4, plugOutCntPerPerson.size(), 0);
				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size()); //not necessary

				Assert.assertTrue(plugInCntPerPerson.containsKey(Id.createPersonId("Charger Selection long distance leg")));

				for (Id<Person> personId : plugInCntPerPerson.keySet()) {
					Assert.assertTrue("in this test, each agent should only plugin once. agent=" + personId,
							plugInCntPerPerson.get(personId) == 1);
					Assert.assertTrue( "each agent should plug in just as often as it plugs out. agent = " + personId,
							plugInCntPerPerson.get(personId).doubleValue() == plugOutCntPerPerson.get(personId).doubleValue());
				}
			}

			this.plugInCntPerPerson.clear();
			this.plugOutCntPerPerson.clear();
			this.actStarts.clear();
		}
	}


}