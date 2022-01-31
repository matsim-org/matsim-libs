package lsp.scoring;

import lsp.LSP;
import lsp.LSPs;
import org.matsim.core.controler.events.ScoringEvent;

class LSPScoringModuleImpl implements LSPScoringModule{

	private final LSPs lsps;
	
	LSPScoringModuleImpl(LSPs lsps) {
		this.lsps = lsps;
	}
		
	@Override
	public void notifyScoring(ScoringEvent event) {
		scoreLSPs(event);
	}

	@Override
	public void scoreLSPs(ScoringEvent arg0) {
		for(LSP lsp : lsps.getLSPs().values()) {
			lsp.scoreSelectedPlan();
		}
	}
}
