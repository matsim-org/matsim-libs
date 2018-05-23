package lsp.usecase;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentTuple;
import lsp.resources.Resource;
import lsp.resources.ResourceScheduler;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.ScheduledShipmentHandle;

public class ReloadingPointScheduler extends ResourceScheduler {

	private ReloadingPoint reloadingPoint;
	private double capacityNeedLinear;
	private double capacityNeedFixed;
	private ReloadingPointEventHandler eventHandler;

	public static class Builder{
		private double capacityNeedLinear;
		private double capacityNeedFixed;
		
		private Builder(){
		}
		
		public static Builder newInstance(){
			return new Builder();
		}
		
				
		public Builder setCapacityNeedLinear(double capacityNeedLinear){
			this.capacityNeedLinear = capacityNeedLinear;
			return this;
		}
		
		public Builder setCapacityNeedFixed(double capacityNeedFixed){
			this.capacityNeedFixed = capacityNeedFixed;
			return this;
		}
	
		public ReloadingPointScheduler build(){
			return new ReloadingPointScheduler(this);
		}
	}
	
	private ReloadingPointScheduler(ReloadingPointScheduler.Builder builder){
		this.shipments = new ArrayList<ShipmentTuple>();
		this.capacityNeedLinear = builder.capacityNeedLinear;
		this.capacityNeedFixed = builder.capacityNeedFixed;
		
	}
	
	protected void initializeValues(Resource resource) {
		if(resource.getClass() == ReloadingPoint.class){
			this.reloadingPoint = (ReloadingPoint) resource;
		}
	}
	
	protected void scheduleResource() {
		for(ShipmentTuple tupleToBeAssigned: shipments){
			handleWaitingShipment(tupleToBeAssigned);
		}
	}
	
	protected void updateShipments() {
		
	}
	
	
	
	private void handleWaitingShipment(ShipmentTuple tupleToBeAssigned){
		updateSchedule(tupleToBeAssigned);
		addShipmentToEventHandler(tupleToBeAssigned);
	}
	
	private void updateSchedule(ShipmentTuple tuple){
		ScheduledShipmentHandle.Builder builder = ScheduledShipmentHandle.Builder.newInstance();
		builder.setStartTime(tuple.getTime());
		builder.setEndTime(tuple.getTime() + capacityNeedFixed + capacityNeedLinear * tuple.getShipment().getCapacityDemand());
		builder.setResourceId(reloadingPoint.getId());
		for(LogisticsSolutionElement element : reloadingPoint.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		builder.setLinkId(reloadingPoint.getStartLinkId());
		ScheduledShipmentHandle  handle = builder.build();
		String idString = handle.getResourceId() + "" + handle.getSolutionElement().getId() + "" + handle.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, handle);
	}
	
	private void addShipmentToEventHandler(ShipmentTuple tuple){
		for(LogisticsSolutionElement element : reloadingPoint.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				eventHandler.addShipment(tuple.getShipment(), element);
				break;
			}
		}
	}
	
	public double getCapacityNeedLinear() {
		return capacityNeedLinear;
	}


	public double getCapacityNeedFixed() {
		return capacityNeedFixed;
	}


	public ReloadingPoint getReloadingPoint() {
		return reloadingPoint;
	}


	public void setReloadingPoint(ReloadingPoint reloadingPoint) {
		this.reloadingPoint = reloadingPoint;
	}
	
	public void setEventHandler(ReloadingPointEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
}
