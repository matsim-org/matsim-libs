package lsp.usecase;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.events.ServiceStartEvent;
import lsp.eventhandlers.ServiceStartEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;

public class DistributionServiceEventHandler implements ServiceStartEventHandler {

	private CarrierService carrierService;
	private LSPShipment lspShipment;
	private LogisticsSolutionElement solutionElement;
	private CarrierResource resource;

	public DistributionServiceEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, CarrierResource resource) {
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
	public void handleEvent(ServiceStartEvent event) {
		if (event.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
			logTransport(event);
			logUnload(event);
		}
	}

	private void logTransport(ServiceStartEvent event) {
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if(abstractPlanElement instanceof LoggedShipmentTransport) {
			LoggedShipmentTransport transport = (LoggedShipmentTransport) abstractPlanElement;
			transport.setEndTime(event.getTime());
		}		
	}

	private void logUnload(ServiceStartEvent event) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setLinkId(event.getService().getLocationLinkId());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + event.getService().getServiceDuration());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(unloadId, unload);
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}

	public LSPShipment getLspShipment() {
		return lspShipment;
	}

	public LogisticsSolutionElement getSolutionElement() {
		return solutionElement;
	}

	public CarrierResource getResource() {
		return resource;
	}

	

}
