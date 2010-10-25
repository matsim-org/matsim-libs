package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TourBuilder;
import playground.mzilske.freight.Tour.TourElement;

public class TrivialReplanner {

	private static Logger logger = Logger.getLogger(TrivialReplanner.class);
	
	public CarrierPlan replan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts, CarrierPlan selectedPlan) {
		logger.debug("We have " + selectedPlan.getScheduledTours().size() + " scheduled tours.");
		ScheduledTour sourceTour = pickOne(selectedPlan.getScheduledTours());
		ScheduledTour targetTour = pickOne(selectedPlan.getScheduledTours());

		if (sourceTour == targetTour) {
			logger.debug("Picked the same tour for source and target.");
			return selectedPlan;
		}


		List<Shipment> shipments = sourceTour.getTour().getShipments();
		if (shipments.isEmpty()) {
			logger.debug("No shipments in source tour.");
			return selectedPlan;
		}

		Shipment shipment = pickOne(shipments);

		logger.debug("Moving shipment " + shipment.toString() + " from " + sourceTour.toString() + " to " + targetTour.toString());
		
		List<ScheduledTour> newTours = new ArrayList<ScheduledTour>();

		for (ScheduledTour scheduledTour : selectedPlan.getScheduledTours()) {
			ScheduledTour newTour;
			if (scheduledTour == sourceTour) {
				newTour = buildNewSourceTour(sourceTour, shipment); 
			} else if (scheduledTour == targetTour) {
				newTour = buildNewTargetTour(targetTour, shipment); 
			} else {
				newTour = sourceTour;
			}
			newTours.add(newTour);
		}

		CarrierPlan newPlan = new CarrierPlan(newTours);
		return newPlan;
	}

	private ScheduledTour buildNewSourceTour(ScheduledTour sourceTour,
			Shipment shipment) {
		ScheduledTour newTour;
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.scheduleStart(sourceTour.getTour().getStartLinkId());
		for (TourElement tourElement : sourceTour.getTour().getTourElements()) {
			if (tourElement.getShipment() != shipment) {
				tourBuilder.schedule(tourElement);
			}
		}
		tourBuilder.scheduleEnd(sourceTour.getTour().getEndLinkId());
		newTour = new ScheduledTour(tourBuilder.build(), sourceTour.getVehicle(), sourceTour.getDeparture());
		return newTour;
	}

	private ScheduledTour buildNewTargetTour(ScheduledTour targetTour, Shipment shipment) {
		ScheduledTour newTour;
		TourBuilder tourBuilder = new TourBuilder();
		tourBuilder.scheduleStart(targetTour.getTour().getStartLinkId());
		tourBuilder.schedulePickup(shipment);
		tourBuilder.scheduleDelivery(shipment);
		for (TourElement tourElement : targetTour.getTour().getTourElements()) {
			tourBuilder.schedule(tourElement);
		}
		tourBuilder.scheduleEnd(targetTour.getTour().getEndLinkId());
		newTour = new ScheduledTour(tourBuilder.build(), targetTour.getVehicle(), targetTour.getDeparture());
		return newTour;
	}

	private static <T> T pickOne(Collection<T> scheduledTours) {
		List<T> list = new ArrayList<T>(scheduledTours);
		Collections.shuffle(list);
		return list.get(0);
	}

}
