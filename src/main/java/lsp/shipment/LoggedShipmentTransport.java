package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;
import lsp.shipment.LoggedShipmentLoad.Builder;

public class LoggedShipmentTransport implements AbstractShipmentPlanElement {

	private final String type = "TRANSPORT";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private Id<Carrier> carrierId;
	
	public static class Builder{
		private double startTime;
		private LogisticsSolutionElement element;
		private Id<Resource> resourceId;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private Id<Carrier> carrierId;
		
		private Builder(){
		}
		
		public static Builder newInstance(){
			return new Builder();
		}
		
		public Builder setStartTime(double startTime){
			this.startTime = startTime;
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
	
		public Builder setFromLinkId(Id<Link> fromLinkId){
			this.fromLinkId = fromLinkId;
			return this;
		}
		
		public Builder setToLinkId(Id<Link> toLinkId){
			this.toLinkId = toLinkId;
			return this;
		}
		
		public Builder setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
			return this;
		}
		
		public LoggedShipmentTransport build(){
			return new LoggedShipmentTransport(this);
		}
	}
	
	private LoggedShipmentTransport(LoggedShipmentTransport.Builder builder){
		this.startTime = builder.startTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.fromLinkId = builder.fromLinkId;
		this.carrierId = builder.carrierId;
		this.toLinkId = builder.toLinkId;
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
