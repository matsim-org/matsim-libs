package lsp.usecase;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.events.ServiceEndEvent;
import lsp.events.ServiceEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;

public class CollectionServiceEventHandler implements ServiceEndEventHandler {

	private CarrierService carrierService;
	private LSPShipment lspShipment;
	private LogisticsSolutionElement solutionElement;
	private CarrierResource resource;
	
	public CollectionServiceEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, CarrierResource resource){
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.solutionElement = element;
		this.resource = resource;
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ServiceEndEvent event) {
		if(event.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()){
			logTransport(event);
			logLoad(event);
		}	
	}

	private void logLoad(ServiceEndEvent event){
		ShipmentUtils.LoggedShipmentLoadBuilder builder  =  ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setStartTime(event.getTime() - event.getService().getServiceDuration());
		builder.setEndTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setLinkId(event.getService().getLocationLinkId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement loggedShipmentLoad = builder.build();
		String idString = loggedShipmentLoad.getResourceId() + "" + loggedShipmentLoad.getSolutionElement().getId() + "" + loggedShipmentLoad.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(loadId, loggedShipmentLoad);
	}

	private void logTransport(ServiceEndEvent event){
		ShipmentUtils.LoggedShipmentTransportBuilder builder  =  ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setFromLinkId(event.getService().getLocationLinkId());
		builder.setCarrierId(event.getCarrierId());
		LoggedShipmentTransport transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(transportId, transport);
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public LogisticsSolutionElement getElement() {
		return solutionElement;
	}


	public Id<Resource> getResourceId() {
		return resource.getId();
	}


}
