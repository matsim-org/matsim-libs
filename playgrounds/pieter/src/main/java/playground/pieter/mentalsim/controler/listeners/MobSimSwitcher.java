package playground.pieter.mentalsim.controler.listeners;

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
 *         Reads a parameter for switching back to the expensive simulation every
 *         EXPENSIVE_SIM_ITERS
 * 
 */
public class MobSimSwitcher implements ControlerListener,
		IterationStartsListener {

	final static String EXPENSIVE_SIM_ITERS = "expensiveSimIters";
	public static int expensiveSimIters = 1;
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		PersonalizableTravelTime ttcalc = event.getControler()
				.getTravelTimeCalculator();

		int expensiveSimIters = Integer.parseInt(event.getControler()
				.getConfig().getParam("MobSimSwitcher", EXPENSIVE_SIM_ITERS));
		MobSimSwitcher.expensiveSimIters = expensiveSimIters;

		int iterationsFromStart = event.getIteration()
				- event.getControler().getFirstIteration();

		if (iterationsFromStart % expensiveSimIters == 0) {
			String mobsim = event.getControler().getConfig().controler()
					.getMobsim();

			if (mobsim != null) {
				if (mobsim.equals("qsim")) {
					event.getControler().setMobsimFactory(new QSimFactory());
				} else if (mobsim.equals("jdeqsim")) {
					event.getControler().setMobsimFactory(
							new JDEQSimulationFactory());
				}
			} else {
				event.getControler().setMobsimFactory(
						new QueueSimulationFactory());
//				event.getControler().setMobsimFactory(
//						new FakeSimFactory(ttcalc));
			}
		} else {
			event.getControler().setMobsimFactory(new MentalSimFactory(ttcalc));
		}
	}

}
