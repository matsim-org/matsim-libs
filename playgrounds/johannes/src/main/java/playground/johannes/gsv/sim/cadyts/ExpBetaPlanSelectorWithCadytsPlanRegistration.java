/**
 * 
 */
package playground.johannes.gsv.sim.cadyts;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
public final class ExpBetaPlanSelectorWithCadytsPlanRegistration<T> implements PlanSelector {

	private final ExpBetaPlanSelector<Plan, Person> delegate ;
	private final CadytsContextI<T> cContext;
	
	public ExpBetaPlanSelectorWithCadytsPlanRegistration(double beta, CadytsContextI<T> cContext ) {
		delegate = new ExpBetaPlanSelector<Plan, Person>( beta ) ;
		this.cContext = cContext ;
	}
	

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		Plan selectedPlan = delegate.selectPlan(person) ;
		cadyts.demand.Plan<T> cadytsPlan = cContext.getPlansTranslator().getCadytsPlan( selectedPlan ) ;
		cContext.getCalibrator().addToDemand(cadytsPlan) ;
		return selectedPlan ;
	}
	
}
