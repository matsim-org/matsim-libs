package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.selectors.PlanSelector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorstPlanForRemovalSelector implements PlanSelector<LSPPlan, LSP> {

	private static final String UNDEFINED_TYPE = "undefined";


	@Override
	public LSPPlan selectPlan(HasPlansAndId<LSPPlan, LSP> lsp) {

		Map<String, Integer> typeCounts = new ConcurrentHashMap<String, Integer>();

		// count how many plans per type an agent has:
		for (LSPPlan plan : lsp.getPlans()) {
			String type = plan.getType();
			if ( type==null ) {
				type = UNDEFINED_TYPE ;
			}
			typeCounts.merge( type, 1, ( a, b ) -> a + b );
		}

		LSPPlan worst = null;
		double worstScore = Double.POSITIVE_INFINITY;
		for (LSPPlan plan : lsp.getPlans()) {
			String type = plan.getType();
			if ( type==null ) {
				type = UNDEFINED_TYPE;
			}
			if ( typeCounts.get( type ) > 1) {
				if (plan.getScore() == null || plan.getScore().isNaN() ) {
					worst = plan;
					worstScore = Double.NEGATIVE_INFINITY;
				} else if ( plan.getScore() < worstScore) {
					worst = plan;
					worstScore = plan.getScore();
				}
			}
		}
		if (worst == null) {
			// there is exactly one plan, or we have of each plan-type exactly one.
			// select the one with worst score globally, or the first one with score=null
			for (LSPPlan plan : lsp.getPlans()) {
				if (plan.getScore() == null || plan.getScore().isNaN() ) {
					return plan;
				}
				if ( plan.getScore() < worstScore) {
					worst = plan;
					worstScore = plan.getScore();
				}
			}
		}
		return worst;
	}
}
