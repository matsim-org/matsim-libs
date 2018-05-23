package lsp.shipment;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public interface AbstractShipmentPlan {

	public LSPShipment getShipment();

	public HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement> getPlanElements();

	public void addPlanElement(Id<AbstractShipmentPlanElement> id, AbstractShipmentPlanElement element);
	
	public AbstractShipmentPlanElement getMostRecentEntry();
	
	public void clear();
	
}