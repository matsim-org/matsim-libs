package org.matsim.freight.logistics.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;

/**
 * A shipment plan element that describes a transport leg between two links. Implemented by
 * {@code ScheduledLspShipmentTransport} and {@code LoggedLspShipmentTransport}.
 */
public interface LspShipmentLeg extends LspShipmentPlanElement {
  /** @return the link where the transport leg ends. */
  Id<Link> getToLinkId();

  /** @param endLinkId the link where the transport leg ends. */
  void setToLinkId(Id<Link> endLinkId);

  /** @return the id of the carrier performing the transport. */
  Id<Carrier> getCarrierId();

  /** @return the link where the transport leg starts. */
  Id<Link> getFromLinkId();

  /** @return the carrier service used for the transport leg. */
  CarrierService getCarrierService();

  /** @param time the simulation time at which the transport leg ends. */
  void setEndTime(double time);
}
