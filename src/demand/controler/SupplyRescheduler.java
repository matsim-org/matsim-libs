package demand.controler;

import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import demand.decoratedLSP.LSPsWithOffers;
import lsp.LSP;


public class SupplyRescheduler implements BeforeMobsimListener{

	private LSPsWithOffers lsps;
	
	public SupplyRescheduler(LSPsWithOffers  lsps) {
		this.lsps = lsps;
	}
	
	
	public void notifyBeforeMobsim(BeforeMobsimEvent arg0) {
		if(arg0.getIteration() !=  0) {
			for(LSP lsp : lsps.getLSPs().values()){
				lsp.scheduleSoultions();
			}		
		}	
	}
}
