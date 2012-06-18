package playground.pieter.mentalsim.controler.listeners;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
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
public class MentalSimSubSetSimulationListener implements ControlerListener,
		IterationStartsListener {
	final static String CONFIG_MODULE_NAME = "MentalSim";
	final static String POP_SWAP_ITERS = "populationSwapIters";
	final static String OFFLINE_PLAN_SIZE = "maxOfflineAgentPlanMemorySize";
	MentalSimControler controler;
	boolean fakePopulationActive = false;

	public MentalSimSubSetSimulationListener(MentalSimControler c) {
		this.controler = c;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

//		int iterationsFromStart = event.getIteration()
//				- controler.getFirstIteration();
		
//		only perform subset simulation if it's required
		if(!this.controler.isSimulateSubsetPersonsOnly())
			return;
		
		if (!MobSimSwitcher.expensiveIter && !fakePopulationActive) {
			controler.markMentalSimAgents(SimpleAnnealer.currentProportion);
			fakePopulationActive = true;
			String offLineMaxPlansPerAgent = controler.getConfig().getParam(
					CONFIG_MODULE_NAME, OFFLINE_PLAN_SIZE);
			int offLineplansPerAgent = offLineMaxPlansPerAgent != null ? Integer
					.parseInt(offLineMaxPlansPerAgent) : 5;
			controler.getStrategyManager().setMaxPlansPerAgent(
					offLineplansPerAgent);
			// we want to execute plans with only non-selector re-planning
			// strategies operating, so need to set that proportion for all
			// MentalSim iterations
			SimpleAnnealer.anneal(event, 0.99999);
		}

		if (fakePopulationActive && MobSimSwitcher.expensiveIter) {
			controler
					.stripOutMentalSimPlansExceptSelected(new ExpBetaPlanSelector(
							new PlanCalcScoreConfigGroup()));
			controler.getStrategyManager().setMaxPlansPerAgent(
					Integer.parseInt(controler.getConfig().getParam("strategy",
							"maxAgentPlanMemorySize")));
			fakePopulationActive = false;
		}
		// if (iterationsFromStart % fullMobSimIterationFrequency != 0) {
		// String maxOfflineAgentPlanMemorySize = controler.getConfig()
		// .getParam(CONFIG_MODULE_NAME, POP_SWAP_ITERS);
		// if (maxOfflineAgentPlanMemorySize != null) {
		// controler.getStrategyManager().setMaxPlansPerAgent(
		// Integer.parseInt(maxOfflineAgentPlanMemorySize));
		// }
		// }
	}
}
