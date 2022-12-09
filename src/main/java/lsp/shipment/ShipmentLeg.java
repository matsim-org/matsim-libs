package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface ShipmentLeg extends ShipmentPlanElement {

	Id<Link> getFromLinkId();


	void setEndTime(double time);
}
