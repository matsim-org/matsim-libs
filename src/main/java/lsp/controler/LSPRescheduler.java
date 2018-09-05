package lsp.controler;

import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.handler.EventHandler;

import lsp.LSP;
import lsp.LSPs;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.shipment.LSPShipment;



public class LSPRescheduler implements BeforeMobsimListener{

	
	private LSPs lsps;
	
	public LSPRescheduler(LSPs lsps) {
		this.lsps = lsps;
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent arg0) {
		if(arg0.getIteration() !=  0) {
			for(LSP lsp : lsps.getLSPs().values()){
				for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
					solution.getShipments().clear();
					for(LogisticsSolutionElement element : solution.getSolutionElements()) {
						element.getIncomingShipments().clear();
						element.getOutgoingShipments().clear();
					}
				}
				
				for(LSPShipment shipment : lsp.getShipments()) {
					shipment.getSchedule().clear();
					shipment.getLog().clear();
					lsp.getSelectedPlan().getAssigner().assignShipment(shipment);
				}
				lsp.scheduleSoultions();
			}		
		}		
	}
}
