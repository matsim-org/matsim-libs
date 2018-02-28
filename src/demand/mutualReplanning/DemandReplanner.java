package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlanImpl;

public interface DemandReplanner {
	public void replan(Collection<LSP> lsps, ReplanningEvent event);
	public GenericStrategyManager<LSPPlanImpl,LSP> getStrategyManager();
	public void setStrategyManager(GenericStrategyManager<LSPPlanImpl,LSP> manager);
}
