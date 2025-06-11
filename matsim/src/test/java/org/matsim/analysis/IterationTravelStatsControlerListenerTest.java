/**
 *
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.StandaloneExperiencedPlansModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class IterationTravelStatsControlerListenerTest {

	final IdMap<Person, Plan> map = new IdMap<>(Person.class);
	Config config = ConfigUtils.createConfig();

	private int person;
	private int executed_score;
	private int first_act_x;
	private int first_act_y;
	private int first_act_type;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIterationTravelStatsControlerListener() {

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

		Person person1 = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		person1.addPlan(plan1);
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));
		person2.addPlan(plan2);
		Person person3 = PopulationUtils.getFactory().createPerson(Id.create("3", Person.class));
		person3.addPlan(plan3);
		Person person4 = PopulationUtils.getFactory().createPerson(Id.create("4", Person.class));
		person4.addPlan(plan4);

		scenario.getPopulation().addPerson(person1);
		scenario.getPopulation().addPerson(person2);
		scenario.getPopulation().addPerson(person3);
		scenario.getPopulation().addPerson(person4);

		performTest(scenario, utils.getOutputDirectory() + "/IterationTravelStatsControlerListener");
	}

	private void performTest(Scenario scenario, String outputDirectory) {

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		ShutdownEvent shutdownEvent = new ShutdownEvent(null, false, 0, null);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new IterationTravelStatsModule());
				install(new ScenarioByInstanceModule(scenario));
				install(new StandaloneExperiencedPlansModule());
				// an AnalysisMainModeIdentifier must be bound to avoid injection creation errors. TripRouterModule should do this. Check thereby that TripRouterModule still does that by installing TripRouterModule instead of binding AnalysisMainModeIdentifier directly
				install(new TripRouterModule());
				install(new TimeInterpretationModule());
				bind(OutputDirectoryHierarchy.class).asEagerSingleton();
				//bind(ExperiencedPlansService.class).to(ExperiencedPlansServiceImpl.class);
				bind(IterationTravelStatsControlerListener.class).asEagerSingleton();
				bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
			}
		});
		IterationTravelStatsControlerListener ltcl = injector.getInstance(IterationTravelStatsControlerListener.class);
		ltcl.notifyShutdown(shutdownEvent);
		readAndValidateValues(scenario);
	}

	private Activity identifyFirstActivity(Person person) {
		return (Activity) person.getSelectedPlan().getPlanElements().get(0);
	}

	private void readAndValidateValues(Scenario scenario) {

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

					Person personInScenario = scenario.getPopulation().getPersons().get(personId);
					Activity firstActivity = identifyFirstActivity(personInScenario);

					Assertions.assertEquals(personInScenario.getSelectedPlan().getScore(), Double.valueOf(column[executed_score]), MatsimTestUtils.EPSILON, "wrong score");
					Assertions.assertEquals(firstActivity.getCoord().getX(), x,
							MatsimTestUtils.EPSILON,
							"x coordinate does not match");
					Assertions.assertEquals(firstActivity.getCoord().getY(), y,
							MatsimTestUtils.EPSILON,
							"y coordinate does not match");
					Assertions.assertEquals(firstActivity.getType(), column[first_act_type], "type of first activity does not match");

					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "person":
				person = i;
				break;

			case "executed_score":
				executed_score = i;
				break;

			case "first_act_x":
				first_act_x = i;
				break;

			case "first_act_y":
				first_act_y = i;
				break;

			case "first_act_type":
				first_act_type = i;
				break;

			}
			i++;
		}
	}
}
