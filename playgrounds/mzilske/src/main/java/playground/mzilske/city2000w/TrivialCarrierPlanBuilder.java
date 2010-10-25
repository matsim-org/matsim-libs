package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;

public class TrivialCarrierPlanBuilder {

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		boolean first = true;
		for (CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles()) {
			Id vehicleLocation = carrierVehicle.getLocation();
			TourBuilder tourBuilder = new TourBuilder();
			tourBuilder.scheduleStart(vehicleLocation);
			if (first) {
				for (Contract contract : contracts) {
					for (Shipment shipment : contract.getShipments()) {
						tourBuilder.schedulePickup(shipment);
						tourBuilder.scheduleDelivery(shipment);
					}
				}
				tourBuilder.scheduleEnd(vehicleLocation);
				first = false;
			} 
			tourBuilder.scheduleEnd(vehicleLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		return carrierPlan;
	}

}
