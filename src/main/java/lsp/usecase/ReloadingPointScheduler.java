package lsp.usecase;

import java.util.ArrayList;

import lsp.shipment.ShipmentUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentTuple;
import lsp.resources.LSPResource;
import lsp.resources.LSPResourceScheduler;
import lsp.shipment.ShipmentPlanElement;

/*package-private*/ class ReloadingPointScheduler extends LSPResourceScheduler {

	Logger log = Logger.getLogger(ReloadingPointScheduler.class);

	private ReloadingPoint reloadingPoint;
	private double capacityNeedLinear;
	private double capacityNeedFixed;
	private ReloadingPointTourEndEventHandler eventHandler;

	ReloadingPointScheduler(UsecaseUtils.ReloadingPointSchedulerBuilder builder){
		this.shipments = new ArrayList<ShipmentTuple>();
		this.capacityNeedLinear = builder.getCapacityNeedLinear();
		this.capacityNeedFixed = builder.getCapacityNeedFixed();

	}
	
	protected void initializeValues(LSPResource resource) {
		if(resource.getClass() == ReloadingPoint.class){
			this.reloadingPoint = (ReloadingPoint) resource;
		}
	}
	
	protected void scheduleResource() {
		for(ShipmentTuple tupleToBeAssigned: shipments){
			handleWaitingShipment(tupleToBeAssigned);
		}
	}

	@Deprecated //TODO Method has no content, KMT Jul'20
	protected void updateShipments() {
		log.error("This method is not implemented. Nothing will happen here. ");
	}
	
	
	
	private void handleWaitingShipment(ShipmentTuple tupleToBeAssigned){
		updateSchedule(tupleToBeAssigned);
		addShipmentToEventHandler(tupleToBeAssigned);
	}
	
	private void updateSchedule(ShipmentTuple tuple){
		ShipmentUtils.ScheduledShipmentHandleBuilder builder = ShipmentUtils.ScheduledShipmentHandleBuilder.newInstance();
		builder.setStartTime(tuple.getTime());
		builder.setEndTime(tuple.getTime() + capacityNeedFixed + capacityNeedLinear * tuple.getShipment().getCapacityDemand());
		builder.setResourceId(reloadingPoint.getId());
		for(LogisticsSolutionElement element : reloadingPoint.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		builder.setLinkId(reloadingPoint.getStartLinkId());
		ShipmentPlanElement  handle = builder.build();
		String idString = handle.getResourceId() + "" + handle.getSolutionElement().getId() + "" + handle.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
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
	
	public void setEventHandler(ReloadingPointTourEndEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
}
