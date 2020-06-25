/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class IterationTravelStatsControlerListenerTest {

	final IdMap<Person, Plan> map = new IdMap<>(Person.class);
	Config config = ConfigUtils.createConfig();
	
	private static int person;
	private static int first_act_x;
	private static int first_act_y;
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testIterationTravelStatsControlerListener() {
		
		Plans plans = new Plans();

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/

		Plan plan1 = plans.createPlanOne();

		/********************************
		 * Plan 2 - creating plan 2
		 ********************************/
		Plan plan2 = plans.createPlanTwo();

		/*****************************
		 * Plan 3 - creating plan 3
		 ************************************/
		Plan plan3 = plans.createPlanThree();

		/************************
		 * Plan 4-----creating plan 4
		 **************************************/
		Plan plan4 = plans.createPlanFour();
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Person person1 = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));;
		person1.addPlan(plan1);
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));;
		person2.addPlan(plan2);
		Person person3 = PopulationUtils.getFactory().createPerson(Id.create("3", Person.class));;
		person3.addPlan(plan3);
		Person person4 = PopulationUtils.getFactory().createPerson(Id.create("4", Person.class));;
		person4.addPlan(plan4);
		
		scenario.getPopulation().addPerson(person1);
		scenario.getPopulation().addPerson(person2);
		scenario.getPopulation().addPerson(person3);
		scenario.getPopulation().addPerson(person4);
		
		performTest(scenario, utils.getOutputDirectory() + "/IterationTravelStatsControlerListener");
	}
	
	private void performTest(Scenario scenario, String outputDirectory) {
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		ShutdownEvent shutdownEvent = new ShutdownEvent(null, false);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new IterationTravelStatsModule());
				install(new ScenarioByInstanceModule(scenario));
				install(new ExperiencedPlansModule());
				bind(OutputDirectoryHierarchy.class).asEagerSingleton();
				//bind(ExperiencedPlansService.class).to(ExperiencedPlansServiceImpl.class);
				bind(IterationTravelStatsControlerListener.class).asEagerSingleton();
				bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
			}
		});
		IterationTravelStatsControlerListener ltcl = injector.getInstance(IterationTravelStatsControlerListener.class);
		ltcl.notifyShutdown(shutdownEvent);
		Map<Id<Person>, List<String>> firstActivity = identifyFirstActivityLocation(scenario);
		readAndValidateValues(firstActivity);
	}
	
	private Map<Id<Person>, List<String>> identifyFirstActivityLocation(Scenario scenario) {
		Map<Id<Person>, List<String>> firstActivity = new HashMap<Id<Person>, List<String>>();
		 for (Person p : scenario.getPopulation().getPersons().values()) {
			Id<Person> id = p.getId();
			List<String> coordinates = new ArrayList<>();
			 Activity firstAct = (Activity) p.getSelectedPlan().getPlanElements().get(0);
			 String x = Double.toString(firstAct.getCoord().getX());
			 String y = Double.toString(firstAct.getCoord().getY());
			 coordinates.add(x);
			 coordinates.add(y);
			 firstActivity.put(id, coordinates);
		 }
		 return firstActivity;
	}
	
	private void readAndValidateValues(Map<Id<Person>, List<String>> firstActivity) {

		String file = utils.getOutputDirectory() + "/output_persons.csv.gz";
		BufferedReader br;
		String line;
		try {
			GZIPInputStream input = new GZIPInputStream(new FileInputStream(file));
			Reader decoder = new InputStreamReader(input);
			br = new BufferedReader(decoder);
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split(";");
			decideColumns(columnNames);
			while ((line = br.readLine()) != null) {
					String[] column = line.split(";");
					// checking if column number in greater than 0, because 0th column is always
					// 'Iteration' and we don't need that --> see decideColumns() method
					Double x = (first_act_x > 0) ? Double.valueOf(column[first_act_x]) : 0;
					Double y = (first_act_y > 0) ? Double.valueOf(column[first_act_y]) : 0;
					Id<Person> personId = Id.create(column[person], Person.class);

					Assert.assertEquals("x coordinate does not match", Double.valueOf(firstActivity.get(personId).get(0)), x,
							0);
					Assert.assertEquals("y coordinate does not match", Double.valueOf(firstActivity.get(personId).get(1)), y,
							0);

					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	
	}
	
	private static void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "person":
				person = i;
				break;

			case "first_act_x":
				first_act_x = i;
				break;
				
			case "first_act_y":
				first_act_y = i;
				break;

			}
			i++;
		}
	}
}
