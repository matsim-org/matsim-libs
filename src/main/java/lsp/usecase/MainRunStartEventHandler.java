package lsp.usecase;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import lsp.events.TourStartEvent;
import lsp.events.TourStartEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;

public class MainRunStartEventHandler implements TourStartEventHandler {

	private LSPShipment lspShipment;
	private CarrierService carrierService;
	private LogisticsSolutionElement solutionElement;
	private CarrierResource resource;
	
	
	public MainRunStartEventHandler (LSPShipment lspShipment, CarrierService carrierService, LogisticsSolutionElement solutionElement, CarrierResource resource){
		this.lspShipment=lspShipment;
		this.carrierService=carrierService;
		this.solutionElement=solutionElement;
		this.resource=resource;
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(TourStartEvent event) {
		for(TourElement tourElement : event.getTour().getTourElements()){
			if(tourElement instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) tourElement;
				if(serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()){
					logLoad(event);
					logTransport(event);
				}
			}
		}

	}

	private void logLoad(TourStartEvent event){
		ShipmentUtils.LoggedShipmentLoadBuilder builder = ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setLinkId(event.getTour().getStartLinkId());
		double startTime = event.getTime() - getCumulatedLoadingTime(event.getTour());
		builder.setStartTime(startTime);
		builder.setEndTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		ShipmentPlanElement loggedShipmentLoad = builder.build();
		String idString = loggedShipmentLoad.getResourceId() + "" + loggedShipmentLoad.getSolutionElement().getId() + "" + loggedShipmentLoad.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(loadId, loggedShipmentLoad);
	}

	private double getCumulatedLoadingTime(Tour tour){
		double cumulatedLoadingTime = 0;
		for(TourElement tourElement : tour.getTourElements()){
			if(tourElement instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) tourElement;
				cumulatedLoadingTime = cumulatedLoadingTime + serviceActivity.getDuration();
			}
		}
		return cumulatedLoadingTime;
	}

	private void logTransport(TourStartEvent event){
		ShipmentUtils.LoggedShipmentTransportBuilder builder = ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setFromLinkId(event.getTour().getStartLinkId());
		builder.setToLinkId(event.getTour().getEndLinkId());
		builder.setStartTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		LoggedShipmentTransport transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(transportId, transport);
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LogisticsSolutionElement getSolutionElement() {
		return solutionElement;
	}


	public CarrierResource getResource() {
		return resource;
	}

	
	
}
