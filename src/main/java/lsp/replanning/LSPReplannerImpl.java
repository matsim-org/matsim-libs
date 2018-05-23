package lsp.replanning;

import java.util.ArrayList;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;


public class LSPReplannerImpl implements LSPReplanner{

	private LSP lsp;
	private GenericStrategyManager<LSPPlan, LSP> strategyManager;
	
	public LSPReplannerImpl(LSP lsp) {
		this.lsp = lsp;
	}
	
	public LSPReplannerImpl() {
		
	}
	
	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}
	
	@Override
	public void replan(ReplanningEvent event) {
		if(strategyManager != null) {
			ArrayList<LSP> lspList = new ArrayList <LSP>();
			lspList.add(lsp);
			strategyManager.run(lspList, null, event.getIteration(), event.getReplanningContext());
		}
	}

	@Override
	public GenericStrategyManager<LSPPlan, LSP> getStrategyManager() {
		return strategyManager;
	}

	@Override
	public void setStrategyManager(GenericStrategyManager<LSPPlan, LSP> manager) {
		this.strategyManager = manager;
	}

}
