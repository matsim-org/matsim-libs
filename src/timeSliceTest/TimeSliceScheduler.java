package timeSliceTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.ShipmentTuple;
import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.resources.ResourceScheduler;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.ScheduledShipmentLoad;
import lsp.shipment.ScheduledShipmentTransport;
import lsp.shipment.ScheduledShipmentUnload;
import lsp.usecase.CollectionServiceEventHandler;
import lsp.usecase.CollectionTourEndEventHandler;




public class TimeSliceScheduler extends ResourceScheduler{

	class LSPCarrierPair{
		private ShipmentTuple tuple;
		private CarrierService service;
		
		public LSPCarrierPair(ShipmentTuple tuple, CarrierService service){
			this.tuple = tuple;
			this.service = service;
		}	
	}
	
	private Carrier carrier;
	private HashMap<Id<LogisticsSolution>,Carrier> auxiliaryCarriers;
	private TimeSliceCarrierAdapter adapter;
	private ArrayList<LSPCarrierPair>pairs;
	
	
	@Override
	protected void initializeValues(Resource resource) {
		this.pairs = new ArrayList<LSPCarrierPair>();
		this.auxiliaryCarriers = new HashMap<>();
		if(resource.getClass() == TimeSliceCarrierAdapter.class){
			this.adapter = (TimeSliceCarrierAdapter) resource;
			this.carrier = adapter.getCarrier();
			this.carrier.getServices().clear();
			this.carrier.getShipments().clear();
			this.carrier.getPlans().clear();
			this.carrier.setSelectedPlan(null);
		}	
	}

	@Override
	protected void scheduleResource() {
		int i = 0;
		for(ShipmentTuple tupleToBeAssigned: shipments){
			CarrierService carrierService = convertToCarrierService(tupleToBeAssigned);
			if(auxiliaryCarriers.containsKey(tupleToBeAssigned.getShipment().getSolutionId())) {
				auxiliaryCarriers.get(tupleToBeAssigned.getShipment().getSolutionId()).getServices().add(carrierService);
			}
			else {
				Carrier auxiliaryCarrier = createAuxiliaryCarrier(Id.create("AuxiliaryCarrier_"+i, Carrier.class), tupleToBeAssigned);
				i++;
				auxiliaryCarrier.getServices().add(carrierService);
				auxiliaryCarriers.put(tupleToBeAssigned.getShipment().getSolutionId(), auxiliaryCarrier);
			}
		}		
	
		for(Carrier carrier : auxiliaryCarriers.values()) {
			routeCarrier(carrier);
		}
		
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
		for(Carrier carrier : auxiliaryCarriers.values()) {
			scheduledTours.addAll(carrier.getSelectedPlan().getScheduledTours());
		}
		
		CarrierPlan plan = new CarrierPlan(carrier,scheduledTours);
		carrier.setSelectedPlan(plan);
	}

