package playground.gregor.sim2d_v2.calibration_v2;


import java.util.ArrayList;
import java.util.Collections;
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
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

public class CalibrationController2D  {


	private PedVisPeekABot vis;
	private final List<Double> allDiffs = new ArrayList<Double>();
	private final  List<Double>  allAi = new ArrayList<Double>();

	private final Config config;
	private Sim2DConfigGroup sim2dConfig;
	private final Scenario scenario;
	private final PhantomEvents phantomEvents;

	public CalibrationController2D(String[] args) {
		String configFile = args[0];
		this.config = ConfigUtils.loadConfig(configFile);
		initSim2DConfigGroup();
		this.scenario = ScenarioUtils.createScenario(this.config);
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenario);
		loader.loadScenario();
		this.sim2dConfig.setEnableCircularAgentInterActionModule("false");
		this.sim2dConfig.setEnableCollisionPredictionAgentInteractionModule("true");
		this.sim2dConfig.setEnableCollisionPredictionEnvironmentForceModule("true");
		this.sim2dConfig.setEnableDrivingForceModule("true");
		this.sim2dConfig.setEnableEnvironmentForceModule("false");
		this.sim2dConfig.setEnablePathForceModule("true");
		this.sim2dConfig.setPhantomPopulationEventsFile("/Users/laemmel/devel/dfg/phantomEvents.xml.gz");
		this.sim2dConfig.setAi(50);
		this.phantomEvents = new PhantomPopulationLoader(this.sim2dConfig.getPhantomPopulationEventsFile()).getPhantomPopulation();
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

		List<Id> ids = new ArrayList<Id>();
		for (int i = 0; i < 480; i++) {
			ids.add(new IdImpl(i));
		}

		int numOfThreads = 4;

		for (int it = 0; it < 20; it++){

			List<Validator> vs = new ArrayList<Validator>();
			List<Thread> ts = new ArrayList<Thread>();

			int size = 40/ numOfThreads;
			for (int i = 0; i < numOfThreads; i++) {
				Collections.shuffle(ids);
				List<Id> sub = ids.subList(0, size-1);
				Validator v1 = new Validator(null);
				Worker w1 = new Worker(this.scenario,this.phantomEvents,v1,sub);
				Thread t1 = new Thread(w1);
				vs.add(v1);
				ts.add(t1);
				t1.start();
			}

			double diff = 0;

			for (int i = 0; i < numOfThreads; i++) {
				Thread t1 = ts.get(i);
				try {
					t1.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Validator v1 = vs.get(i);
				diff += v1.getAndResetAllDiff();
			}

			this.allDiffs.add(diff);
			this.allAi .add(this.sim2dConfig.getAi());
			System.out.println("ITERATION " + it + " finished");
			this.sim2dConfig.setAi(this.sim2dConfig.getAi()+5);
		}




		System.err.println("=================================================");
		for (int i = 0; i <this.allAi.size(); i++) {
			System.err.println("diff:" + this.allDiffs.get(i) + "  Ai:" + this.allAi.get(i));
		}
		System.err.println("=================================================");
	}

	private static class Worker implements Runnable {

		private final Scenario scenario;
		private final PhantomEvents phantomEvents;
		private final Validator validator;
		private final List<Id> ids;


		public Worker( Scenario scenario, PhantomEvents phantomEvents, Validator validator, List<Id> ids) {
			this.scenario = scenario;
			this.phantomEvents = phantomEvents;
			this.validator = validator;
			this.ids = ids;
		}

		@Override
		public void run() {
			CalibrationSimulationEngine cse = new CalibrationSimulationEngine(this.scenario, this.phantomEvents, this.validator);
			cse.doOneIteration(this.ids);

		}

	}



	public static void main(String [] args) {
		CalibrationController2D controller = new CalibrationController2D(args);
		controller.run();
	}



}
