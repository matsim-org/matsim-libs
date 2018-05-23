package lsp.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;



public class LoggedShipmentHandle implements AbstractShipmentPlanElement {

	private final String type = "HANDLE";
	private double startTime;
	private double endTime;
	private LogisticsSolutionElement element;
	private Id<Resource> resourceId;
	private Id<Link> linkId;
	
	public static class Builder{
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<Resource> resourceId;
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
		
		public LoggedShipmentHandle build(){
			return new LoggedShipmentHandle(this);
		}
	}
	
	private LoggedShipmentHandle(LoggedShipmentHandle.Builder builder){
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.element = builder.element;
		this.resourceId = builder.resourceId;
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

	public Id<Link> getLinkId() {
		return linkId;
	}

}
