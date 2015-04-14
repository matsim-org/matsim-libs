package playground.pieter.pseudosimulation.controler.listeners;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.mobsim.PSimFactory;

import java.util.ArrayList;

/**
 * @author fouriep
 *         <p/>
 *         Switches between two mobility simulations, the first being the
 *         expensive one, the second being cheap.
 *         <p/>
 *         Switches between the expensive sim and the cheap sim according to the
 *         config parameters used in the constructore. Always executes the
 *         expensive sim at the last iteration.
 *         <p>
 *         Raises a static boolean flag for others to read if it's currently on
 *         an expensive sim; this flag defaults to true if the mobsimswitcher is
 *         not instantiated
 */

public class MobSimSwitcher implements ControlerListener,
		IterationStartsListener {
	public static boolean isQSimIteration = true;
	private final static String START_RATE = "startRate";
	private final static String END_RATE = "endRate";
	private final static String SWITCH_TYPE = "switchType";
	private final static String START_ITER = "startIter";
	private final static String END_ITER = "endIter";
	private final static String INCREASE_EVERY_N = "increaseEveryNExpensiveIters";
	private int increaseEveryNExpensiveIters = 1;
	private static int qsimIterCount = 0;
	private int cheapIterCount = 0;
	private int currentRate = 0;
    private int endRate = 0;
	private int startIter;
	private int endIter;
	private static final ArrayList<Integer> qsimIters = new ArrayList<>();
	private final PSimControler psimControler;

	private enum SwitchType {
		incrementing, doubling
	}

	private SwitchType switchType = SwitchType.incrementing;
	private final Logger log = Logger.getLogger(this.getClass());
	private final Controler matsimControler;

	public MobSimSwitcher(PSimControler p) {
		this.psimControler = p;
		this.matsimControler = p.getMATSimControler();
        int startRate = 0;
        if (matsimControler.getConfig().getParam("MobSimSwitcher", START_RATE) != null)
			startRate = Math.max(
					0,
					Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
							START_RATE)));
		if (matsimControler.getConfig().getParam("MobSimSwitcher", END_RATE) != null)
			endRate = Math.max(
					0,
					Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
							END_RATE)));
		currentRate = startRate;

		startIter = matsimControler.getConfig().controler().getFirstIteration();
		if (matsimControler.getConfig().getParam("MobSimSwitcher", START_ITER) != null)
			startIter = Math.max(
					startIter,
					Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
							START_ITER)));
		endIter = matsimControler.getConfig().controler().getLastIteration();
		if (matsimControler.getConfig().getParam("MobSimSwitcher", END_ITER) != null)
			endIter = Math.min(
					endIter,
					Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
							END_ITER)));
		if (matsimControler.getConfig().getParam("MobSimSwitcher", INCREASE_EVERY_N) != null)
			increaseEveryNExpensiveIters = Math.max(
					increaseEveryNExpensiveIters,
					Integer.parseInt(matsimControler.getConfig().getParam("MobSimSwitcher",
							INCREASE_EVERY_N)));
		String rc = matsimControler.getConfig().getParam("MobSimSwitcher", SWITCH_TYPE);
		if (rc == null) {
			switchType = SwitchType.incrementing;
		} else if (rc.equals("doubling")) {
			switchType = SwitchType.doubling;
		}

	}

	static ArrayList<Integer> getQSimIters() {
		return qsimIters;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		

		if (checkExpensiveIter(event.getIteration())) {
			log.warn("Running full queue simulation");
			String mobsim = matsimControler.getConfig().controler().getMobsim();

			if (mobsim != null) {
				if (mobsim.equals("qsim")) {
					matsimControler.setMobsimFactory(QSimFactory.createQSimFactory());
					// controler.setMobsimFactory(new MentalSimFactory(ttcalc));
				} else if (mobsim.equals("jdeqsim")) {
					matsimControler.setMobsimFactory(new JDEQSimulationFactory());

				} 

			} else {
				matsimControler.setMobsimFactory(QSimFactory.createQSimFactory());
			}
		} else {
			log.info("Running PSim");
			matsimControler.setMobsimFactory(new PSimFactory( ));
			psimControler.clearPlansForPseudoSimulation();

		}
	}

	private boolean checkExpensiveIter(int iteration) {

		if (iteration == matsimControler.getConfig().controler().getLastIteration()) {
			MobSimSwitcher.isQSimIteration = true;
			return isQSimIteration;
		}
		if (iteration < endIter && qsimIterCount > 0) {
			if (qsimIterCount >= increaseEveryNExpensiveIters
					&& iteration > startIter) {
				log.warn("Increasing rate of switching between QSim and PSim");
				if (currentRate < endRate) {
					if (switchType.equals(SwitchType.doubling)) {
						currentRate *= 2;

					} else {
						currentRate++;
					}
				}
				qsimIterCount = 0;
			}
		}
		if (isQSimIteration && cheapIterCount == 0
				&& iteration > startIter) {
			isQSimIteration = false;
			cheapIterCount++;
			return isQSimIteration;
		}
		if (cheapIterCount >= currentRate - 1) {
			isQSimIteration = true;
			qsimIters.add(iteration);
			cheapIterCount = 0;
			qsimIterCount++;
			return isQSimIteration;
		}
		if (isQSimIteration) {
			qsimIters.add(iteration);
			qsimIterCount++;
		} else {
			cheapIterCount++;

		}
		return isQSimIteration;
	}

}
