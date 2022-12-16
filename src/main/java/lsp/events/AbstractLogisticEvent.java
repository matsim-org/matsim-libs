package lsp.events;

import lsp.HasLspShipmentId;
import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

/**
 * A general logistic event contains the information (= {@link Id}) of the
 * 	- the location (= {@link Link})
 * 	- the lspShipment (= {@link lsp.shipment.LSPShipment})
 * 	belonging to it.
 * <p>
 * 	Please note, that general _freight_ events can be found in the freight contrib.
 *
 * @author Kai Martins-Turner (kturner)
 */
public abstract class AbstractLogisticEvent extends Event implements HasLinkId, HasLspShipmentId {

	private final Id<Link> linkId;
	private final Id<LSPShipment> lspShipmentId;

	public AbstractLogisticEvent(double time, Id<Link> linkId, Id<LSPShipment> lspShipmentId) {
		super(time);
		this.linkId = linkId;
		this.lspShipmentId = lspShipmentId;
	}

	/**
	 * @return id of the {@link lsp.shipment.LSPShipment}
	 */
	@Override public final Id<LSPShipment> getLspShipmentId() {
		return lspShipmentId;
	}

	@Override public final Id<Link> getLinkId() {
		return linkId;
	}

	/**
	 * Adds the {@link Id<LSPShipment>} to the list of attributes.
	 *  {@link Id<Link>} is handled by superclass {@link Event}
	 *
	 * @return The map of attributes
	 */
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LspShipmentId, lspShipmentId.toString());
		return attr;
	}
}
