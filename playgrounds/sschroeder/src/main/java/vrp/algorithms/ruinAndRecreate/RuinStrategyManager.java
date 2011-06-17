package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;


/**
 * Manages ruin algorithms.
 * 
 * @author stefan schroeder
 *
 */

public class RuinStrategyManager {
	
	private List<RuinStrategy> strategies = new ArrayList<RuinStrategy>();
	
	private List<Double> weights = new ArrayList<Double>();
	
	public RuinStrategyManager() {
		super();
	}

	/**
	 * Weight is the probability of the ruin-strategy to be chosen.
	 * 
	 * @param strat
	 * @param weight
	 */
	public void addStrategy(RuinStrategy strat, Double weight){
		strategies.add(strat);
		weights.add(weight);
	}
	
	public RuinStrategy getRandomStrategy(){
		double randomFig = MatsimRandom.getRandom().nextDouble();
		double sumWeight = 0.0;
		for(int i=0;i<weights.size();i++){
			sumWeight += weights.get(i);
			if(randomFig < sumWeight){
				return strategies.get(i);
			}
		}
		return null;
	}

}
