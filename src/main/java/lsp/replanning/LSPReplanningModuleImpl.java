package lsp.replanning;

import org.matsim.core.controler.events.ReplanningEvent;

import lsp.LSP;
import lsp.LSPs;

public class LSPReplanningModuleImpl implements LSPReplanningModule{

	private LSPs lsps;
	
	public LSPReplanningModuleImpl(LSPs lsps) {
		this.lsps = lsps;
	}
		
	@Override
	public void notifyReplanning(ReplanningEvent arg0) {
		replanLSPs(arg0);
		
	}
	
	@Override
	public void replanLSPs(ReplanningEvent arg0) {
		for(LSP lsp : lsps.getLSPs().values()) {
			lsp.replan(arg0);
		}
	}
}
