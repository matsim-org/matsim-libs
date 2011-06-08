/**
 * 
 */
package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author balmermi
 *
 */
public class MultiplePlanAlgorithmRunner implements PlanAlgorithm {

	private final List<PlanAlgorithm> planAlgorithms = new ArrayList<PlanAlgorithm>();
	
	public final boolean add(PlanAlgorithm planAlgorithm) {
		if (planAlgorithm == null) { return false; }
		planAlgorithms.add(planAlgorithm);
		return true;
	}
	
	@Override
	public void run(Plan plan) {
		for (PlanAlgorithm planAlgorithm : planAlgorithms) {
			planAlgorithm.run(plan);
		}
	}
}
