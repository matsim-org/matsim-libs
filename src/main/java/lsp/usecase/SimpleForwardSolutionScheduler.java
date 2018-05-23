package lsp.usecase;

import java.util.ArrayList;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.shipment.LSPShipment;

public class SimpleForwardSolutionScheduler implements SolutionScheduler {

	private LSP lsp;
	private ArrayList<Resource> resources;
	private int bufferTime;
	
	public SimpleForwardSolutionScheduler(ArrayList<Resource> resources) {
		this.resources = resources;
	}
	
	@Override
	public void scheduleSolutions() {
		insertShipmentsAtBeginning();
		for(Resource resource : resources) {
			for(Resource lspResource : lsp.getResources()) {
				if(lspResource == resource) {
					lspResource.schedule(bufferTime);
				}
			}
		}
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	
	private void insertShipmentsAtBeginning() {
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			LogisticsSolutionElement firstElement = getFirstElement(solution);
			for(LSPShipment shipment : solution.getShipments() ) {
				firstElement.getIncomingShipments().addShipment(shipment.getStartTimeWindow().getStart(), shipment);
			}
		}
	}
	
	private LogisticsSolutionElement getFirstElement(LogisticsSolution solution){
		for(LogisticsSolutionElement element : solution.getSolutionElements()){
			if(element.getPreviousElement() == null){
				return element;
			}
			
		}
		return null;
	}

	@Override
	public void setBufferTime(int bufferTime) {
		this.bufferTime = bufferTime;
	}
}
