package lsp.usecase;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import lsp.events.TourEndEvent;
import lsp.eventhandlers.TourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;

public class CollectionTourEndEventHandler implements TourEndEventHandler {

	private CarrierService carrierService;
	private LSPShipment lspShipment;
	private LogisticsSolutionElement solutionElement;
	private CarrierResource resource;
	
	public CollectionTourEndEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticsSolutionElement element, CarrierResource resource){
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
	public void handleEvent(TourEndEvent event) {
		Tour tour = event.getTour();
		for(TourElement element : tour.getTourElements()){
			if(element instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) element;
					if(serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()){
						logTransport(event, tour);	
						logUnload(event, tour);		
					}
			}
		}
	}

	private void logUnload(TourEndEvent event, Tour tour){
		ShipmentUtils.LoggedShipmentUnloadBuilder builder  =  ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(unloadId, unload);
	}

	private void logTransport(TourEndEvent event, Tour tour){
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if(abstractPlanElement instanceof LoggedShipmentTransport) {
			LoggedShipmentTransport transport = (LoggedShipmentTransport) abstractPlanElement;
			//Auskommentiert, im Rahmen des reducing-public-footprint-Prozesses. Kein Test reagiert drauf. Was "sollte" hier geschehen? KMT(&kai) Jun'20
//			transport.setEndTime(event.getTime());
//			transport.setToLinkId(tour.getEndLinkId());
		}		
	}

	private double getTotalUnloadingTime(Tour tour){
		double totalTime = 0;
		for(TourElement element : tour.getTourElements()){
			if(element instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) element;
				totalTime = totalTime + serviceActivity.getDuration();
			}
		}	
		return totalTime;
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

	
	
	
	

