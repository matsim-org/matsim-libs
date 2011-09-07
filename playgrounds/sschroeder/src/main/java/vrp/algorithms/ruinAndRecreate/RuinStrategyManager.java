/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;
import vrp.basics.RandomNumberGeneration;


/**
 * Manages ruin algorithms.
 * 
 * @author stefan schroeder
 *
 */

public class RuinStrategyManager {
	
	private List<RuinStrategy> strategies = new ArrayList<RuinStrategy>();
	
	private List<Double> weights = new ArrayList<Double>();
	
	private Random random = RandomNumberGeneration.getRandom();
	
	public void setRandom(Random random) {
		this.random = random;
	}

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
		double randomFig = random.nextDouble();
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
