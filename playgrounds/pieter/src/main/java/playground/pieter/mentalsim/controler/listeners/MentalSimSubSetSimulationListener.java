package playground.pieter.mentalsim.controler.listeners;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
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
 *         <p/>
 *         This listener has to be added to the controler before the SimpleAnnealer
 *         because it overrides that modules annealing setting to force all
 *         selected persons to replan
 * 
 */
public class MentalSimSubSetSimulationListener implements ControlerListener,
		IterationStartsListener {
	final static String CONFIG_MODULE_NAME = "MentalSim";
	final static String POP_SWAP_ITERS = "populationSwapIters";
	final static String OFFLINE_PLAN_SIZE = "maxOfflineAgentPlanMemorySize";
	MentalSimControler controler;
	boolean fakePopulationActive = false;
	private PlanSelector planselector;



	public MentalSimSubSetSimulationListener(MentalSimControler controler,
			PlanSelector planselector) {
		super();
		this.controler = controler;
		this.planselector = planselector;
	}



	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
//		only perform subset simulation if it's required
		if(!this.controler.isSimulateSubsetPersonsOnly())
			return;
		
		if (!MobSimSwitcher.expensiveIter && !fakePopulationActive) {
			controler.markMentalSimAgents(SimpleAnnealer.currentProportion);
			String offLinePlanSizeString = controler.getConfig().getParam(
					CONFIG_MODULE_NAME, OFFLINE_PLAN_SIZE);
			int offLinePlanSizeInt = offLinePlanSizeString != null ? Integer
					.parseInt(offLinePlanSizeString) : 5;
			controler.getStrategyManager().setMaxPlansPerAgent(
					offLinePlanSizeInt);
			// we want to execute plans with only non-selector re-planning
			// strategies operating, so need to set that proportion for all
			// MentalSim iterations
			SimpleAnnealer.anneal(event, 0.9999999);
			fakePopulationActive = true;
			return;
		}

		if (fakePopulationActive && MobSimSwitcher.expensiveIter) {
			controler
					.stripOutMentalSimPlansExceptSelected(this.planselector);
			controler.getStrategyManager().setMaxPlansPerAgent(
					Integer.parseInt(controler.getConfig().getParam("strategy",
							"maxAgentPlanMemorySize")));
			fakePopulationActive = false;
			return;
		}
		if(fakePopulationActive && !MobSimSwitcher.expensiveIter){
			SimpleAnnealer.anneal(event, 0.9999999);
		}

	}


//	@Override
//	public void notifyStartup(StartupEvent event) {
//		String subsettrigger = this.controler.getConfig().getParam("MentalSim", "simulateSubsetPersonsOnly");
//		if(subsettrigger != null){
//			this.controler.setSimulateSubsetPersonsOnly(Boolean.parseBoolean(subsettrigger));
//		}
//	}
}
