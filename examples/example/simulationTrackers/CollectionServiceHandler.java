package example.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.contrib.freight.carrier.CarrierService;

import lsp.events.ServiceStartEvent;
import lsp.events.ServiceStartEventHandler;
import lsp.events.ServiceEndEvent;
import lsp.events.ServiceEndEventHandler;


public class CollectionServiceHandler implements ServiceStartEventHandler, ServiceEndEventHandler{

	
	
	private class ServiceTuple {
		private CarrierService service;
		private double startTime;
		
		public ServiceTuple(CarrierService service, double startTime) {
			this.service = service;
			this.startTime = startTime;
		}

		public CarrierService getService() {
			return service;
		}

		public double getStartTime() {
			return startTime;
		}
		
	}

	private Collection<ServiceTuple> tuples;
	private double totalLoadingCosts;
	private int totalNumberOfShipments;
	private int totalWeightOfShipments;
	
	public  CollectionServiceHandler() {
		this.tuples = new ArrayList<ServiceTuple>();
	}
	
	@Override
	public void reset(int iteration) {
			tuples.clear();
			totalNumberOfShipments = 0;
			totalWeightOfShipments = 0;
	}

	@Override
	public void handleEvent(ServiceEndEvent event) {
		System.out.println("Service Ends");
		double loadingCosts = 0;
		for(ServiceTuple tuple : tuples) {
			if(tuple.getService() == event.getService()) {
				double serviceDuration = event.getTime() - tuple.getStartTime();
				loadingCosts = serviceDuration * event.getVehicle().getVehicleType().getVehicleCostInformation().perTimeUnit;
				totalLoadingCosts = totalLoadingCosts + loadingCosts;
				tuples.remove(tuple);
				break;
			}
		}
	}

	@Override
	public void handleEvent(ServiceStartEvent event) {
		totalNumberOfShipments++;
		totalWeightOfShipments = totalWeightOfShipments + event.getService().getCapacityDemand();
		tuples.add(new ServiceTuple(event.getService(), event.getTime()));
	}

	public double getTotalLoadingCosts() {
		return totalLoadingCosts;
	}

	public int getTotalNumberOfShipments() {
		return totalNumberOfShipments;
	}

	public int getTotalWeightOfShipments() {
		return totalWeightOfShipments;
	}
	
}
