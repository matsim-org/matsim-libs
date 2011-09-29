package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.TourBuilder;

public class TourSchedulerImpl implements TourScheduler{
	
	private Carrier carrier;
	
	private int vehicleIdCounter = 0;
	
	public TourSchedulerImpl(Carrier carrier) {
		super();
		this.carrier = carrier;
	}

	public Collection<ScheduledTour> getScheduledTours(Collection<Tour> tours, Map<CarrierShipment, Collection<CarrierShipment>> aggregatedShipments) {
		reset();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(Tour tour : tours){
			boolean firstShipment = true;
			double startTime = 0.0;
			TourBuilder tourBuilder = new TourBuilder();
			Id start = tour.getStartLinkId();
			tourBuilder.scheduleStart(start);
			for(TourElement e : tour.getTourElements()){
				Shipment shipment = e.getShipment();
				if(firstShipment){
					startTime = shipment.getPickupTimeWindow().getStart();
					firstShipment = false;
				}
				Collection<CarrierShipment> relatedShipments = new ArrayList<CarrierShipment>();
				if(shipment != null){
					relatedShipments.addAll(aggregatedShipments.get(shipment));
				}
				if(e instanceof Pickup){
					for(CarrierShipment s : relatedShipments){
						tourBuilder.schedulePickup(s);
					}
				}
				if(e instanceof Delivery){
					for(CarrierShipment s : relatedShipments){
						tourBuilder.scheduleDelivery(s);
					}
				}
			}
			tourBuilder.scheduleEnd(tour.getEndLinkId());
			Tour newTour = tourBuilder.build();
			CarrierVehicle vehicle = getVehicle(start);
			carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
			scheduledTours.add(new ScheduledTour(newTour, vehicle, startTime));
		}
		return scheduledTours;
	}
	
	private CarrierVehicle getVehicle(Id start) {
		CarrierVehicle cV = null;
		for(CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles()){
			if(v.getLocation().equals(start)){
				cV = v;
			}
		}
		assertVehicleNotNull(cV);
		return new CarrierVehicle(getVehicleId(cV), cV.getLocation());
	}

	private void assertVehicleNotNull(CarrierVehicle cV) {
		if(cV==null){
			throw new IllegalStateException("no vehicle found");
		}
	}
	
	private Id getVehicleId(CarrierVehicle cV) {
		vehicleIdCounter++;
		return new IdImpl("veh_" + carrier.getId().toString() + "_" + vehicleIdCounter);
	}

	public void reset() {
		vehicleIdCounter = 0;
		
	}


}
