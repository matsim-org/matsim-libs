package vrp.algorithms.ruinAndRecreate.basics;

import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.api.TourAgent;


/**
 * 
 * @author stefan schroeder
 *
 */

public class Solution {
	
	private Collection<TourAgent> tourAgents;
	
	private CostFunction costFunction;

	public Solution(Collection<TourAgent> tourAgents) {
		super();
		this.tourAgents = tourAgents;
		this.costFunction = new CostFunction();
	}

	public double getResult() {
		return computeTotalResult();
	}
	
	public Collection<TourAgent> getTourAgents(){
		return tourAgents;
	}

	private double computeTotalResult() {
		costFunction.reset();
		for(TourAgent a : tourAgents){
			costFunction.add(a.getTotalCost());
		}
		return costFunction.getTotalCost();
	}
	
	public void setCostFunction(CostFunction costFunction) {
		this.costFunction = costFunction;
	}

	@Override
	public String toString() {
		return "totalResult="+getResult();
	}
}