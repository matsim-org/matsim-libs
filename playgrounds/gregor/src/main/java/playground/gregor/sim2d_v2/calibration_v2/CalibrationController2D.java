package playground.gregor.sim2d_v2.calibration_v2;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomPopulationLoader;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

public class CalibrationController2D  {


	private static final int THREADS = 4;

	private final Mutator mut = new Mutator();

	private final Config config;
	private Sim2DConfigGroup sim2dConfig;
	private final Scenario scenario;
	private final PhantomEvents phantomEvents;

	private double[] params;

	double c = 1000;
	int it = 0;
	private final Tuple<Double,Double>[] ranges;

	private double[] target;

	private double targetLL;

	public CalibrationController2D(String[] args) {
		String configFile = args[0];
		this.config = ConfigUtils.loadConfig(configFile);
		initSim2DConfigGroup();
		this.scenario = ScenarioUtils.loadScenario(this.config);
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenario);
		loader.load2DScenario();
		this.sim2dConfig.setPhantomPopulationEventsFile("/Users/laemmel/devel/dfg/phantomEvents.xml.gz");
		this.phantomEvents = new PhantomPopulationLoader(this.sim2dConfig.getPhantomPopulationEventsFile()).getPhantomPopulation();

		this.params = new double[3];
		this.ranges = new Tuple[3];
		this.params[0] = 0.5; //MatsimRandom.getRandom().nextDouble();
		this.params[1] = 1.2; //MatsimRandom.getRandom().nextDouble()*5;
		this.params[2] = 12; //MatsimRandom.getRandom().nextDouble()*2000;;
		this.ranges[0] = new Tuple<Double, Double>(0.01, 1.);
		this.ranges[1] = new Tuple<Double, Double>(0.01, 5.);
		this.ranges[2] = new Tuple<Double, Double>(1., 200.);
	}



	private void run() {
		List<Id> ids = new ArrayList<Id>();
		for (int i = 0; i < 480; i++) {
			ids.add(new IdImpl(i));
		}
		Collections.shuffle(ids);

		this.target = new double[3];
		this.target[0] = this.sim2dConfig.getLambda();
		this.target[1] = this.sim2dConfig.getBi();
		this.target[2] = this.sim2dConfig.getAi();
		this.targetLL = run(ids);

		this.sim2dConfig.setLambda(this.params[0]);
		this.sim2dConfig.setBi(this.params[1]);
		this.sim2dConfig.setAi(this.params[2]);
		double actualLL = run(ids);

		for (; this.it < 2000; this.it++) {
			this.c = this.c*0.99;
			double[] proposed = Arrays.copyOf(this.params, 3);
			this.mut.mutate(proposed, this.ranges);
			this.sim2dConfig.setLambda(proposed[0]);
			this.sim2dConfig.setBi(proposed[1]);
			this.sim2dConfig.setAi(proposed[2]);
			double proposedLL = run(ids);

			if (proposedLL > actualLL) {
				actualLL = proposedLL;
				this.params = proposed;
				printStats(actualLL);
			} else {
				double exp = Math.exp((proposedLL - actualLL)/ this.c);
				double rand = MatsimRandom.getRandom().nextDouble();
				if (exp > rand) {
					actualLL = proposedLL;
					this.params = proposed;

					System.out.println("------------------------------------------");
					printStats(actualLL);
				} else {
					this.sim2dConfig.setLambda(this.params[0]);
					this.sim2dConfig.setBi(this.params[1]);
					this.sim2dConfig.setAi(this.params[2]);
				}
			}


			//			System.out.println("ITERATION " + it + " finished");

		}
		System.out.println("target: LL:" + this.targetLL + "  lambda:" + this.target[0] + " Bi:" + this.target[1] + " Ai:" + this.target[2]);
	}

	private void printStats(double actualLL) {
		System.out.println("LL:" + actualLL + "  lambda:" + this.params[0] + " Bi:" + this.params[1] + " Ai:" + this.params[2] + " iteration:" + this.it);
		//		System.out.println("target: LL:" + this.targetLL + "  lambda:" + this.target[0] + " Bi:" + this.target[1] + " Ai:" + this.target[2]);
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


	public double run(List<Id> ids) {







		LLCalculator llCalc = new LLCalculator();
		List<Thread> ts = new ArrayList<Thread>();

		int size = 20/ THREADS;
		for (int i = 0; i < THREADS; i++) {

			int lb = i*size;
			int ub = i*size+size-1;

			List<Id> sub = ids.subList(lb, ub);
			Worker w1 = new Worker(this.scenario,this.phantomEvents,llCalc,sub);
			Thread t1 = new Thread(w1);
			ts.add(t1);
			t1.start();
		}


		for (int i = 0; i < THREADS; i++) {
			try {
				ts.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		double ll = llCalc.getLL();
		//		System.out.println("n:" + llCalc.getSamples());
		return ll;
	}

	private static class Worker implements Runnable {

		private final Scenario scenario;
		private final PhantomEvents phantomEvents;
		private final List<Id> ids;
		private final LLCalculator llCalc;


		public Worker( Scenario scenario, PhantomEvents phantomEvents, LLCalculator llCalc, List<Id> ids) {
			this.scenario = scenario;
			this.phantomEvents = phantomEvents;
			this.llCalc = llCalc;
			this.ids = ids;
		}

		@Override
		public void run() {
			CalibrationSimulationEngine cse = new CalibrationSimulationEngine(this.scenario, this.phantomEvents, this.llCalc);
			cse.doOneIteration(this.ids);

		}

	}



	public static void main(String [] args) {
		CalibrationController2D controller = new CalibrationController2D(args);
		controller.run();
	}





}
