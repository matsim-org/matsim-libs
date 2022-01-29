package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;



/*package*/ class LoggedShipmentHandle implements ShipmentPlanElement {

	private final String type = "HANDLE";
	private final double startTime;
	private final double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Link> linkId;

	LoggedShipmentHandle(ShipmentUtils.LoggedShipmentHandleBuilder builder){
		this.startTime = builder.getStartTime();
		this.endTime = builder.getEndTime();
		this.element = builder.getElement();
		this.resourceId = builder.getResourceId();
		this.linkId = builder.getLinkId();
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

	public Id<Link> getLinkId() {
		return linkId;
	}

}
