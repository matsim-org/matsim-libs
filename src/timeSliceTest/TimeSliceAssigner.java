package timeSliceTest;

import lsp.LSP;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;

public class TimeSliceAssigner implements ShipmentAssigner{

	private LSP lsp;
	
	
	@Override
	public void assignShipment(LSPShipment shipment) {
		for(LogisticsSolution solution ) {
			
		}
		
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

}
