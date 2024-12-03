package org.matsim.freight.logistics.events;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.logistics.HasLspShipmentId;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * A general logistic event contains the information (= {@link Id}) of the - the location (= {@link
 * Link}) - the lspShipment (= {@link LspShipment}) belonging to it.
 *
 * <p>Please note, that general _freight_ events can be found in the freight contrib.
 *
 * @author Kai Martins-Turner (kturner)
 */
public abstract class AbstractLogisticEvent extends Event implements HasLinkId, HasLspShipmentId {

  private final Id<Link> linkId;
  private final Id<LspShipment> lspShipmentId;

  public AbstractLogisticEvent(double time, Id<Link> linkId, Id<LspShipment> lspShipmentId) {
    super(time);
    this.linkId = linkId;
    this.lspShipmentId = lspShipmentId;
  }

  /**
   * @return id of the {@link LspShipment}
   */
  @Override
  public final Id<LspShipment> getLspShipmentId() {
    return lspShipmentId;
  }

  @Override
  public final Id<Link> getLinkId() {
    return linkId;
  }

  /**
   * Adds the {@link Id<LspShipment>} to the list of attributes. {@link Id<Link>} is handled by
   * superclass {@link Event}
   *
   * @return The map of attributes
   */
  @Override
  public Map<String, String> getAttributes() {
    Map<String, String> attr = super.getAttributes();
    attr.put(ATTRIBUTE_LSP_SHIPMENT_ID, lspShipmentId.toString());
    return attr;
  }
}
