package example.lsp.lspReplanning;

import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.ShipmentAssigner;
import lsp.replanning.LSPPlanStrategyManagerFactory;

/*package-private*/ class GenericStrategyManagerFactoryImpl implements LSPPlanStrategyManagerFactory {

	@Override
	public GenericStrategyManager<LSPPlan, LSP> createStrategyManager(LSP lsp) {
		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManager<LSPPlan, LSP>();
		ShipmentAssigner tomorrowAssigner = new MaybeTodayAssigner();
		tomorrowAssigner.setLSP(lsp);

		//Warum wird hier an der Stelle im GenericStrategyManager eine spezifische Strategy hinzugefügt? KMT Jun'20
		strategyManager.addStrategy(new TomorrowShipmentAssignerStrategyFactory(tomorrowAssigner).createStrategy(), null, 1);
		return strategyManager;
	}

}
