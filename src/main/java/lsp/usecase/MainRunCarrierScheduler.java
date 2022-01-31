package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentWithTime;
import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;
import lsp.resources.LSPResourceScheduler;
import org.matsim.vehicles.VehicleType;


/*package-private*/  class MainRunCarrierScheduler extends LSPResourceScheduler {

	static class LSPCarrierPair{
		private final ShipmentWithTime tuple;
		private final CarrierService service;
		
		public LSPCarrierPair( ShipmentWithTime tuple, CarrierService service ){
			this.tuple = tuple;
			this.service = service;
		}
		
	}
	
	private Carrier carrier;
	private MainRunCarrierAdapter adapter;
	private ArrayList<LSPCarrierPair>pairs;


	/*package-private*/   MainRunCarrierScheduler(){
		this.pairs = new ArrayList<>();
	}
	
	@Override protected void initializeValues( LSPResource resource ) {
		this.pairs = new ArrayList<>();
		if(resource.getClass() == MainRunCarrierAdapter.class){
			this.adapter = (MainRunCarrierAdapter) resource;
			this.carrier = adapter.getCarrier();
			this.carrier.getServices().clear();
			this.carrier.getShipments().clear();
			this.carrier.getPlans().clear();
			this.carrier.setSelectedPlan(null);
		}
	}
	
	@Override protected void scheduleResource() {
		int load = 0;
		ArrayList<ShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(shipments);
		copyOfAssignedShipments.sort(new ShipmentComparator());
		ArrayList<ShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();

		for( ShipmentWithTime tuple : copyOfAssignedShipments){
			VehicleType vehicleType = carrier.getCarrierCapabilities().getVehicleTypes().iterator().next();
			if((load + tuple.getShipment().getSize()) <= vehicleType.getCapacity().getOther().intValue() ){
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			}
			else{
				load=0;
				CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
				scheduledTours.addAll(plan.getScheduledTours());
				shipmentsInCurrentTour.clear();
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
			}
			
		}
		if(!shipmentsInCurrentTour.isEmpty()) {
			CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
			scheduledTours.addAll(plan.getScheduledTours());
			shipmentsInCurrentTour.clear();
		}
		CarrierPlan plan = new CarrierPlan(carrier,scheduledTours);
		carrier.setSelectedPlan(plan);
	}
	
	
	
	private CarrierPlan createPlan(Carrier carrier, ArrayList<ShipmentWithTime> tuples ){
		
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(adapter.getNetwork(), adapter.getCarrier().getCarrierCapabilities().getVehicleTypes());
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		Collection<ScheduledTour> tours = new ArrayList<>();
		
		Tour.Builder tourBuilder = Tour.Builder.newInstance();
		tourBuilder.scheduleStart(Id.create(adapter.getStartLinkId(), Link.class));

		double totalLoadingTime = 0;
		double latestTupleTime = 0;

		for ( ShipmentWithTime tuple : tuples){
			totalLoadingTime = totalLoadingTime + tuple.getShipment().getDeliveryServiceTime();
			if(tuple.getTime() > latestTupleTime){
				latestTupleTime = tuple.getTime();
			}
			tourBuilder.addLeg(new Leg());
			CarrierService carrierService = convertToCarrierService(tuple);
			tourBuilder.scheduleService(carrierService);
		}
		
		
		tourBuilder.addLeg(new Leg());
		tourBuilder.scheduleEnd(Id.create(adapter.getEndLinkId(), Link.class));
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
		double tourStartTime = latestTupleTime + totalLoadingTime;
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);
		tours.add(sTour);
		CarrierPlan plan = new CarrierPlan(carrier,tours);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		return plan;
	}


	private CarrierService convertToCarrierService( ShipmentWithTime tuple ){
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, adapter.getEndLinkId());
		builder.setCapacityDemand(tuple.getShipment().getSize() );
		builder.setServiceDuration(tuple.getShipment().getDeliveryServiceTime() );
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}
	 
	
	@Override protected void updateShipments() {
		for( ShipmentWithTime tuple : shipments) {
			updateSchedule(tuple);
		}
	}
	
	
	private void updateSchedule( ShipmentWithTime tuple ){
		//outerLoop:
		for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()){
			Tour tour = scheduledTour.getTour();
			for(TourElement element: tour.getTourElements()){
				if(element instanceof Tour.ServiceActivity){
					Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
					LSPCarrierPair carrierPair = new LSPCarrierPair(tuple, serviceActivity.getService());
					for(LSPCarrierPair pair : pairs){
						if(pair.tuple == carrierPair.tuple && pair.service.getId() == carrierPair.service.getId()){
							addShipmentLoadElement(tuple, tour, serviceActivity);
							addShipmentTransportElement(tuple, tour, serviceActivity);
							addShipmentUnloadElement(tuple, tour, serviceActivity);
							addMainRunStartEventHandler(pair.service, tuple, adapter);
							addMainRunEndEventHandler(pair.service, tuple, adapter);
							//break outerLoop;	
						}						
					}
				}
			}		
		}
	}
	
	private void addShipmentLoadElement( ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity ){
		ShipmentUtils.ScheduledShipmentLoadBuilder builder = ShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
		builder.setResourceId(adapter.getId());
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
		double cumulatedLoadingTime = 0;
		for(TourElement element: tour.getTourElements()){
			if(element instanceof Tour.ServiceActivity){
				Tour.ServiceActivity activity = (Tour.ServiceActivity) element;
				cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
			}
		}
		builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
		builder.setEndTime(startTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getStartLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement  load = builder.build();
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, load);
	}

	private void addShipmentTransportElement( ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity ){
		ShipmentUtils.ScheduledShipmentTransportBuilder builder = ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
		builder.setResourceId(adapter.getId());
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
		builder.setStartTime(startTimeOfTransport);
		builder.setEndTime(legAfterStart.getExpectedTransportTime() + startTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(tour.getStartLinkId());
		builder.setToLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement  transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, transport);
	}

	private void addShipmentUnloadElement( ShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity ){
		ShipmentUtils.ScheduledShipmentUnloadBuilder builder = ShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
		builder.setResourceId(adapter.getId());
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		double cumulatedLoadingTime = 0;
		for(TourElement element: tour.getTourElements()){
			if(element instanceof Tour.ServiceActivity){
				Tour.ServiceActivity activity = (Tour.ServiceActivity) element;
				cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
			}
		}
		int startIndex = tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
		Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
		builder.setStartTime(legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime());
		builder.setEndTime(legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime() + cumulatedLoadingTime);
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement  unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, unload);
	}
	
	private void addMainRunStartEventHandler( CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource ){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				MainRunTourStartEventHandler handler = new MainRunTourStartEventHandler(tuple.getShipment(), carrierService, element, resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}
	}

	private void addMainRunEndEventHandler( CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource ){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				MainRunTourEndEventHandler handler = new MainRunTourEndEventHandler(tuple.getShipment(), carrierService, element,resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}
		
	}
}
