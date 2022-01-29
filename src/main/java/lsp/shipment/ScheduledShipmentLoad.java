package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;



class ScheduledShipmentLoad implements ShipmentPlanElement {

	private final String type = "LOAD";
	private final double startTime;
	private final double endTime;
	private final LogisticsSolutionElement element;
	private final Id<LSPResource> resourceId;
	private final Id<Carrier> carrierId;
	private final Id<Link> linkId;
	private final CarrierService carrierService;

	ScheduledShipmentLoad( ShipmentUtils.ScheduledShipmentLoadBuilder builder ){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.linkId = builder.linkId;
		this.carrierId = builder.carrierId;
		this.carrierService = builder.carrierService;
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

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}

	
}
