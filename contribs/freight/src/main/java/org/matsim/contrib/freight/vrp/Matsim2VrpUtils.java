package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.core.basic.v01.IdImpl;

class Matsim2VrpUtils {
	
	static Collection<ScheduledTour> createTours(VehicleRoutingProblemSolution vrpSolution, Matsim2VrpMap matsim2vrpMap){
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(VehicleRoute route : vrpSolution.getRoutes()){
			TourImpl tour = route.getTour();
			TourBuilder tourBuilder = new TourBuilder();
			for(TourActivity act : tour.getActivities()){
				if(act instanceof Pickup){
					Shipment shipment = (Shipment)((Pickup)act).getJob();
					CarrierShipment carrierShipment = matsim2vrpMap.getCarrierShipment(shipment);
					tourBuilder.addLeg(new Leg());
					tourBuilder.schedulePickup(carrierShipment);
				}
				else if(act instanceof Delivery){
					Shipment shipment = (Shipment)((Delivery)act).getJob();
					CarrierShipment carrierShipment = matsim2vrpMap.getCarrierShipment(shipment);
					tourBuilder.addLeg(new Leg());
					tourBuilder.scheduleDelivery(carrierShipment);
				}
				else if(act instanceof Start){
					tourBuilder.scheduleStart(makeId(act.getLocationId()), act.getEarliestOperationStartTime(), act.getLatestOperationStartTime());
				}
				else if(act instanceof End){
					tourBuilder.addLeg(new Leg());
					tourBuilder.scheduleEnd(makeId(act.getLocationId()));
				}
				else {
					throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
				}
			}
			org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
			ScheduledTour scheduledTour = new ScheduledTour(vehicleTour, matsim2vrpMap.getCarrierVehicle(route.getVehicle()), vehicleTour.getEarliestDeparture());
			scheduledTours.add(scheduledTour);
		}
		return scheduledTours;
	}
	
	private static Id makeId(String id) {
		return new IdImpl(id);
	}

}
