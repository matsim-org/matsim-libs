package playground.pieter.pseudosim.replanning;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;

/**
 * @author fouriep
 *	ensures that persons that are not selected for mental simulation don't have their plans mutated
 */
public class PseudoSimSubSetSimulationStrategyManager extends StrategyManager {
	PseudoSimControler controler;
	PlanStrategy selectorStrategy=new PlanStrategyImpl(new RandomPlanSelector());
	public PseudoSimSubSetSimulationStrategyManager(PseudoSimControler controler) {
		super();
		this.controler = controler;
	}

	public PlanStrategy chooseStrategy(final Person person) {
		if(!MobSimSwitcher.isMobSimIteration){
			
			if(controler.getAgentPlansMarkedForSubsetMentalSim().getAttribute(person.getId().toString(), controler.AGENT_ATT)==null){
				return selectorStrategy;
			}else{
				return super.chooseStrategy(person);
			}
		}else{
			return super.chooseStrategy(person);
		}

	}
}
