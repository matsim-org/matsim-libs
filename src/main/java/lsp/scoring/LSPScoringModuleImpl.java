package lsp.scoring;

import org.matsim.core.controler.events.ScoringEvent;

import demand.decoratedLSP.LSPDecorators;
import lsp.LSP;
import lsp.LSPs;

public class LSPScoringModuleImpl implements LSPScoringModule{

	private LSPs lsps;
	
	public LSPScoringModuleImpl(LSPs lsps) {
		this.lsps = lsps;
	}
		
	@Override
	public void notifyScoring(ScoringEvent event) {
		scoreLSPs(event);
	}

	@Override
	public void scoreLSPs(ScoringEvent arg0) {
		for(LSP lsp : lsps.getLSPs().values()) {
			if(lsp.getScorer() != null) {
				lsp.scoreSelectedPlan();
			}
		}	
	}
}
