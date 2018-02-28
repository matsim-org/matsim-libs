package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.core.controler.events.ReplanningEvent;

import demand.decoratedLSP.LSPWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;
import lsp.LSP;

public class MutualReplanningModuleImpl extends MutualReplanningModule{
	
	public MutualReplanningModuleImpl(Collection<LSP> lsps, DemandObjects demandObjects) {
		super(lsps, demandObjects);
	}
	
	@Override
	void replanLSPs(ReplanningEvent event) {
		for(LSP lsp : lsps) {
			if(lsp instanceof LSPWithOffers) {
				LSPWithOffers lspWithOffers = (LSPWithOffers)lsp;
				if(lspWithOffers.getReplanner()!= null) {
					lspWithOffers.getReplanner().replan(event);
				}
				if(lspWithOffers.getOfferUpdater()!= null) {
					lspWithOffers.getOfferUpdater().updateOffers();
				}
			}
		}
	}

	@Override
	void replanDemandObjects(ReplanningEvent event) {
		for(DemandObject demandObject : demandObjects) {
			if(demandObject.getReplanner()!= null) {
				demandObject.getReplanner().replan(lsps, event);
			}
		}
	}

}
