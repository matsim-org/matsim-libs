package lsp.replanning;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;

public interface LSPReplanner {

	public void replan(ReplanningEvent event);
	public GenericStrategyManager<LSPPlan,LSP> getStrategyManager();
	public void setStrategyManager(GenericStrategyManager<LSPPlan,LSP> manager);
	public void setLSP(LSP lsp);
}
