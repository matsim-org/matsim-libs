/**
 * 
 */
package org.matsim.contrib.cadyts.general;

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
	private final CadytsContextI cContext;
	
	public CadytsExtendedExpBetaPlanChanger(double beta, CadytsContextI cContext ) {
		delegate = new ExpBetaPlanChanger( beta ) ;
		this.cContext = cContext ;
	}
	

	@Override
	public Plan selectPlan(Person person) {
		Plan selectedPlan = delegate.selectPlan(person) ;
		cadyts.demand.Plan<Link> cadytsPlan = cContext.getPlansTranslator().getPlanSteps( selectedPlan ) ;
		cContext.getCalibrator().addToDemand(cadytsPlan) ;
		return selectedPlan ;
	}
	
}
