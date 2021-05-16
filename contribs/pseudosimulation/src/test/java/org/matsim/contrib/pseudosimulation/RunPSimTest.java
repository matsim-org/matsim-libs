
package org.matsim.contrib.pseudosimulation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
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
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;
import org.matsim.testcases.MatsimTestUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunPSimTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Logger logger = Logger.getLogger(RunPSimTest.class );

	private final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial" ),"0.config.xml" ) );

	/**
	 * Run 1 normal qsim iteration, a couple of psim iterations and a final 2nd qsim iteration.
	 */
	@Test
	public void testA() {
		config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
		config.controler().setCreateGraphs(false);

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(20);
		
		config.plansCalcRoute().setRoutingRandomness(0.);

		//identify selector strategies
		Field[] selectors = DefaultPlanStrategiesModule.DefaultSelector.class.getDeclaredFields();
		List<String> selectorNames = new ArrayList<>();
//		for (Field selector : selectors) {
//			selectorNames.add(selector.toString());
//			logger.warn( selector.toString() );
//		}
		// yyyyyy does not work as designed
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.BestScore );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectPathSizeLogit );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectRandom );
		selectorNames.add( DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta );

		//lower the weight of non-selector strategies, as we will run many iters
		for( StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings() ){
			if( !selectorNames.contains( settings.getStrategyName() ) ){
				logger.warn( settings.getStrategyName() );
				settings.setWeight( settings.getWeight() * 20 );
				settings.setDisableAfter( 18 );
			}
		}
		// yyyyyy I think that the above also includes the selector strategies.  ??
//		System.exit( -1);

		final String outDir = utils.getOutputDirectory();
		config.controler().setOutputDirectory( outDir );
		config.controler().setLastIteration(20);
//		config.controler().setDumpDataAtEnd(false);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
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
		Assert.assertEquals("RunPsim score changed.", 134.54001491094124d, psimScore, MatsimTestUtils.EPSILON);
//		Assert.assertEquals("RunPsim score changed.", 134.52369453719413d, psimScore, MatsimTestUtils.EPSILON);
//		Assert.assertEquals("RunPsim score changed.", 132.73129073101293d, psimScore, MatsimTestUtils.EPSILON);
	}

	/**
	 * For comparison run 2 normal qsim iterations. Psim score should be slightly higher than default Controler score.
	 * 
	 * Prior to implementing routing mode RunPSimTest tested only that psimScore outperformed default Controler on this
	 * test for executed score by a margin > 1%. In the last commit in matsim master where the test ran, the psim score
	 * in testA() was 134.52369453719413 and qsim score in testB was 131.84309487251033).
	 */
	@Test
	public void testB() {
		config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(2);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		config.plansCalcRoute().setRoutingRandomness(0.);
		Controler controler = new Controler(config);
		ExecScoreTracker execScoreTracker = new ExecScoreTracker(controler);
		controler.addControlerListener(execScoreTracker);
		controler.run();
		
		double qsimScore = execScoreTracker.executedScore;
		logger.info("Default controler score was " + qsimScore );
//		Assert.assertEquals("Default controler score changed.", 131.84309487251033d, qsimScore, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Default controler score changed.", 131.84350487113088d, qsimScore, MatsimTestUtils.EPSILON);
	}

	class ExecScoreTracker implements ShutdownListener {
		private final MatsimServices controler;
		double executedScore = Double.NEGATIVE_INFINITY;

		ExecScoreTracker(MatsimServices controler) {
			this.controler = controler;
		}

		@Override
		public void notifyShutdown(ShutdownEvent event) {
			executedScore = controler.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.executed).get(controler.getConfig().controler().getLastIteration());
		}


	}
}
