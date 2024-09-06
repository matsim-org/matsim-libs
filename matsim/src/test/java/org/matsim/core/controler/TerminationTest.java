package org.matsim.core.controler;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Singleton;

/**
 * This test makes sure that events are written based on the
 * TerminationCriterion.
 */
public class TerminationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testSimulationEndsOnInterval() {
		prepareExperiment(2, 4, ControllerConfigGroup.CleanIterations.keep).run();

		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.4/4.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/output_events.xml.gz").exists());

		long iterationOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/ITERS/it.4/4.events.xml.gz");
		long mainOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/output_events.xml.gz");
		Assertions.assertEquals(iterationOutput, mainOutput);
	}

	@Test
	void testOnlyRunIterationZero() {
		prepareExperiment(2, 0, ControllerConfigGroup.CleanIterations.keep).run();

		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.0/0.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/output_events.xml.gz").exists());

		long iterationOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/ITERS/it.0/0.events.xml.gz");
		long mainOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/output_events.xml.gz");
		Assertions.assertEquals(iterationOutput, mainOutput);
	}

	@Test
	void testSimulationEndsOffInterval() {
		// This is the case when the TerminationCriterion decides that the simulation is
		// done, but it does not fall at the same time as the output interval.

		prepareExperiment(2, 3, ControllerConfigGroup.CleanIterations.keep).run();

		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.2/2.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.3/3.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/output_events.xml.gz").exists());

		long iterationOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/ITERS/it.3/3.events.xml.gz");
		long mainOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/output_events.xml.gz");
		Assertions.assertEquals(iterationOutput, mainOutput);
	}

	@Test
	void testSimulationEndDeleteIters() {
		prepareExperiment(2, 3, ControllerConfigGroup.CleanIterations.delete).run();
		Assertions.assertFalse(new File(utils.getOutputDirectory(), "/ITERS").exists());
	}

	private Controler prepareExperiment(int interval, int criterion, ControllerConfigGroup.CleanIterations iters) {
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setCleanItersAtEnd(iters);

		config.controller().setWriteEventsInterval(interval);
		config.controller().setLastIteration(criterion);

		return new Controler(config);
	}

	@Test
	void testMultipleLastIterations() {
		/**
		 * This test covers the case where the termination criterion decides that the
		 * coming iteration may be the last, but then, after analysis and after the data
		 * is written, decides that more iterations need to be run.
		 */

		Controler controller = prepareExperiment(2, 1000, ControllerConfigGroup.CleanIterations.keep);

		controller.setTerminationCriterion(new TerminationCriterion() {
			@Override
			public boolean mayTerminateAfterIteration(int iteration) {
				return iteration >= 2; // After it 2 we decide that we need to write events
			}

			@Override
			public boolean doTerminate(int iteration) {
				return iteration >= 4; // But only iteration 4 is actually the last one
			}
		});

		controller.run();

		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.2/2.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.3/3.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.4/4.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/output_events.xml.gz").exists());

		long iterationOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/ITERS/it.4/4.events.xml.gz");
		long mainOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/output_events.xml.gz");
		Assertions.assertEquals(iterationOutput, mainOutput);
	}

	@Test
	void testCustomConverenceCriterion() {
		/**
		 * In this test, we set all legs to walk and let agents change them to car. We
		 * stop the simulation once there are more car legs than walk legs.
		 */

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setCleanItersAtEnd(ControllerConfigGroup.CleanIterations.keep);

		{ // Set up mode choice
			config.changeMode().setModes(new String[] { "car", "walk" });

			StrategySettings modeStrategy = new StrategySettings();
			modeStrategy.setStrategyName(DefaultStrategy.ChangeTripMode);
			modeStrategy.setWeight(0.1);
			config.replanning().addStrategySettings(modeStrategy);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		{ // Change initial mode to walk
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
					leg.setMode("walk");
					leg.setRoute(null);
				}
			}
		}

		Controler controler = new Controler(scenario);

		{ // Set convergence criterion
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().to(CustomConvergenceCriterion.class);
					bind(TerminationCriterion.class).to(CustomConvergenceCriterion.class);
					addEventHandlerBinding().to(CustomConvergenceCriterion.class);
				}
			});
		}

		controler.run();

		Assertions.assertEquals(12, (int) controler.getIterationNumber());

		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/ITERS/it.12/12.events.xml.gz").exists());
		Assertions.assertTrue(new File(utils.getOutputDirectory(), "/output_events.xml.gz").exists());

		long iterationOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/ITERS/it.12/12.events.xml.gz");
		long mainOutput = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/output_events.xml.gz");
		Assertions.assertEquals(iterationOutput, mainOutput);
	}

	@Singleton
	static private class CustomConvergenceCriterion
			implements TerminationCriterion, PersonDepartureEventHandler, IterationStartsListener {
		private int countCar = 0;
		private int countWalk = 0;

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			countCar = 0;
			countWalk = 0;
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("car")) {
				countCar++;
			} else {
				countWalk++;
			}
		}

		@Override
		public boolean mayTerminateAfterIteration(int iteration) {
			return countCar > countWalk; // Check before the iteration!
		}

		@Override
		public boolean doTerminate(int iteration) {
			return countCar > countWalk; // Verify after the iteration!
		}
	}
}
