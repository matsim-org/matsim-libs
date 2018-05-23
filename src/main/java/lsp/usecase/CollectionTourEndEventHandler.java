package lsp.usecase;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import lsp.events.TourEndEvent;
import lsp.events.TourEndEventHandler;
import lsp.LogisticsSolutionElement;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.LoggedShipmentTransport;
import lsp.shipment.LoggedShipmentUnload;

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
		LoggedShipmentUnload.Builder builder  =  LoggedShipmentUnload.Builder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		LoggedShipmentUnload unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<AbstractShipmentPlanElement> unloadId = Id.create(idString, AbstractShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(unloadId, unload);
	}

	private void logTransport(TourEndEvent event, Tour tour){
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		AbstractShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if(abstractPlanElement instanceof LoggedShipmentTransport) {
			LoggedShipmentTransport transport = (LoggedShipmentTransport) abstractPlanElement;
			transport.setEndTime(event.getTime());
			transport.setToLinkId(tour.getEndLinkId());
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

	
	
	
	

