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

	private record ServiceInfo(TimeWindow serviceWindow, double capacityDemand){}
	/**
	 * @author antonstock
	 */
	public static boolean capacityCheck(Carriers carriers) {
		/**
		 * This method checks if every carrier is able to handle every given job (services + shipments) with the available fleet. This method does not check the vehicle's schedule but the capacity only.
		 * capacityCheck returns boolean isVehicleSufficient:
		 * = true: the highest vehicle capacity is greater or equal to the highest capacity demand
		 * = false: the highest vehicle capacity is less tan or equal to the highest capacity demand
		 */
		boolean isVehicleSufficient = false;

		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Double> vehicleCapacityList = new ArrayList<>();
			List<CarrierJob> jobTooBigForVehicle = new LinkedList<>();

			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}
			//determine the vehicle with the highest capacity
			final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

			//a carrier has only one job type: shipments OR services
			//checks if the largest vehicle is sufficient for all jobs (=shipment)
			//if not, job is added to the jobTooBigForVehicle-List.
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(shipment);
				}
			}
			//checks if the largest vehicle is sufficient for all jobs (=service)
			//if not, job is added to the jobTooBigForVehicle-List.
			for (CarrierService service : carrier.getServices().values()) {
				if (service.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(service);
				}
			}

			//if map is empty, there is a sufficient vehicle for every job
			if (jobTooBigForVehicle.isEmpty()) {
				log.info("Carrier '{}': At least one vehicle has sufficient capacity ({}) for all jobs.", carrier.getId().toString(), maxVehicleCapacity); //TODO: kann weg, wenn alles funktioniert.
				isVehicleSufficient = true;
			} else {
				//if map is not empty, at least one job's capacity demand is too high for the largest vehicle.
				isVehicleSufficient = false;
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
			}
		}
		return isVehicleSufficient;
	}

	public static void vehicleScheduleTest(Carriers carriers) {
		//TODO: Alle drei Maps umstellen von String auf Vehicle/Job id

		//determine the operating hours of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
			Map<Id<CarrierVehicle>, VehicleInfo> vehicleOperationWindows = new HashMap<>();

			Map<Id<CarrierShipment>, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();

			Map<Id<CarrierShipment>, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();

			Map<Id<CarrierService>, ServiceInfo> serviceWindows = new HashMap<>();

			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//read vehicle ID
				//TODO: @KMT: ich würde gerne die Id noch spezifizieren (zB Id<CarrierVehicle, das ist aber inkompatibel), aber ich kann mich nicht entscheiden,
				// was richtig ist, IntelliJ schlägt mir <VehicleType> vor...
				Id vehicleID = carrierVehicle.getType().getId();
				//read earliest start and end of vehicle in seconds after midnight (21600 = 06:00:00 (am), 64800 = 18:00:00
				var vehicleOperationStart = carrierVehicle.getEarliestStartTime();
				var vehicleOperationEnd = carrierVehicle.getLatestEndTime();
				var vehicleCapacity = carrierVehicle.getType().getCapacity().getOther();
				//create TimeWindow with start and end of operation
				TimeWindow operationWindow = TimeWindow.newInstance(vehicleOperationStart, vehicleOperationEnd);
				//write vehicle ID (key as Id) and time window of operation (value as TimeWindow) in HashMap
				vehicleOperationWindows.put(vehicleID, new VehicleInfo(operationWindow, vehicleCapacity));
			}
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				Id shipmentID = shipment.getId();
				TimeWindow shipmentPickupWindow = shipment.getPickupStartingTimeWindow();
				shipmentPickupWindows.put(shipmentID, new ShipmentPickupInfo(shipmentPickupWindow, shipment.getCapacityDemand()));
			}

			//determine delivery hours of shipments
			//Shipment ID (key as Id) and times (value as double) are being stored in HashMaps
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				Id shipmentID = shipment.getId();
				TimeWindow shipmentDeliveryWindow = shipment.getDeliveryStartingTimeWindow();
				shipmentDeliveryWindows.put(shipmentID, new ShipmentDeliveryInfo(shipmentDeliveryWindow, shipment.getCapacityDemand()));
			}

			//determine delivery hours of services
			//Service ID (key as Id) and times (value as double) are being stored in HashMaps
			for (CarrierService service : carrier.getServices().values()) {
				Id shipmentID = service.getId();
				TimeWindow serviceTimeWindow = service.getServiceStaringTimeWindow();
				shipmentDeliveryWindows.put(shipmentID, new ShipmentDeliveryInfo(serviceTimeWindow, service.getCapacityDemand()));
			}

			Map<Id<CarrierShipment>, String> nonFeasibleShipment = new HashMap<>();
			Map<Id<CarrierService>, String> nonFeasibleService = new HashMap<>();

			//a carrier has only one job type: shipments OR services
			//checks if a vehicle is in operation and has enough capacity to fit given shipment
			for (Id<CarrierShipment> shipmentID : shipmentPickupWindows.keySet()) {
				ShipmentPickupInfo pickupInfo = shipmentPickupWindows.get(shipmentID);
				ShipmentDeliveryInfo deliveryInfo = shipmentDeliveryWindows.get(shipmentID);
				boolean shipmentCanBeTransported = false;
				boolean capacityFits = false;
				boolean pickupOverlap = false;
				boolean deliveryOverlap = false;

				for (Id<CarrierVehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);

					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), pickupInfo.capacityDemand());

					pickupOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), pickupInfo.pickupWindow());

					deliveryOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), deliveryInfo.deliveryWindow());

					if (capacityFits && pickupOverlap && deliveryOverlap) {
						shipmentCanBeTransported = true;
					}
				}
				System.out.println(capacityFits + "" + pickupOverlap + "" + deliveryOverlap);

				if (!shipmentCanBeTransported) {
					if (!capacityFits) {
						nonFeasibleShipment.put(shipmentID, "Vehicle(s) in operation is too small.");
					} else if (!pickupOverlap) {
						nonFeasibleShipment.put(shipmentID, "No sufficient vehicle in operation");
					} else if (!deliveryOverlap) {
						nonFeasibleShipment.put(shipmentID, "No sufficient vehicle in operation");
					} else {
						throw new RuntimeException("Unexpected."); //TODO
					}
				}
			}
			//checks if a vehicle is in operation and has enough capacity to fit given service
			for (Id<CarrierService> serviceID : serviceWindows.keySet()) {
				ServiceInfo serviceInfo = serviceWindows.get(serviceID);

				boolean shipmentCanBeTransported = false;
				boolean capacityFits = false;
				boolean serviceOverlap = false;

				for (Id<CarrierVehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);

					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), serviceInfo.capacityDemand());

					serviceOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), serviceInfo.serviceWindow);

					if (capacityFits && serviceOverlap) {
						shipmentCanBeTransported = true;
					}
				}

				if (!shipmentCanBeTransported) {
					if (!capacityFits) {
						//TODO: @KMT: Kannst du mir erklären, wieso er hier meckert? In der capacityCheck-Methode funktioniert es in meinen Augen fast genauso...
						// wieso ist shipmentID vom Typ <CarrierShipment> und nicht <CarrierJob>?
						nonFeasibleService.put(serviceID, "Vehicle(s) in operation is too small.");
					} else if (!serviceOverlap) {
						nonFeasibleService.put(serviceID, "No sufficient vehicle in operation");
					} else {
						throw new RuntimeException("Unexpected outcome."); //TODO
					}
				}
			}
			if (!nonFeasibleShipment.isEmpty()||!nonFeasibleService.isEmpty()) {
				log.warn("A total of '{}' shipment(s) cannot be transported by carrier '{}'. Affected shipment(s): '{}' Reason(s): '{}'", nonFeasibleShipment.size(), carrier.getId().toString(), nonFeasibleShipment.keySet(), nonFeasibleShipment.values());
				log.warn("A total of '{}' shipment(s) cannot be transported by carrier '{}'. Affected shipment(s): '{}' Reason(s): '{}'", nonFeasibleService.size(), carrier.getId().toString(), nonFeasibleService.keySet(), nonFeasibleService.values());
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
