package lspReplanningTests;

import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.LSPPlanImpl;
import lsp.ShipmentAssigner;
import lsp.replanning.LSPPlanStrategyManagerFactory;

public class GenericStrategyManagerFactoryImpl implements LSPPlanStrategyManagerFactory {

	@Override
	public GenericStrategyManager<LSPPlan, LSP> createStrategyManager(LSP lsp) {
		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManager<LSPPlan, LSP>();
		ShipmentAssigner tomorrowAssigner = new TomorrowAssigner();
		tomorrowAssigner.setLSP(lsp);
		strategyManager.addStrategy(new TomorrowShipmentAssignerStrategyFactory(tomorrowAssigner).createStrategy(), null, 1);
		return strategyManager;
	}

}
