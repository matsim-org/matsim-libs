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
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.LoggedShipmentTransport;
import lsp.shipment.LoggedShipmentUnload;

public class MainRunEndEventHandler implements TourEndEventHandler{

	private LSPShipment lspShipment;
	private CarrierService carrierService;
	private LogisticsSolutionElement solutionElement;
	private CarrierResource resource;
	
	
	public MainRunEndEventHandler (LSPShipment lspShipment, CarrierService carrierService, LogisticsSolutionElement solutionElement, CarrierResource resource){
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
	public void handleEvent(TourEndEvent event) {
		for(TourElement tourElement : event.getTour().getTourElements()){
			if(tourElement instanceof ServiceActivity){
				ServiceActivity serviceActivity = (ServiceActivity) tourElement;
				if(serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()){
					logUnload(event);
					logTransport(event);
				}
			}
		}
	}

	private void logUnload(TourEndEvent event){
		LoggedShipmentUnload.Builder builder  =  LoggedShipmentUnload.Builder.newInstance();
		builder.setStartTime(event.getTime() - getTotalUnloadingTime(event.getTour()));
		builder.setEndTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		LoggedShipmentUnload unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<AbstractShipmentPlanElement> unloadId = Id.create(idString, AbstractShipmentPlanElement.class);
		lspShipment.getLog().getPlanElements().put(unloadId, unload);
	}

	private void logTransport(TourEndEvent event){
		String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		AbstractShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
		if(abstractPlanElement instanceof LoggedShipmentTransport) {
			LoggedShipmentTransport transport = (LoggedShipmentTransport) abstractPlanElement;
			transport.setEndTime(event.getTime()- getTotalUnloadingTime(event.getTour()));
			transport.setToLinkId(event.getTour().getEndLinkId());
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
