package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;

public class CarrierFactory {
	
	public Carrier createCarrier(Id carrierId, Id linkId){
		return new CarrierImpl(carrierId, linkId);
	}
	
	public CarrierVehicle createVehicle(Id vehicleId, Id linkId){
		return new CarrierVehicle(vehicleId, linkId);
	}
	
	public CarrierShipment createShipment(Id from, Id to, int size, TimeWindow pickupTW, TimeWindow deliveryTW){
		return new CarrierShipment(from, to, size, pickupTW, deliveryTW);
	}
	
	public TimeWindow createTimeWindow(double start, double end){
		return new TimeWindow(start,end);
	}
	
	public CarrierCapabilities createCapabilities(){
		return new CarrierCapabilities();
	}
	
	public CarrierContract createContract(CarrierShipment shipment){
		return new CarrierContract(shipment, null);
	}
	
	public ScheduledTour createScheduledTour(Tour tour, CarrierVehicle vehicle, double departureTime){
		return new ScheduledTour(tour, vehicle, departureTime);
	}
	
	public TourBuilder createTourBuilder(){
		return new TourBuilder();
	}

	public Carriers createCarriers() {
		return new Carriers();
	}

	public CarrierPlan createPlan(List<ScheduledTour> sTours) {
		return new CarrierPlan(sTours);
	}

	public void createVehicle(Id vehicleId, Id linkId, int capacity, double earliestDep, double latestArr) {
		CarrierVehicle veh = createVehicle(vehicleId, linkId);
		veh.setCapacity(capacity);
		veh.setEarliestStartTime(earliestDep);
		veh.setLatestEndTime(latestArr);
	}

	public Collection<CarrierShipment> getShipments(Collection<CarrierContract> contracts) {
		Collection<CarrierShipment> shipments = new ArrayList<CarrierShipment>();
		for(Contract c : contracts){
			shipments.add((CarrierShipment)c.getShipment());
		}
		return shipments;
	}

	public Collection<CarrierVehicle> getVehicles(CarrierCapabilities carrierCapabilities) {
		return new ArrayList<CarrierVehicle>(carrierCapabilities.getCarrierVehicles());
	}

	public Tour copyTour(Tour tour) {
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.scheduleStart(tour.getStartLinkId(), tour.getEarliestDeparture(), tour.getLatestDeparture());
		for(TourElement e : tour.getTourElements()){
			if(e instanceof Pickup){
				tourBuilder.schedulePickup(((Pickup) e).getShipment());
			}
			else if(e instanceof Delivery){
				tourBuilder.scheduleDelivery(((Delivery) e).getShipment());
			}
			if(e instanceof Leg){
				Leg leg = tourBuilder.createLeg();
				leg.setDepartureTime(((Leg) e).getDepartureTime());
				leg.setExpectedTransportTime(((Leg) e).getExpectedTransportTime());
				NetworkRoute oldRoute = (NetworkRoute) ((Leg) e).getRoute();
				NetworkRoute route = tourBuilder.createRoute(oldRoute.getStartLinkId(),oldRoute.getLinkIds(),oldRoute.getEndLinkId());
				leg.setRoute(route);
				tourBuilder.addLeg(leg);
			}
		}
		tourBuilder.scheduleEnd(tour.getEndLinkId());
		return tourBuilder.build();
	}
	
	

}
