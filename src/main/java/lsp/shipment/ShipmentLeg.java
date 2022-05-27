package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

public interface ShipmentLeg extends ShipmentPlanElement {
	Id<Link> getToLinkId();
	Id<Carrier> getCarrierId();
	Id<Link> getFromLinkId();
	CarrierService getCarrierService();
	void setEndTime( double time );
	void setToLinkId( Id<Link> endLinkId );
}
