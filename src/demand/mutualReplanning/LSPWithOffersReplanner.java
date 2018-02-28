package demand.mutualReplanning;

import java.util.ArrayList;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPWithOffers;
import demand.offer.OfferUpdater;
import lsp.LSP;
import lsp.LSPPlan;
import lsp.replanning.LSPReplanner;

public class LSPWithOffersReplanner implements LSPReplanner{

	private LSPDecorator lsp;
	private GenericStrategyManager<LSPPlan, LSP> strategyManager;
	private OfferUpdater offerUpdater;
	
	public LSPWithOffersReplanner(LSPDecorator  lsp) {
		this.lsp = lsp;
	}
	
	@Override
	public void replan(ReplanningEvent event) {
		if(strategyManager != null) {
			ArrayList<LSP> lspList = new ArrayList <LSP>();
			lspList.add(lsp);
			strategyManager.run(lspList, null, event.getIteration(), event.getReplanningContext());
		}
		if(offerUpdater != null) {
			offerUpdater.updateOffers();
		}
	}

	@Override
	public GenericStrategyManager<LSPPlan, LSP> getStrategyManager() {
		return strategyManager;
	}

	@Override
	public void setStrategyManager(GenericStrategyManager<LSPPlan, LSP> strategyManager) {
		this.strategyManager = strategyManager;
	}

	public void setOfferUpdater(OfferUpdater offerUpdater) {
		this.offerUpdater = offerUpdater;
		offerUpdater.setLSP(lsp);
	}

	public OfferUpdater getOfferUpdater() {
		return offerUpdater;
	}
}
