
package org.matsim.contrib.pseudosimulation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunPSimTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	Logger logger = Logger.getLogger(RunPSimTest.class);

	final Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"),"0.config.xml"));

	static double psimscore, qsimscore = Double.NEGATIVE_INFINITY;


	@Test
	public void testA() {
		config.controler().setCreateGraphs(false);

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		pSimConfigGroup.setIterationsPerCycle(20);

		//identify selector strategies
		Field[] selectors = DefaultPlanStrategiesModule.DefaultSelector.class.getDeclaredFields();
		List<String> selectorNames = new ArrayList<>();
		for (Field selector : selectors) {
			selectorNames.add(selector.toString());
		}

		//lower the weight of non-selector strategies, as we will run many iters
		Iterator<StrategyConfigGroup.StrategySettings> iterator = config.strategy().getStrategySettings().iterator();
		while (iterator.hasNext()) {
			StrategyConfigGroup.StrategySettings settings = iterator.next();
			if (!selectorNames.contains(settings.getStrategyName()))
				settings.setWeight(settings.getWeight() / 20);
		}
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(20);
		config.controler().setDumpDataAtEnd(false);
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
		psimscore = execScoreTracker.executedScore;
	}

	@Test
	public void testB() {
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(2);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);
		Controler controler = new Controler(config);
		ExecScoreTracker execScoreTracker = new ExecScoreTracker(controler);
		controler.addControlerListener(execScoreTracker);
		controler.run();
		qsimscore = execScoreTracker.executedScore;
		logger.info("RunPSim score was " + psimscore);
		logger.info("Default controler score was " + qsimscore);
		if ((psimscore - qsimscore) / qsimscore < 0.01)
			Assert.fail("Usually RunPSim outperforms default Controler on this test for executed score by a margin > 1%; something changed.");
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