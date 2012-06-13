package playground.pieter.mentalsim.controler.listeners;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.pieter.mentalsim.mobsim.MentalSimFactory;

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
	final private String START_RATE = "startRate";
	final private String END_RATE = "endRate";
	final private String RATE_CHANGE = "rateChange";
	final private String START_ITER = "startIter";
	final private String END_ITER = "endIter";
	final private String DOUBLE_EVERY_N = "doubleEveryNExpensiveIters";
	private int doubleEveryNExpensiveIters = 1;
	private int switchCount = 0;
	private int currentRate = 1;
	private int startRate = 1;
	private int endRate = 1;
	private int startIter;
	private int endIter;
	private Controler controler;
	Logger log = Logger.getLogger(this.getClass());

	public MobSimSwitcher(Controler c) {
		this.controler = c;
		if (c.getConfig().getParam("MobSimSwitcher", START_RATE) != null)
			startRate = Math.max(
					1,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							START_RATE)));
		if (c.getConfig().getParam("MobSimSwitcher", END_RATE) != null)
			endRate = Math.max(
					1,
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
		if (c.getConfig().getParam("MobSimSwitcher", DOUBLE_EVERY_N) != null)
			doubleEveryNExpensiveIters = Math.max(
					doubleEveryNExpensiveIters,
					Integer.parseInt(c.getConfig().getParam("MobSimSwitcher",
							DOUBLE_EVERY_N)));

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		PersonalizableTravelTime ttcalc = controler.getTravelTimeCalculator();

		if (checkExpensiveIter()) {
			log.warn("Running an expensive iteration with full queue simulation");
			String mobsim = controler.getConfig().controler().getMobsim();

			if (mobsim != null) {
				if (mobsim.equals("qsim")) {
					controler.setMobsimFactory(new QSimFactory());
					// controler.setMobsimFactory(new MentalSimFactory(ttcalc));
				} else if (mobsim.equals("jdeqsim")) {
					controler.setMobsimFactory(new JDEQSimulationFactory());
				}
			} else {
				controler.setMobsimFactory(new QueueSimulationFactory());
			}
		} else {
			log.info("Running a cheap iteration with fake simulation");
			controler.setMobsimFactory(new MentalSimFactory(ttcalc));
		}
	}

	public boolean checkExpensiveIter() {
		double iterationsFromStart = controler.getIterationNumber() - startIter;
//		if (iterationsFromStart > 0 && iterationsFromStart % 2 == 0
				if( controler.getIterationNumber() < endIter && switchCount > 0) {
			// double slope = (double) (startRate - endRate)
			// / (startIter - endIter);
			// currentRate = startRate
			// + (int) Math.floor(slope * iterationsFromStart);
			// double the currentRate of switching
			if (switchCount % doubleEveryNExpensiveIters == 0) {
				if (currentRate < endRate) {

					currentRate *= 2;
				}
				switchCount = 0;
			}
		}
		MobSimSwitcher.expensiveIter = iterationsFromStart % currentRate == 0;
		if (MobSimSwitcher.expensiveIter)
			switchCount++;
		if (controler.getIterationNumber() == controler.getLastIteration())
			MobSimSwitcher.expensiveIter = true;
		return expensiveIter;
	}

}
