package lsp.shipment;

import java.util.Map;

import org.matsim.api.core.v01.Id;

public interface ShipmentPlan {

	LSPShipment getShipment();

	Map<Id<ShipmentPlanElement>, ShipmentPlanElement> getPlanElements();

	void addPlanElement( Id<ShipmentPlanElement> id, ShipmentPlanElement element );
	
	ShipmentPlanElement getMostRecentEntry();
	
	void clear();
	
}
