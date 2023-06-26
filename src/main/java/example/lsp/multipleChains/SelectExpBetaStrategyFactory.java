package example.lsp.multipleChains;

import lsp.LSP;
import lsp.LSPPlan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

public class SelectExpBetaStrategyFactory {

	public GenericPlanStrategy<LSPPlan, LSP> createStrategy() {
		return new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new PlanCalcScoreConfigGroup()));
	}
}
