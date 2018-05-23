package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentTuple;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.resources.ResourceScheduler;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.ScheduledShipmentLoad;
import lsp.shipment.ScheduledShipmentTransport;
import lsp.shipment.ScheduledShipmentUnload;
import lsp.shipment.ShipmentComparator;
import lsp.shipment.ScheduledShipmentLoad.Builder;


public class MainRunCarrierScheduler extends ResourceScheduler {

	class LSPCarrierPair{
		private ShipmentTuple tuple;
		private CarrierService service;
		
		public LSPCarrierPair(ShipmentTuple tuple, CarrierService service){
			this.tuple = tuple;
			this.service = service;
		}
		
	}
	
	private Carrier carrier;
	private MainRunCarrierAdapter adapter;
	private ArrayList<LSPCarrierPair>pairs;

	
	public MainRunCarrierScheduler(){
		this.pairs = new ArrayList<LSPCarrierPair>();
	}
	
	protected void initializeValues(Resource resource) {
		this.pairs = new ArrayList<LSPCarrierPair>();
		if(resource.getClass() == MainRunCarrierAdapter.class){
			this.adapter = (MainRunCarrierAdapter) resource;
			this.carrier = adapter.getCarrier();
			this.carrier.getServices().clear();
			this.carrier.getShipments().clear();
			this.carrier.getPlans().clear();
			this.carrier.setSelectedPlan(null);
		}
	}
	
	protected void scheduleResource() {
		int load = 0;
		ArrayList<ShipmentTuple> copyOfAssignedShipments = new ArrayList<ShipmentTuple>(shipments);
		Collections.sort(copyOfAssignedShipments, new ShipmentComparator());
		ArrayList<ShipmentTuple> shipmentsInCurrentTour = new ArrayList<ShipmentTuple>();
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();

		for(ShipmentTuple tuple : copyOfAssignedShipments){
			CarrierVehicleType vehicleType = carrier.getCarrierCapabilities().getVehicleTypes().iterator().next();
			if((load + tuple.getShipment().getCapacityDemand()) <= vehicleType.getCarrierVehicleCapacity()){
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getCapacityDemand();
			}
			else{
				load=0;
				CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
				scheduledTours.addAll(plan.getScheduledTours());
				shipmentsInCurrentTour.clear();
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getCapacityDemand();
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
	
	
	
	private CarrierPlan createPlan(Carrier carrier, ArrayList<ShipmentTuple> tuples){
		
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(adapter.getNetwork(), adapter.getCarrier().getCarrierCapabilities().getVehicleTypes());
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		Collection<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		
		Tour.Builder tourBuilder = Tour.Builder.newInstance();
		tourBuilder.scheduleStart(Id.create(adapter.getStartLinkId(), Link.class));

		double totalLoadingTime = 0;
		double latestTupleTime = 0;

		for (ShipmentTuple tuple : tuples){	
			totalLoadingTime = totalLoadingTime + tuple.getShipment().getServiceTime();
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
		CarrierVehicle vehicle = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		double tourStartTime = latestTupleTime + totalLoadingTime;
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);
		tours.add(sTour);
		CarrierPlan plan = new CarrierPlan(carrier,tours);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		return plan;
	}


	private CarrierService convertToCarrierService(ShipmentTuple tuple){
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, adapter.getEndLinkId());
		builder.setCapacityDemand(tuple.getShipment().getCapacityDemand());
		builder.setServiceDuration(tuple.getShipment().getServiceTime());
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}
	 
	
	protected void updateShipments() {	
		for(ShipmentTuple tuple : shipments) {
			updateSchedule(tuple);
		}
	}
	
	
	private void updateSchedule(ShipmentTuple tuple){
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
	
	private void addShipmentLoadElement(ShipmentTuple tuple, Tour tour, Tour.ServiceActivity serviceActivity){
		ScheduledShipmentLoad.Builder builder = ScheduledShipmentLoad.Builder.newInstance();
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
		ScheduledShipmentLoad  load = builder.build();
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, load);
	}

	private void addShipmentTransportElement(ShipmentTuple tuple, Tour tour, Tour.ServiceActivity serviceActivity){ 
		ScheduledShipmentTransport.Builder builder = ScheduledShipmentTransport.Builder.newInstance();
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
		ScheduledShipmentTransport  transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, transport);
	}

	private void addShipmentUnloadElement(ShipmentTuple tuple, Tour tour, Tour.ServiceActivity serviceActivity){
		ScheduledShipmentUnload.Builder builder = ScheduledShipmentUnload.Builder.newInstance();
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
		ScheduledShipmentUnload  unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, unload);
	}
	
	private void addMainRunStartEventHandler(CarrierService carrierService, ShipmentTuple tuple, CarrierResource resource){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				MainRunStartEventHandler handler = new MainRunStartEventHandler(tuple.getShipment(), carrierService, element, resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}
	}

	private void addMainRunEndEventHandler(CarrierService carrierService, ShipmentTuple tuple, CarrierResource resource){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				MainRunEndEventHandler handler = new MainRunEndEventHandler(tuple.getShipment(), carrierService, element,resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}
		
	}
}
