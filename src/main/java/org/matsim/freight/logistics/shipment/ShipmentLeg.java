package org.matsim.freight.logistics.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;

public interface ShipmentLeg extends ShipmentPlanElement {
	Id<Link> getToLinkId();

	void setToLinkId(Id<Link> endLinkId);

	Id<Carrier> getCarrierId();

	Id<Link> getFromLinkId();

	CarrierService getCarrierService();

	void setEndTime(double time);
}
