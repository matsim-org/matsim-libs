/**
 * 
 */
package org.matsim.contrib.cadyts.car;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
public final class CadytsExtendedExpBetaPlanChanger implements PlanSelector {

	private final PlanSelector delegate ;
	private final CadytsContext cContext;
	
	public CadytsExtendedExpBetaPlanChanger(double beta, CadytsContext cContext ) {
		delegate = new ExpBetaPlanChanger( beta ) ;
		this.cContext = cContext ;
	}
	

	@Override
	public Plan selectPlan(Person person) {
		Plan selectedPlan = delegate.selectPlan(person) ;
		cadyts.demand.Plan<Link> cadytsPlan = cContext.getPlanToPlanStepBasedOnEvents().getPlanSteps( selectedPlan ) ;
		cContext.getAnalyticalCalibrator().addToDemand(cadytsPlan) ;
		return selectedPlan ;
	}
	
}
