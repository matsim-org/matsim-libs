package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

public class LoggedShipmentUnload implements ShipmentPlanElement {

	private final String type = "UNLOAD";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Carrier> carrierId;
	private Id<Link> linkId;

	LoggedShipmentUnload( ShipmentUtils.LoggedShipmentUnloadBuilder builder ){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.carrierId = builder.carrierId;
		this.linkId = builder.linkId;
	}
	
	
	@Override
	public LogisticsSolutionElement getSolutionElement() {
		return element;
	}

	@Override
	public Id<Resource> getResourceId() {
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

}
