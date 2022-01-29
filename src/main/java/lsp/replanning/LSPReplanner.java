package lsp.replanning;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;

public interface LSPReplanner {

	void replan(ReplanningEvent event);
	GenericStrategyManager<LSPPlan,LSP> getStrategyManager();
	void setStrategyManager(GenericStrategyManager<LSPPlan, LSP> manager);
	void setLSP(LSP lsp);
}
