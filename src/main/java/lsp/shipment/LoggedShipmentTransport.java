package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

public final class LoggedShipmentTransport implements ShipmentPlanElement {
	// yyyy cannot make package-private since used outside package.  kai, jun'20

	private final String type = "TRANSPORT";
	private final double startTime;
	private double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> fromLinkId;
	private Id<Link> toLinkId;

	LoggedShipmentTransport(ShipmentUtils.LoggedShipmentTransportBuilder builder){
		this.startTime = builder.getStartTime();
		this.element = builder.getElement();
		this.resourceId = builder.getResourceId();
		this.fromLinkId = builder.getFromLinkId();
		this.toLinkId = builder.getToLinkId();
	}
	
	
	@Override
	public LogisticsSolutionElement getSolutionElement() {
		return element;
	}

	@Override
	public Id<LSPResource> getResourceId() {
		return resourceId;
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

	public void setEndTime(double endTime){
		this.endTime = endTime;
	}


	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}


	public Id<Link> getToLinkId() {
		return toLinkId;
	}


	public void setToLinkId(Id<Link> toLinkId) {
		this.toLinkId = toLinkId;
	}
	
	
}
