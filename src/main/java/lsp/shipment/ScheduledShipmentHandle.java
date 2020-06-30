package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

class ScheduledShipmentHandle implements ShipmentPlanElement {

	private final String type = "HANDLE";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Link> linkId;

	ScheduledShipmentHandle( ShipmentUtils.ScheduledShipmentHandleBuilder builder ){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
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
	public Id<Resource> getResourceId() {
		return resourceId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

}