	private CarrierService convertToCarrierService(ShipmentTuple tuple){
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, tuple.getShipment().getFromLinkId());
		builder.setServiceStartTimeWindow(tuple.getShipment().getStartTimeWindow());
		builder.setCapacityDemand(tuple.getShipment().getCapacityDemand());
		builder.setServiceDuration(tuple.getShipment().getServiceTime());
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}
	
	private Carrier createAuxiliaryCarrier(Id<Carrier> id, ShipmentTuple tupleToBeAssigned) {
		Carrier auxiliaryCarrier = CarrierImpl.newInstance(id);
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		
		for(CarrierVehicleType vehicleType : carrier.getCarrierCapabilities().getVehicleTypes()) {
			capabilitiesBuilder.addType(vehicleType);
		}
		
		CarrierVehicle latestAvailable = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next();
		double smallestDifference = Double.MAX_VALUE;
		
		for(CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles()) {	
			if(vehicle.getEarliestStartTime() < tupleToBeAssigned.getTime() && vehicle.getLatestEndTime() > tupleToBeAssigned.getTime()) {
				double timeDifference = tupleToBeAssigned.getTime() - vehicle.getEarliestStartTime();
				if(timeDifference < smallestDifference) {
					smallestDifference = timeDifference;
					latestAvailable = vehicle;
				}
			}
		}
	
		capabilitiesBuilder.addVehicle(latestAvailable);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		auxiliaryCarrier.setCarrierCapabilities(capabilitiesBuilder.build());
		return auxiliaryCarrier;
	}
	
	private void routeCarrier(Carrier carrier){
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, adapter.getNetwork());
	    NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(adapter.getNetwork(), carrier.getCarrierCapabilities().getVehicleTypes());
	    NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
	    vrpBuilder.setRoutingCost(netbasedTransportcosts);
	    VehicleRoutingProblem vrp = vrpBuilder.build();

	    VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
        
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
       
        VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
        
        CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
        NetworkRouter.routePlan(plan, netbasedTransportcosts);
        carrier.setSelectedPlan(plan);
	}
	
	
	
	@Override
	protected void updateShipments() {
		for(ShipmentTuple tuple : shipments) {
			updateShipment(tuple);
		}	
	}

	private void updateShipment(ShipmentTuple tuple){
		
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
							addCollectionTourEndEventHandler(pair.service, tuple, adapter);
							addCollectionServiceEventHandler(pair.service, tuple, adapter);
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
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex-1);
		double startTimeOfLoading = legBeforeService.getExpectedDepartureTime() + legBeforeService.getExpectedTransportTime();
		builder.setStartTime(startTimeOfLoading);
		builder.setEndTime(startTimeOfLoading + tuple.getShipment().getServiceTime());
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(serviceActivity.getLocation());
		builder.setCarrierService(serviceActivity.getService());
		ScheduledShipmentLoad  load = builder.build();
		String idString = load.getResourceId() + "" + load.getSolutionElement().getId() + "" + load.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, load);
	}
	
	private void addCollectionServiceEventHandler(CarrierService carrierService, ShipmentTuple tuple,  CarrierResource resource){	
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				CollectionServiceEventHandler endHandler = new CollectionServiceEventHandler(carrierService, tuple.getShipment(), element, resource);
				tuple.getShipment().getEventHandlers().add(endHandler);
				break;
			}
		}	
	}
	
	private void addCollectionTourEndEventHandler(CarrierService carrierService, ShipmentTuple tuple, CarrierResource resource){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				CollectionTourEndEventHandler handler = new CollectionTourEndEventHandler(carrierService, tuple.getShipment(), element, resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}	
	}
	
	private void addShipmentTransportElement(ShipmentTuple tuple, Tour tour, Tour.ServiceActivity serviceActivity){
		ScheduledShipmentTransport.Builder builder = ScheduledShipmentTransport.Builder.newInstance();
		builder.setResourceId(adapter.getId());
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				builder.setLogisticsSolutionElement(element);
			}
		}
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		Leg legAfterService = (Leg) tour.getTourElements().get(serviceIndex+1);
		double startTimeOfTransport = legAfterService.getExpectedDepartureTime();
		builder.setStartTime(startTimeOfTransport);
		Leg lastLeg = (Leg) tour.getTourElements().get(tour.getTourElements().size()-1);
		double endTimeOfTransport = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
		builder.setEndTime(endTimeOfTransport);
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(serviceActivity.getLocation());
		builder.setToLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ScheduledShipmentTransport transport = builder.build();
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
		Leg lastLeg = (Leg) tour.getTourElements().get(tour.getTourElements().size()-1);
		double startTime = lastLeg.getExpectedDepartureTime() + lastLeg.getExpectedTransportTime();
		builder.setStartTime(startTime);
		builder.setEndTime(startTime + getUnloadEndTime(tour));
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(tour.getEndLinkId());
		builder.setCarrierService(serviceActivity.getService());
		ScheduledShipmentUnload unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<AbstractShipmentPlanElement> id = Id.create(idString, AbstractShipmentPlanElement.class);
		tuple.getShipment().getSchedule().addPlanElement(id, unload);
	}
	
	private double getUnloadEndTime(Tour tour){
		double unloadEndTime = 0;
		for(TourElement element: tour.getTourElements()){
			if(element instanceof Tour.ServiceActivity){
				Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
				unloadEndTime = unloadEndTime + serviceActivity.getDuration();
			}
		}
		return unloadEndTime;
	}
	
}
