package playground.gregor.sim2d_v2.calibration_v2;


import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomPopulationLoader;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.controller.Controller2D;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

public class CalibrationController2D  {


	private PedVisPeekABot vis;
	private final PhantomEvents phantomEvents;
	private final Validator validator;
	private final List<Double> allDiffs = new ArrayList<Double>();
	private final  List<Double>  allAi = new ArrayList<Double>();

	private final Id cali = new IdImpl("1");
	private final Config config;
	private Sim2DConfigGroup sim2dConfig;
	private final EventsManager events;
	private final Scenario scenario;
	private final Scenario2DImpl scenario2DData;

	public CalibrationController2D(String[] args) {
		String configFile = args[0];
		this.config = ConfigUtils.loadConfig(configFile);
		initSim2DConfigGroup();
		this.events = EventsUtils.createEventsManager();
		this.scenario = ScenarioUtils.createScenario(this.config);
		this.scenario2DData = new Scenario2DImpl(this.config);
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenario2DData);
		loader.loadScenario();
		this.sim2dConfig.setEnableCircularAgentInterActionModule("false");
		this.sim2dConfig.setEnableCollisionPredictionAgentInteractionModule("true");
		this.sim2dConfig.setEnableCollisionPredictionEnvironmentForceModule("true");
		this.sim2dConfig.setEnableDrivingForceModule("true");
		this.sim2dConfig.setEnableEnvironmentForceModule("false");
		this.sim2dConfig.setEnablePathForceModule("true");
		this.sim2dConfig.setPhantomPopulationEventsFile("/Users/laemmel/devel/dfg/phantomEvents.xml.gz");
		this.sim2dConfig.setAi(40);
		this.phantomEvents = new PhantomPopulationLoader(this.sim2dConfig.getPhantomPopulationEventsFile()).getPhantomPopulation();
		this.validator = new Validator(this.events);
		this.events.addHandler(this.validator);
	}

	/**
	 * 
	 */
	private void initSim2DConfigGroup() {
		Module module = this.config.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		this.sim2dConfig = s;
		this.config.getModules().put("sim2d", s);
	}


	public void run() {
		for (int it = 0; it < 100; it++){
			runMobSim();
			System.out.println("ITERATION " + it + " finished");
		}
		System.err.println("=================================================");
		for (int i = 0; i <this.allAi.size(); i++) {
			System.err.println("diff:" + this.allDiffs.get(i) + "  Ai:" + this.allAi.get(i));
		}
		System.err.println("=================================================");
	}


	protected void runMobSim() {

		CalibrationSimulationEngine cse = new CalibrationSimulationEngine(this.scenario2DData, this.phantomEvents, this.events, this.validator);

		cse.doOneIteration(this.cali);

		this.allDiffs.add(this.validator.getAndResetAllDiff());
		this.allAi .add(this.sim2dConfig.getAi());

		this.sim2dConfig.setAi(this.sim2dConfig.getAi()+1);
	}

	public static void main(String [] args) {
		CalibrationController2D controller = new CalibrationController2D(args);
		controller.run();
	}



}
