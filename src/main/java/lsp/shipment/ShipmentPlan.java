package lsp.shipment;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public interface ShipmentPlan {

	LSPShipment getShipment();

	HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement> getPlanElements();

	void addPlanElement( Id<ShipmentPlanElement> id, ShipmentPlanElement element );
	
	ShipmentPlanElement getMostRecentEntry();
	
	void clear();
	
}
