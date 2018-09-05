package lsp.usecase;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import lsp.events.TourStartEvent;
import lsp.events.TourStartEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.LoggedShipmentLoad;
import lsp.shipment.LoggedShipmentTransport;

public class DistributionStartEventHandler implements TourStartEventHandler {

	private CarrierService carrierService;
	private LSPShipment lspShipment;
	private LogisticsSolutionElement element;
	private CarrierResource resource;
	
	public DistributionStartEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, CarrierResource resource){
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.element = element;
		this.resource = resource;
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
		LoggedShipmentLoad.Builder builder = LoggedShipmentLoad.Builder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setLinkId(event.getTour().getStartLinkId());
		builder.setLogisticsSolutionElement(element);
		builder.setResourceId(resource.getId());
		builder.setEndTime(event.getTime());
		builder.setStartTime(event.getTime() - getCumulatedLoadingTime(event.getTour()));
		LoggedShipmentLoad load = builder.build();
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<AbstractShipmentPlanElement> loadId = Id.create(idString, AbstractShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(loadId, load);
	}
	
	private void logTransport(TourStartEvent event){
		LoggedShipmentTransport.Builder builder = LoggedShipmentTransport.Builder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setFromLinkId(event.getTour().getStartLinkId());
		builder.setToLinkId(event.getTour().getEndLinkId());
		builder.setLogisticsSolutionElement(element);
		builder.setResourceId(resource.getId());
		builder.setStartTime(event.getTime());
		LoggedShipmentTransport transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<AbstractShipmentPlanElement> transportId = Id.create(idString, AbstractShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(transportId, transport);
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


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public LogisticsSolutionElement getElement() {
		return element;
	}


	public CarrierResource getResource() {
		return resource;
	}


}
