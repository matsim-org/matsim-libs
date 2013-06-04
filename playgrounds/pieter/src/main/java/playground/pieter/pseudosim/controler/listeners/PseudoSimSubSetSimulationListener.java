package playground.pieter.pseudosim.controler.listeners;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;

/**
 * @author fouriep
 *         <p/>
 *         calls on the {@link PseudoSimControler} to replace the orignal
 *         population with a subsample every n iters, according to the scheme
 *         proposed in the MobSimSwitcher config group.
 *         <p/>
 *         Use the current proportion from the <code>SimpleAnnealer</code>,
 *         which will either be its default value or whatever is set by the
 *         annealer itself
 *         <p/>
 *         This listener has to be added to the controler before the SimpleAnnealer
 *         because it overrides that module's annealing setting to force all
 *         selected persons to replan
 * 
 */
public class PseudoSimSubSetSimulationListener implements ControlerListener,
		IterationStartsListener {
	final static String CONFIG_MODULE_NAME = "MentalSim";
	final static String POP_SWAP_ITERS = "populationSwapIters";
	final static String OFFLINE_PLAN_SIZE = "maxOfflineAgentPlanMemorySize";
	PseudoSimControler controler;
	boolean fakePopulationActive = false;
	private PlanSelector planselector;



	public PseudoSimSubSetSimulationListener(PseudoSimControler controler,
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
		
		if (!MobSimSwitcher.isMobSimIteration && !fakePopulationActive) {
			controler.markSubsetAgents(SimpleAnnealer.currentProportion);
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

		if (fakePopulationActive && MobSimSwitcher.isMobSimIteration) {
			controler
					.stripOutMentalSimPlansExceptSelected(this.planselector);
			controler.getStrategyManager().setMaxPlansPerAgent(
					Integer.parseInt(controler.getConfig().getParam("strategy",
							"maxAgentPlanMemorySize")));
			fakePopulationActive = false;
			return;
		}
		if(fakePopulationActive && !MobSimSwitcher.isMobSimIteration){
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
