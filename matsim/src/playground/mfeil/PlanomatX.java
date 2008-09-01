/**
 * 
 */
package playground.mfeil;

import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.*;

/**
 * @author Matthias Feil
 * PlanomatX will be the class where to implement the Tabu Search. Currently, it is only 
 * an empty mantle instantiating the PlanOptimizeTimes object. 
 */
public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm {
	

	public PlanomatX (final LegTravelTimeEstimator legTravelTimeEstimator) {

		PlanAlgorithm planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);
	//	planomatAlgorithm =  new PlanOptimizeTimes (legTravelTimeEstimator);

		
	}
	public void run (final Plan plan){
		System.out.println("Das ist nur ein PlanomatX-Test, Pläne bleiben unverändert.");
		
	}

}
