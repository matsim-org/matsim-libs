/**
 * 
 */
package org.matsim.contrib.cadyts.general;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import cadyts.utilities.math.Vector;

/**
 * @author nagel
 *
 */
@Deprecated // please test before using.  If it works, remove deprecated tag.  Optimally, write test case. kai, dec'13
public final class PlanSelectionByCadyts<T> implements PlanSelector {

	private final CadytsContextI<T> cContext;
	private double beta;
	
	public PlanSelectionByCadyts(double beta, CadytsContextI<T> cContext ) {
		this.cContext = cContext ;
		this.beta = beta ;
	}
	

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		List<cadyts.demand.Plan<T>> plans = new ArrayList<cadyts.demand.Plan<T>>() ;
		Vector choiceProbs = new Vector( person.getPlans().size() ) ;
		int pos = 0 ;
		for ( Plan plan : person.getPlans() ) {
			plans.add( cContext.getPlansTranslator().getCadytsPlan(plan) ) ;

			choiceProbs.add(pos, ExpBetaPlanSelector.getSelectionProbability(new ExpBetaPlanSelector(beta), plan.getPerson(), plan) );
			// I guess these are supposed to be the prior probabilities. If so, than the above shoudl be correct (albeit a bit expensive).
			// I would have expected that these are internally ignored when brute force is switched on, but they are not.
			// I think that what it does is to use them when plans are equal according to cadyts. kai, dec'13
			
			pos++ ;
		}
		int idx = cContext.getCalibrator().selectPlan(plans, choiceProbs) ;

//		cContext.getCalibrator().addToDemand(cadytsPlan) ;
		// I _think_ that's done inside calibrator.selectPlan. kai, dec'13

		return person.getPlans().get(idx) ;
	}
	
}
