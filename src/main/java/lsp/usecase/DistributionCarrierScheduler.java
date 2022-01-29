package lsp.usecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import lsp.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentWithTime;
import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;
import lsp.resources.LSPResourceScheduler;
import org.matsim.vehicles.VehicleType;

/*package-private*/ class DistributionCarrierScheduler extends LSPResourceScheduler {

	static class LSPCarrierPair{
		private final ShipmentWithTime tuple;
		private final CarrierService service;
		
		public LSPCarrierPair( ShipmentWithTime tuple, CarrierService service ){
			this.tuple = tuple;
			this.service = service;
		}
		
	}
	
	
	private Carrier carrier;
	private DistributionCarrierAdapter adapter;
	private ArrayList<LSPCarrierPair>pairs;

	
	DistributionCarrierScheduler(){
		this.pairs = new ArrayList<>();
	}
	

	@Override protected void initializeValues( LSPResource resource ) {
		this.pairs = new ArrayList<>();
		if(resource.getClass() == DistributionCarrierAdapter.class){
			this.adapter = (DistributionCarrierAdapter) resource;
			this.carrier = adapter.getCarrier();
			this.carrier.getServices().clear();
			this.carrier.getShipments().clear();
			this.carrier.getPlans().clear();
			this.carrier.setSelectedPlan(null);
		}
	}
	
	@Override protected void scheduleResource() {
		int load = 0;
		double cumulatedLoadingTime = 0;
		double availiabilityTimeOfLastShipment = 0;
		ArrayList<ShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(shipments);
		ArrayList<ShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
		
		for( ShipmentWithTime tuple : copyOfAssignedShipments){
			VehicleType vehicleType = carrier.getCarrierCapabilities().getVehicleTypes().iterator().next();
			if((load + tuple.getShipment().getSize()) <= vehicleType.getCapacity().getOther().intValue() ){
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
				cumulatedLoadingTime = cumulatedLoadingTime + tuple.getShipment().getDeliveryServiceTime();
				availiabilityTimeOfLastShipment = tuple.getTime();
			}
			else{
				load=0;
				Carrier auxiliaryCarrier = createAuxiliaryCarrier(shipmentsInCurrentTour, availiabilityTimeOfLastShipment + cumulatedLoadingTime);
				routeCarrier(auxiliaryCarrier);
				scheduledTours.addAll(auxiliaryCarrier.getSelectedPlan().getScheduledTours());
				cumulatedLoadingTime = 0;
				shipmentsInCurrentTour.clear();
				shipmentsInCurrentTour.add(tuple);
				load = load + tuple.getShipment().getSize();
				cumulatedLoadingTime = cumulatedLoadingTime + tuple.getShipment().getDeliveryServiceTime();
				availiabilityTimeOfLastShipment = tuple.getTime();
			}
		}
		
		if(!shipmentsInCurrentTour.isEmpty()) {
			Carrier auxiliaryCarrier = createAuxiliaryCarrier(shipmentsInCurrentTour, availiabilityTimeOfLastShipment + cumulatedLoadingTime);
			routeCarrier(auxiliaryCarrier);
			scheduledTours.addAll(auxiliaryCarrier.getSelectedPlan().getScheduledTours());
			cumulatedLoadingTime = 0;
			shipmentsInCurrentTour.clear();
		}
		
		CarrierPlan plan = new CarrierPlan(carrier,scheduledTours);
		carrier.setSelectedPlan(plan);	
	}
		
	private CarrierService convertToCarrierService( ShipmentWithTime tuple ){
		Id<CarrierService> serviceId = Id.create(tuple.getShipment().getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, tuple.getShipment().getTo() );
		//builder.setServiceStartTimeWindow(tuple.getShipment().getEndTimeWindow());
		builder.setCapacityDemand(tuple.getShipment().getSize() );
		builder.setServiceDuration(tuple.getShipment().getDeliveryServiceTime() );
		CarrierService service = builder.build();
		pairs.add(new LSPCarrierPair(tuple, service));
		return service;
	}
	
	private void routeCarrier(Carrier carrier){
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, adapter.getNetwork());
	    NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(adapter.getNetwork(), carrier.getCarrierCapabilities().getVehicleTypes());
	    NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
	    vrpBuilder.setRoutingCost(netbasedTransportcosts);
	    VehicleRoutingProblem vrp = vrpBuilder.build();

	    VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
	    algorithm.setMaxIterations(1);
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
       
        VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);
        
        CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
        NetworkRouter.routePlan(plan, netbasedTransportcosts);
        carrier.setSelectedPlan(plan);
	}
	
	
	@Override protected void updateShipments() {
		for( ShipmentWithTime tuple: shipments) {
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
							addDistributionStartEventHandler(pair.service, tuple, adapter);
							addDistributionServiceEventHandler(pair.service, tuple, adapter);
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
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		Leg legBeforeService = (Leg) tour.getTourElements().get(serviceIndex-1);
		builder.setEndTime(legBeforeService.getExpectedTransportTime() + legBeforeService.getExpectedDepartureTime());
		builder.setCarrierId(carrier.getId());
		builder.setFromLinkId(tour.getStartLinkId());
		builder.setToLinkId(serviceActivity.getLocation());
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
		int serviceIndex = tour.getTourElements().indexOf(serviceActivity);
		ServiceActivity service = (ServiceActivity) tour.getTourElements().get(serviceIndex);
		builder.setStartTime(service.getExpectedArrival());
		builder.setEndTime(service.getDuration() + service.getExpectedArrival());
		builder.setCarrierId(carrier.getId());
		builder.setLinkId(serviceActivity.getLocation());
		builder.setCarrierService(serviceActivity.getService());
		ShipmentPlanElement  unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, unload);
	}
	
	
	private Carrier createAuxiliaryCarrier( ArrayList<ShipmentWithTime> shipmentsInCurrentTour, double startTime ){
		Carrier auxiliaryCarrier = CarrierUtils.createCarrier( carrier.getId() );
		CarrierVehicle carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(carrierVehicle.getId(), carrierVehicle.getLocation());
	    vBuilder.setEarliestStart(startTime);
	    vBuilder.setLatestEnd(24*60*60);
	    vBuilder.setType(carrier.getCarrierCapabilities().getVehicleTypes().iterator().next());
	    CarrierVehicle cv = vBuilder.build();
	    auxiliaryCarrier.getCarrierCapabilities().getVehicleTypes().add(carrier.getCarrierCapabilities().getVehicleTypes().iterator().next());
	    auxiliaryCarrier.getCarrierCapabilities().getCarrierVehicles().put(cv.getId(), cv);
	    auxiliaryCarrier.getCarrierCapabilities().setFleetSize(FleetSize.FINITE);
	    
	    for( ShipmentWithTime tuple :  shipmentsInCurrentTour){
			CarrierService carrierService = convertToCarrierService(tuple);
			auxiliaryCarrier.getServices().put(carrierService.getId(), carrierService);
	    }
	    return auxiliaryCarrier;
	}	
	
	private double getLoadStartTime( ShipmentWithTime tuple, Tour tour ){
		double loadStartTime = 0;
		ListIterator<TourElement> iterator = tour.getTourElements().listIterator(tour.getTourElements().size()-1);

		outerLoop:
		while(iterator.hasPrevious()){
			TourElement element  = iterator.previous();
			if(element instanceof Tour.ServiceActivity){
				Tour.ServiceActivity serviceActivity = (Tour.ServiceActivity) element;
				LSPCarrierPair carrierPair = new LSPCarrierPair(tuple, serviceActivity.getService());
				for(LSPCarrierPair pair : pairs){
					if(pair.tuple == carrierPair.tuple && pair.service.getId() == carrierPair.service.getId()){
						break outerLoop;	
					}						
					else{
						loadStartTime = loadStartTime + serviceActivity.getDuration();
					}	
				}
			}
		}
		
		return loadStartTime;
	}

	private void addDistributionServiceEventHandler( CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource ){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				DistributionServiceStartEventHandler handler = new DistributionServiceStartEventHandler(carrierService, tuple.getShipment(), element, resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}		
	}

	private void addDistributionStartEventHandler( CarrierService carrierService, ShipmentWithTime tuple, LSPCarrierResource resource ){
		for(LogisticsSolutionElement element : adapter.getClientElements()){
			if(element.getIncomingShipments().getShipments().contains(tuple)){
				DistributionTourStartEventHandler handler = new DistributionTourStartEventHandler(carrierService, tuple.getShipment(), element, resource);
				tuple.getShipment().getEventHandlers().add(handler);
				break;
			}
		}
		
	}

}
