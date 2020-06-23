package lsp.shipment;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public interface ShipmentPlan {

	public LSPShipment getShipment();

	public HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement> getPlanElements();

	public void addPlanElement(Id<ShipmentPlanElement> id, ShipmentPlanElement element);
	
	public ShipmentPlanElement getMostRecentEntry();
	
	public void clear();
	
}