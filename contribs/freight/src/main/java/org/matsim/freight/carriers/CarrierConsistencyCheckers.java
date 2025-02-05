package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.*;

public class CarrierConsistencyCheckers {

	private static final Logger log = LogManager.getLogger(CarrierConsistencyCheckers.class);

	private static boolean doesShipmentFitInVehicle(Double capacity, Double demand) {
		return demand <= capacity;
	}
	private static boolean doTimeWindowsOverlap(TimeWindow tw1, TimeWindow tw2) {
		return tw1.getStart() <= tw2.getEnd() && tw2.getStart() <= tw1.getEnd();
	}
	private record VehicleInfo(TimeWindow operationWindow, double capacity){}

	private record ShipmentPickupInfo(TimeWindow pickupWindow, double capacityDemand) {}

	private record ShipmentDeliveryInfo(TimeWindow deliveryWindow, double capacityDemand){}
	/**
	 * @author antonstock
	 * This method checks if every carrier is able to handle every given shipment (TODO: services) with the available fleet. This method doesnot check the vehicle's schedule but the capacity only.
	 */
	public static void capacityCheck(Carriers carriers) {
		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Double> vehicleCapacityList = new ArrayList<>();
			List<CarrierJob> jobTooBigForVehicle = new LinkedList<>();

			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}

			final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

			//determine jobs with capacity demand > maxVehicleCapacity
			//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentsTooBigForVehicle'

			for (CarrierShipment shipment : carrier.getShipments().values()) {
				//TODO: @KMT: Muss ich hier statt CarrierShipment auch CarrierJob nutzen? ich finde nur .getServices und keine .getJobs o.ö.
				//@Anton: einfach ne andere Schleife mit carrier.getServices().values() machen.

				//@Anton: siehe unten: Musst du wirklich alle Sendungen speichern oder reicht es nicht, nur die zu speichern, die zu groß sind?
				//@KMT: war das deine Idee? ->done
				System.out.println(shipment.getCapacityDemand()); //kann dann spöter weg /alternativ in log.debug nutzen.
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(shipment);
				}
			}

			//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
			// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.
			//@KMT: Hier brauche ich bitte nochmal eine genauere/andere Erklärung. -> to be done
			// @Anton: Es gibt zwei mögliche Arten von Aufträgen für die Carrier: Services und Shipments. Dein Checker muss für beides funktionieren,
			// wobei immer jeder Carrier nur Services oder Shipments hat.
			// Das "nette" ist aber, dass beide das Interface "CarrierJob" implementieren. und du damit eine Liste von CarrierJobs machen kannst. (s. auch Kommentar weiter oben).


			//if map is empty, there is a sufficient vehicle for every job
			if (jobTooBigForVehicle.isEmpty()) {
				log.info("Carrier '{}': At least one vehicle has sufficient capacity ({}) for all jobs.", carrier.getId().toString(), maxVehicleCapacity);
				//Todo: @ Anton: Return True
				// Würde vremutlich auf log.debug gehen, damit er dann nicht die Konsole voll schreibt. Aber ist auch erstmal ok so :)
			} else {
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
				//if map is not empty, these jobs are too large for the existing fleet.
				//Todo: @ Anton: Return false
			}
		}
	}

	public static void vehicleScheduleTest(Carriers carriers) {
		//TODO: Alle drei Maps umstellen von String auf Vehicle/Job id

		//determine the operating hours of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
			Map<String, VehicleInfo> vehicleOperationWindows = new HashMap<>();

			Map<String, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();

			Map<String, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//read vehicle ID
				String vehicleID = carrierVehicle.getType().getId().toString();
				//read earliest start and end of vehicle in seconds after midnight (21600 = 06:00:00 (am), 64800 = 18:00:00
				var vehicleOperationStart = carrierVehicle.getEarliestStartTime();
				var vehicleOperationEnd = carrierVehicle.getLatestEndTime();
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				//create TimeWindow with start and end of operation
				TimeWindow operationWindow = TimeWindow.newInstance(vehicleOperationStart, vehicleOperationEnd);
				//write vehicle ID (key as string) and time window of operation (value as TimeWindow) in HashMap
				vehicleOperationWindows.put(vehicleID, new VehicleInfo(operationWindow, capacity));
			}
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				String shipmentID = shipment.getId().toString();
				TimeWindow shipmentPickupWindow = shipment.getPickupStartingTimeWindow();
				var shipmentSize = shipment.getCapacityDemand();
				shipmentPickupWindows.put(shipmentID, new ShipmentPickupInfo(shipmentPickupWindow, shipmentSize));
			}

			//determine delivery hours of shipments
			//Shipment ID (key as string) and times (value as double) are being stored in HashMaps
				for (CarrierShipment shipment : carrier.getShipments().values()) {
					String shipmentID = shipment.getId().toString();
					TimeWindow shipmentDeliveryWindow = shipment.getDeliveryStartingTimeWindow();
					var shipmentSize = shipment.getCapacityDemand();
					shipmentDeliveryWindows.put(shipmentID, new ShipmentDeliveryInfo(shipmentDeliveryWindow, shipmentSize));
				}

			Map<String, List<String>> validAssignments = new HashMap<>();
			Map<String, String> nonTransportableShipments = new HashMap<>();

			for (String shipmentID : shipmentPickupWindows.keySet()) {
				ShipmentPickupInfo pickupInfo = shipmentPickupWindows.get(shipmentID);
				ShipmentDeliveryInfo deliveryInfo = shipmentDeliveryWindows.get(shipmentID);
				boolean shipmentCanBeTransported = false;
				boolean capacityFits = false;
				boolean pickupOverlap = false;
				boolean deliveryOverlap = false;

				for (String vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);

					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), pickupInfo.capacityDemand());

					pickupOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), pickupInfo.pickupWindow());

					deliveryOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), deliveryInfo.deliveryWindow());

					if (capacityFits && pickupOverlap && deliveryOverlap) {
						shipmentCanBeTransported = true;
						validAssignments.computeIfAbsent(vehicleID, k -> new ArrayList<>()).add(shipmentID);
					}
				}
				System.out.println(capacityFits + "" + pickupOverlap + "" + deliveryOverlap);

				if (!shipmentCanBeTransported) {
					if (!capacityFits) {
						nonTransportableShipments.put(shipmentID, "Vehicle(s) in operation is too small.");
					} else if (!pickupOverlap) {
						nonTransportableShipments.put(shipmentID, "No sufficient vehicle in operation");
					} else if (!deliveryOverlap) {
						nonTransportableShipments.put(shipmentID, "No sufficient vehicle in operation");
					} else {
						throw new RuntimeException("Unexpected."); //TODO
					}
				}
			}
			if (!nonTransportableShipments.isEmpty()) {
				log.warn("A total of '{}' shipment(s) cannot be transported by carrier '{}'. Affected shipment(s): '{}' Reason(s): '{}'", nonTransportableShipments.size(), carrier.getId().toString(), nonTransportableShipments.keySet(), nonTransportableShipments.values());
			}
		}
	}
	public static void allJobsInTours(Carriers carriers) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Id<CarrierJob>> liste = new LinkedList<>();
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.ServiceActivity serviceActivity){
						serviceActivity.getService().getId(); //TODO abspeichern in Liste...
					}
					if (tourElement instanceof Tour.ShipmentBasedActivity shipmentBasedActivity){
						shipmentBasedActivity.getShipment().getId(); //Todo absperichern
					}
				}
			}
			//todo Vergleich
		}
	}

}
