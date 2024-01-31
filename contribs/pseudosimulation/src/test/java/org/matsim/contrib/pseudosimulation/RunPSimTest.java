
package org.matsim.contrib.pseudosimulation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class RunPSimTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private final Logger logger = LogManager.getLogger(RunPSimTest.class );

	private final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial" ),"0.config.xml" ) );

	/**
	 * Run 1 normal qsim iteration, a couple of psim iterations and a final 2nd qsim iteration.
	 */
	@Test
	void testA() {
		config.controller().setCreateGraphs(false);

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(20);

		config.routing().setRoutingRandomness(0.);

		//identify selector strategies
		Field[] selectors = DefaultPlanStrategiesModule.DefaultSelector.class.getDeclaredFields();
		List<String> selectorNames = new ArrayList<>();

		// yyyyyy does not work as designed
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.BestScore );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectPathSizeLogit );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectRandom );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta );

		//lower the weight of non-selector strategies, as we will run many iters
		for( ReplanningConfigGroup.StrategySettings settings : config.replanning().getStrategySettings() ){
			if( !selectorNames.contains( settings.getStrategyName() ) ){
				logger.warn( settings.getStrategyName() );
				settings.setWeight( settings.getWeight() * 20 );
				settings.setDisableAfter( 18 );
			}
		}
		// yyyyyy I think that the above also includes the selector strategies.  ??
//		System.exit( -1);

		final String outDir = utils.getOutputDirectory();
		config.controller().setOutputDirectory( outDir );
		config.controller().setLastIteration(20);
//		config.controler().setDumpDataAtEnd(false);
//		config.strategy().setFractionOfIterationsToDisableInnovation( 0.8 ); // crashes


		RunPSim runPSim = new RunPSim(config, pSimConfigGroup);
		ExecScoreTracker execScoreTracker = new ExecScoreTracker(runPSim.getMatsimControler());
		runPSim.getMatsimControler().addControlerListener(execScoreTracker);

		((Controler) runPSim.getMatsimControler()).addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(TransitEmulator.class).to(NoTransitEmulator.class);
			}
		});


		runPSim.run();
		double psimScore = execScoreTracker.executedScore;
		logger.info("RunPSim score was " + psimScore);
		Population popExpected = PopulationUtils.createPopulation( config ) ;
		PopulationUtils.readPopulation( popExpected, utils.getInputDirectory() + "/output_plans.xml.gz" );
		Population popActual = PopulationUtils.createPopulation( config );
		PopulationUtils.readPopulation( popActual, outDir + "/output_plans.xml.gz" );
		new PopulationComparison().compare( popExpected, popActual ) ;
		Assertions.assertEquals(138.86084460860525, psimScore, MatsimTestUtils.EPSILON, "RunPsim score changed.");

	}

	/**
	 * For comparison run 2 normal qsim iterations. Psim score should be slightly higher than default Controler score.
	 *
	 * Prior to implementing routing mode RunPSimTest tested only that psimScore outperformed default Controler on this
	 * test for executed score by a margin > 1%. In the last commit in matsim master where the test ran, the psim score
	 * in testA() was 134.52369453719413 and qsim score in testB was 131.84309487251033).
	 */
	@Test
	void testB() {
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(2);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);
		config.routing().setRoutingRandomness(0.);
		Controler controler = new Controler(config);
		ExecScoreTracker execScoreTracker = new ExecScoreTracker(controler);
		controler.addControlerListener(execScoreTracker);
		controler.run();

		double qsimScore = execScoreTracker.executedScore;
		logger.info("Default controler score was " + qsimScore );
//		Assert.assertEquals("Default controler score changed.", 131.84309487251033d, qsimScore, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(131.8303325803256, qsimScore, MatsimTestUtils.EPSILON, "Default controler score changed.");
	}

	class ExecScoreTracker implements ShutdownListener {
		private final MatsimServices controler;
		double executedScore = Double.NEGATIVE_INFINITY;

		ExecScoreTracker(MatsimServices controler) {
			this.controler = controler;
		}

		@Override
		public void notifyShutdown(ShutdownEvent event) {
			executedScore = controler.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.executed).get(controler.getConfig().controller().getLastIteration());
		}


	}
}
