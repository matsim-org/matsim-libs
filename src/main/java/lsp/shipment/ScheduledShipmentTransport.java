package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

public final class ScheduledShipmentTransport implements ShipmentPlanElement {
	// yyyy cannot make package-private since used in one instanceof outside package.  kai, jun'20

	private final String type = "TRANSPORT";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Carrier> carrierId;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private CarrierService carrierService;

	ScheduledShipmentTransport( ShipmentUtils.ScheduledShipmentTransportBuilder builder ){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.carrierId = builder.carrierId;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
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
	public Id<Resource> getResourceId() {
		return resourceId;
	}


	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	public Id<Carrier> getCarrierId() {
		return carrierId;
	}


	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}
	
}
