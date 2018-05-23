package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;
import lsp.shipment.ScheduledShipmentLoad.Builder;

public class LoggedShipmentLoad implements AbstractShipmentPlanElement {

	private final String type = "LOAD";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Carrier> carrierId;
	private Id<Link> linkId;
	
	public static class Builder{
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<Resource> resourceId;
		private Id<Carrier> carrierId;
		private Id<Link> linkId;
		
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
	
		public Builder setLinkId(Id<Link> linkId){
			this.linkId = linkId;
			return this;
		}
		
		public Builder setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
			return this;
		}
		
		public LoggedShipmentLoad build(){
			return new LoggedShipmentLoad(this);
		}
	}
	
	private LoggedShipmentLoad(LoggedShipmentLoad.Builder builder){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
		this.carrierId = builder.carrierId;
		this.linkId = builder.linkId;
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

}
