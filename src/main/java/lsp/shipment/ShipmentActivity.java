package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface ShipmentActivity extends ShipmentPlanElement {
	Id<Link> getLinkId();
}
