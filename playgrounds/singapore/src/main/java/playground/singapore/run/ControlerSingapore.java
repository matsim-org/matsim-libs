package playground.singapore.run;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;

import playground.artemc.calibration.CalibrationStatsListener;
import playground.singapore.ptsim.qnetsimengine.PTQSimFactory;
import playground.singapore.scoring.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.singapore.transitRouterEventsBased.TransitRouterWSImplFactory;
import playground.singapore.transitRouterEventsBased.stopStopTimes.StopStopTimeCalculator;
import playground.singapore.transitRouterEventsBased.waitTimes.WaitTimeStuckCalculator;
import playground.singapore.typesPopulation.config.groups.StrategyPopsConfigGroup;
import playground.singapore.typesPopulation.controler.corelisteners.PlansDumping;
import playground.singapore.typesPopulation.replanning.StrategyManagerPops;
import playground.singapore.typesPopulation.replanning.StrategyManagerPopsConfigLoader;
import playground.singapore.typesPopulation.scenario.ScenarioUtils;

public class ControlerSingapore extends Controler {

	public ControlerSingapore(Config config) {
		super(config);
		addCoreControlerListener(new PlansDumping());
	}
	public ControlerSingapore(Scenario scenario) {
		super(scenario);
		addCoreControlerListener(new PlansDumping());
	}
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.removeModule(StrategyConfigGroup.GROUP_NAME);
		config.addModule(new StrategyPopsConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		Controler controler = new ControlerSingapore(ScenarioUtils.loadScenario(config));
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), new String[]{args[1], args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id>()));
		WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(controler.getPopulation(), controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().getQSimConfigGroup().getEndTime()-controler.getConfig().getQSimConfigGroup().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().getQSimConfigGroup().getEndTime()-controler.getConfig().getQSimConfigGroup().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		controler.setTransitRouterFactory(new TransitRouterWSImplFactory(controler.getScenario(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes()));
		controler.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario()));
		controler.setMobsimFactory(new PTQSimFactory());
		controler.run();
	}
	@Override
	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManagerPops manager = new StrategyManagerPops();
		addControlerListener(manager);
		StrategyManagerPopsConfigLoader.load(this, manager);
		return manager;
	}

}
