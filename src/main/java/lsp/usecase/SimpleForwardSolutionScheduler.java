package lsp.usecase;

import java.util.List;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.SolutionScheduler;
import lsp.LSPResource;
import lsp.shipment.LSPShipment;

/**
  * In the class SimpleForwardSolutionScheduler two tasks are performed:
 *
 * 1.) the {@link LSPShipment}s that were assigned to the suitable
 * {@link LogisticsSolution} by the {@link lsp.ShipmentAssigner} in a previous step are handed over to the first
 * {@link LogisticsSolutionElement}.
 *
 * 2.) all {@link LSPResource}s that were handed over to the SimpleForwardSolutionScheduler
 * exogenously, are now scheduled sequentially in an order that was also specified exogenously.
 * This order ensures that each {@link LogisticsSolution} is traversed from the
 * first to the last {@link LogisticsSolutionElement}. During this procedure, the concerned
 * {@link LSPShipment}s  are taken from the collection of incoming shipments, handled by the
 * {@link LSPResource} in charge and then added to the collection of outgoing shipments of the client
 * {@link LogisticsSolutionElement}.
 *
 * The SimpleForwardSolutionScheduler needs the sequence in which the Resources are scheduled as exogenous input.
 *
 * The expression "`forward"' refers to the fact that in both cases the scheduling process starts at the first element
 * of each {@link LogisticsSolution} and from the earliest possible point of time.
 */
/*package-private*/ class SimpleForwardSolutionScheduler implements SolutionScheduler {

	private LSP lsp;
	private final List<LSPResource> resources;
	private int bufferTime;
	
	SimpleForwardSolutionScheduler( List<LSPResource> resources ) {
		this.resources = resources;
	}
	
	@Override
	public void scheduleSolutions() {
		insertShipmentsAtBeginning();
		for(LSPResource resource : resources) {
			for(LSPResource lspResource : lsp.getResources()) {
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
				firstElement.getIncomingShipments().addShipment(shipment.getPickupTimeWindow().getStart(), shipment );
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
