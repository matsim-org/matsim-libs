package lsp.replanning;

import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LSPPlan;

public interface LSPPlanStrategyManagerFactory {
	
	public GenericStrategyManager<LSPPlan, LSP> createStrategyManager(LSP lsp);

}
