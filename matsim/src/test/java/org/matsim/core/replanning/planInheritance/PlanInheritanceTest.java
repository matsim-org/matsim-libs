package org.matsim.core.replanning.planInheritance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.replanning.inheritance.PlanInheritanceModule;
import org.matsim.core.replanning.inheritance.PlanInheritanceRecord;
import org.matsim.core.replanning.inheritance.PlanInheritanceRecordReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


public class PlanInheritanceTest {
	/**
	 * @author alex94263
	 */

		@RegisterExtension
		public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testPlanInheritanceEnabled() throws IOException {
			String outputDirectory = util.getOutputDirectory();

			Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
			config.controller().setLastIteration(10);
			config.controller().setOutputDirectory(outputDirectory);
			config.planInheritance().setEnabled(true);
			Controler c = new Controler(config);

			c.run();
			File csv = new File(outputDirectory, "planInheritanceRecords.csv.gz");

			assertThat(csv).exists();


			List<Person> personList = new ArrayList<Person>();
			final Scenario scenario = ScenarioUtils.createScenario(config);
			StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
			spr.addAlgorithm(new PersonAlgorithm() {
	            @Override
	            public void run(Person person) {
	                personList.add(person);
	            }
	        });
			spr.readFile(util.getOutputDirectory()+"output_plans.xml.gz");
			for(Person per : personList) {
				for(Plan p : per.getPlans()) {
					assert(p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.PLAN_MUTATOR));
					assert(p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.ITERATION_CREATED));
					assert(p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.PLAN_ID));
				}

			}

			PlanInheritanceRecordReader reader = new PlanInheritanceRecordReader(outputDirectory+"planInheritanceRecords.csv.gz");
			List<PlanInheritanceRecord> records = reader.read();
			assert(records.size()==2);
			assert( ((PlanInheritanceRecord) records.get(0)).getAgentId().equals(Id.createPersonId("1")));
			assert( ((PlanInheritanceRecord) records.get(0)).getAncestorId().equals(Id.create("NONE",Plan.class)));
			assert( ((PlanInheritanceRecord) records.get(0)).getMutatedBy().equals(PlanInheritanceModule.INITIAL_PLAN));
			assert( ((PlanInheritanceRecord) records.get(0)).getIterationCreated() == 0);
			assert( ((PlanInheritanceRecord) records.get(0)).getIterationRemoved() == -1);
			assert( ((PlanInheritanceRecord) records.get(0)).getPlanId().equals(Id.create("1",Plan.class)));
			assert( ((PlanInheritanceRecord) records.get(0)).getIterationsSelected().equals(Arrays.asList(0, 1, 2, 3, 4, 6, 7, 8, 9, 10)));

			assert( ((PlanInheritanceRecord) records.get(1)).getAgentId().equals(Id.createPersonId("1")));
			assert( ((PlanInheritanceRecord) records.get(1)).getAncestorId().equals(Id.create("1",Plan.class)));
			assert( ((PlanInheritanceRecord) records.get(1)).getMutatedBy().equals("RandomPlanSelector_ReRoute"));
			assert( ((PlanInheritanceRecord) records.get(1)).getIterationCreated() == 5);
			assert( ((PlanInheritanceRecord) records.get(1)).getIterationRemoved() == -1);
			assert( ((PlanInheritanceRecord) records.get(1)).getPlanId().equals(Id.create("2",Plan.class)));
			assert( ((PlanInheritanceRecord) records.get(1)).getIterationsSelected().equals(Arrays.asList(5)));



		}

	@Test
	void testPlanInheritanceDisabled() throws IOException {
			String outputDirectory = util.getOutputDirectory();

			Config config = this.util.loadConfig("test/scenarios/equil/config_plans1.xml");
			config.controller().setLastIteration(1);
			config.controller().setOutputDirectory(outputDirectory);
			Controler c = new Controler(config);

			c.run();

			File csv = new File(outputDirectory, "planInheritanceRecords.csv.gz");

			assertThat(csv).doesNotExist();

			List<Person> personList = new ArrayList<Person>();
			final Scenario scenario = ScenarioUtils.createScenario(config);
			StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
			spr.addAlgorithm(new PersonAlgorithm() {
	            @Override
	            public void run(Person person) {
	                personList.add(person);
	            }
	        });
			spr.readFile(util.getOutputDirectory()+"output_plans.xml.gz");
			for(Person per : personList) {
				for(Plan p : per.getPlans()) {
					assert(!p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.PLAN_MUTATOR));
					assert(!p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.ITERATION_CREATED));
					assert(!p.getAttributes().getAsMap().keySet().contains(PlanInheritanceModule.PLAN_ID));
				}

			}
		}
}
