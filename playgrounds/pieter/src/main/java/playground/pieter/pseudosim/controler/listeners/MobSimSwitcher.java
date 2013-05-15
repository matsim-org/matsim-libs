package playground.pieter.pseudosim.controler.listeners;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.mobsim.PseudoSimFactory;

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
	public static boolean expensiveIter = true;
	final static String START_RATE = "startRate";
	final static String END_RATE = "endRate";
	final static String SWITCH_TYPE = "switchType";
	final static String START_ITER = "startIter";
	final static String END_ITER = "endIter";
	final static String INCREASE_EVERY_N = "increaseEveryNExpensiveIters";
	private int increaseEveryNExpensiveIters = 1;
	private static int expensiveIterCount = 0;
	private int cheapIterCount = 0;
	private int currentRate = 0;
	private int startRate = 0;
	private int endRate = 0;
	private int startIter;
	private int endIter;
	static ArrayList<Integer> expensiveIters = new ArrayList<Integer>();
	private PseudoSimControler controler;

	private enum SwitchType {
		incrementing, doubling
	}

	private SwitchType switchType = SwitchType.incrementing;
	Logger log = Logger.getLogger(this.getClass());

	public MobSimSwitcher(PseudoSimControler c) {
		this.controler = c;
		if (c.getConfig().getParam("MobSimSwitcher", START_RATE) != null)
			startRate = Math.max(
					0,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							START_RATE)));
		if (c.getConfig().getParam("MobSimSwitcher", END_RATE) != null)
			endRate = Math.max(
					0,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							END_RATE)));
		currentRate = startRate;

		startIter = c.getFirstIteration();
		if (c.getConfig().getParam("MobSimSwitcher", START_ITER) != null)
			startIter = Math.max(
					startIter,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							START_ITER)));
		endIter = c.getLastIteration();
		if (c.getConfig().getParam("MobSimSwitcher", END_ITER) != null)
			endIter = Math.min(
					endIter,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							END_ITER)));
		if (c.getConfig().getParam("MobSimSwitcher", INCREASE_EVERY_N) != null)
			increaseEveryNExpensiveIters = Math.max(
					increaseEveryNExpensiveIters,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							INCREASE_EVERY_N)));
		String rc = c.getConfig().getParam("MobSimSwitcher", SWITCH_TYPE);
		if (rc == null) {
			switchType = SwitchType.incrementing;
		} else if (rc.equals("doubling")) {
			switchType = SwitchType.doubling;
		}

	}

	public static ArrayList<Integer> getExpensiveIters() {
		return expensiveIters;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		TravelTime ttcalc = controler.getLinkTravelTimes();

		if (checkExpensiveIter(event.getIteration())) {
			log.warn("Running an expensive iteration with full queue simulation");
			String mobsim = controler.getConfig().controler().getMobsim();

			if (mobsim != null) {
				if (mobsim.equals("qsim")) {
					controler.setMobsimFactory(new QSimFactory());
					// controler.setMobsimFactory(new MentalSimFactory(ttcalc));
				} else if (mobsim.equals("jdeqsim")) {
					controler.setMobsimFactory(new JDEQSimulationFactory());
				} else {
					controler.setMobsimFactory(new QueueSimulationFactory());
				}
			} else {
				controler.setMobsimFactory(new QueueSimulationFactory());
			}
		} else {
			log.info("Running a cheap iteration with mental simulation");
			controler.setMobsimFactory(new PseudoSimFactory(ttcalc, controler));
			controler.clearPlansForMentalSimulation();

		}
	}

	public boolean checkExpensiveIter(int iteration) {

		if (iteration == controler.getLastIteration()) {
			MobSimSwitcher.expensiveIter = true;
			return expensiveIter;
		}
		if (iteration < endIter && expensiveIterCount > 0) {
//			log.error("controler.getIterationNumber() < endIter && expensiveIterCount > 0");
			if (expensiveIterCount >= increaseEveryNExpensiveIters
					&& iteration > startIter) {
				log.error("increase rate");
				if (currentRate < endRate) {
					if (switchType.equals(SwitchType.doubling)) {
						currentRate *= 2;

					} else {
						currentRate++;
					}
				}
				expensiveIterCount = 0;
			}
		}
		if (expensiveIter && cheapIterCount == 0
				&& iteration > startIter) {
			expensiveIter = false;
			cheapIterCount++;
			return expensiveIter;
		}
		if (cheapIterCount >= currentRate - 1) {
			expensiveIter = true;
			this.expensiveIters.add(iteration);
			cheapIterCount = 0;
			expensiveIterCount++;
			return expensiveIter;
		}
		if (expensiveIter) {
			this.expensiveIters.add(iteration);
			expensiveIterCount++;
		} else {
			cheapIterCount++;

		}
		return expensiveIter;
	}

}
