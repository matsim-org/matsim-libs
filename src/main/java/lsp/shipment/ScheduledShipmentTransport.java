package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

public class ScheduledShipmentTransport implements AbstractShipmentPlanElement{

	private final String type = "TRANSPORT";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Carrier> carrierId;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private CarrierService carrierService;
	
	public static class Builder{
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<Resource> resourceId;
		private Id<Carrier> carrierId;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private CarrierService carrierService;
		
		private Builder(){
		}
		
		public static Builder newInstance(){
			return new Builder();
		}
		
		public Builder setStartTime(double startTime){
			this.startTime = startTime;
			return this;
		}
		
		public Builder setEndTime(double endTime){
			this.endTime = endTime;
			return this;
		}
		
		public Builder setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
			return this;
		}
	
		public Builder setResourceId(Id<Resource> resourceId){
			this.resourceId = resourceId;
			return this;
		}
	
		public Builder setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
			return this;
		}

		public Builder setFromLinkId(Id<Link> fromLinkId){
			this.fromLinkId = fromLinkId;
			return this;
		}
		
		public Builder setToLinkId(Id<Link> toLinkId){
			this.toLinkId = toLinkId;
			return this;
		}
		
		public Builder setCarrierService(CarrierService carrierService){
			this.carrierService = carrierService;
			return this;
		}
		
		public ScheduledShipmentTransport build(){
			return new ScheduledShipmentTransport(this);
		}
	}
	
	private ScheduledShipmentTransport(ScheduledShipmentTransport.Builder builder){
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
