package playground.pieter.mentalsim.controler.listeners;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.pieter.mentalsim.controler.MentalSimControler;
import playground.pieter.mentalsim.mobsim.MentalSimFactory;

/**
 * @author fouriep
 *         <p/>
 *         calls on the {@link MentalSimControler} to replace the orignal
 *         population with a subsample every n iters, according to the scheme
 *         proposed in the MobSimSwitcher config group.
 *         <p/>
 *         Use the current proportion from the <code>SimpleAnnealer</code>,
 *         which will either be its default value or whatever is set by the
 *         annealer itself
 * 
 */
public class MentalSimInit implements ControlerListener,
		IterationStartsListener {
	final static String CONFIG_MODULE_NAME = "MentalSim";
	final static String POP_SWAP_ITERS = "populationSwapIters";
	final static String OFFLINE_PLAN_SIZE = "maxOfflineAgentPlanMemorySize";

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		MentalSimControler controler = ((MentalSimControler) event
				.getControler());

		int fullMobSimIterationFrequency = Integer.parseInt(controler
				.getConfig().getParam(CONFIG_MODULE_NAME, POP_SWAP_ITERS));

		int iterationsFromStart = event.getIteration()
				- event.getControler().getFirstIteration();
		// only start replacing the population from the 2nd iter onwards
		if ((iterationsFromStart - 1) % fullMobSimIterationFrequency == 0
				&& iterationsFromStart > 0) {
			controler
					.createSubSetAndStoreOriginalPopulation(SimpleAnnealer.currentProportion);
		}
		// only start restoring the original population from the 3rd iter
		// onwards
		if (iterationsFromStart % fullMobSimIterationFrequency == 0
				&& iterationsFromStart > 3) {
			controler
					.restoreOriginalPopulationAndReturnSubSetPlan(new ExpBetaPlanSelector(
							new PlanCalcScoreConfigGroup()));
			controler.getStrategyManager().setMaxPlansPerAgent(
					Integer.parseInt(controler.getConfig().getParam("strategy",
							"maxAgentPlanMemorySize")));
		}
		// we want to execute plans with only non-selector re-planning
		// strategies operating, so need to set that proportion for all
		// MentalSim iterations
		if (iterationsFromStart % fullMobSimIterationFrequency != 0) {
			SimpleAnnealer.anneal(event, 0.99999);
			String maxOfflineAgentPlanMemorySize = controler.getConfig()
					.getParam(CONFIG_MODULE_NAME, POP_SWAP_ITERS);
			if (maxOfflineAgentPlanMemorySize != null) {
				controler.getStrategyManager().setMaxPlansPerAgent(
						Integer.parseInt(maxOfflineAgentPlanMemorySize));
			}
		}
	}
}
