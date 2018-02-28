package demand.demandObject;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.TimeWindow;

import lsp.shipment.LSPShipment;



public interface ShipperShipment {

	public Id<ShipperShipment> getId();
	public double getShipmentSize();
	public TimeWindow getStartTimeWindow();
	public TimeWindow getEndTimeWindow();
	public double getServiceTime();
	public void setLSPShipment(LSPShipment lspShipment);
	public LSPShipment getLSPShipment();
	public void setDemandObject(DemandObject demandObject);
	public DemandObject getDemandObject();
	
}
