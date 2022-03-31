package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

class ScheduledShipmentUnload implements ShipmentPlanElement {

	private final double startTime;
	private final double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Carrier> carrierId;
	private final Id<Link> linkId;
	private final CarrierService carrierService;

	ScheduledShipmentUnload( ShipmentUtils.ScheduledShipmentUnloadBuilder builder ){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.carrierId = builder.carrierId;
		this.linkId = builder.linkId;
		this.carrierService = builder.carrierService;
	}	
	
	
	@Override
	public String getElementType() {
		String type = "UNLOAD";
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

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}
	
}

