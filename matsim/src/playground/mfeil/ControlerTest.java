/**
 * 
 */
package playground.mfeil;


import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.selectors.RandomPlanSelector;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to refer to the StrategyManagerConfigLoaderTest
 */
public class ControlerTest extends org.matsim.controler.Controler {
	public ControlerTest (String [] args){
		super(args);
	}
		/**
		 * @return A fully initialized StrategyManager for the plans replanning.
		 */
	
	
	@Override
		protected StrategyManager loadStrategyManager() {
			StrategyManager manager = new StrategyManager();			
			PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
			StrategyModule planomatXStrategyModule = new PlanomatXInitialiser(this, legTravelTimeEstimator);
			 // Note that legTravelTimeEstimator is given as an argument here while all other arguments for the 
			 // router algorithm are retrieved in the PlanomatXInitialiser. Both is possible. Should be 
			 // harmonised later on.
			strategy.addStrategyModule(planomatXStrategyModule);
			manager.addStrategy(strategy, 1);
			return manager;
		}
	
}
