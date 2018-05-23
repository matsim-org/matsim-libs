package timeSliceTest;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.SolutionScheduler;
import lsp.shipment.LSPShipment;

public class TimeSliceSolutionScheduler implements SolutionScheduler{

	private LSP lsp;
	private int bufferTime;
	
	
	@Override
	public void scheduleSolutions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public void setBufferTime(int bufferTime) {
		this.bufferTime = bufferTime;
	}
	
	/*Die methode muss anders werden. Wichtig ist der Zeitpunkt in der Reihenfolge. Der entspricht dem Abfahrtszeitpunkt aus der
	 *  entsprechenden Info. Dann wird der auxiliaryCarrier entsprechend zugeordnet
	 */
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
	
}
