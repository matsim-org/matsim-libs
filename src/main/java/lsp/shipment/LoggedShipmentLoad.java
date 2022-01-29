package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

/*package-private*/ class LoggedShipmentLoad implements ShipmentPlanElement {

	private final String type = "LOAD";
	private final double startTime;
	private final double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Carrier> carrierId;
	private final Id<Link> linkId;

	LoggedShipmentLoad(ShipmentUtils.LoggedShipmentLoadBuilder builder){
		this.startTime = builder.getStartTime();
		this.endTime = builder.getEndTime();
		this.element = builder.getElement();
		this.resourceId = builder.getResourceId();
		this.carrierId = builder.getCarrierId();
		this.linkId = builder.getLinkId();
	}
	
	
	@Override
	public String getElementType() {
		return type;
	}

	@Override
	public double getStartTime() {
		return startTime;
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	@Override
	public LogisticsSolutionElement getSolutionElement() {
		return element;
	}

	@Override
	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

}
