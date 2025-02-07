package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

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
	 * This method checks if every carrier is able to handle every given job (services + shipments) with the available fleet. This method does not check the vehicle's schedule but the capacity only.
	 * capacityCheck returns boolean isVehicleSufficient:
	 * = true: the highest vehicle capacity is greater or equal to the highest capacity demand
	 * = false: the highest vehicle capacity is less tan or equal to the highest capacity demand
	 */
	public static boolean capacityCheck(Carriers carriers) {

		//Map, in der CarrierID und true (=Kapazität reicht)/false(=Kapazität reicht nicht) gespeichert wird. Sind alle values = true, return true. Min. 1 value = false, return false.
		Map<Id<Carrier>, Boolean> isCarrierCapable = new HashMap<>();
		//@Anton: Sollte das nicht auch besser in die for (carrier) Schleife? Sonst ist das ja wieder Carrier-Übergreifend...
		// Weiter nachgedacht: Es macht total Sinn, dass generell die Rückgabe über alle Carrier erfolgt. Aber dann muss die Logik sein, dass es True ist, wenn
		// für alle(!) Carrier die Bedingung erfüllt ist. Wenn nur ein Carrier nicht erfüllt ist, dann False. Oder?
		//@KMT: na klar, da habe ich gestern gar nicht dran gedacht... wie findet du diese Umsetzung? Map wird mit CarrierID + true/false bestückt,
		// am Ende wird die Map ausgewertet und ein gemeinsamer boolean (oder wofür wir uns am Ende entscheiden) zurückgegeben.
		// TODO -> Kommentare können weg, wenn es so passt.


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
				isCarrierCapable.put(carrier.getId(), true);
			} else {
				//if map is not empty, at least one job's capacity demand is too high for the largest vehicle.
				isCarrierCapable.put(carrier.getId(), false);
				//@KMT: Die Mischung aus .warn und .info bringt leider die Reihenfolge in der Ausgabe durcheinander (bei zwei Carriern und je 2 zu großen Jobs kommt 1x warn -> 1x info -> 1x warn -> 3x info
				//gibts dafür eine Lösung außer alles in .warn/.info zu wandeln?
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
			}
		}
		//if every carrier has at least one vehicle with sufficient capacity for all jobs, allCarriersCapable will be true
		//TODO: hier könnte man statt eines boolean auch ein anderes/besser geeignetes return nutzen
		//theoretisch könnte man den Ausdruck kürzen (s. IntelliJ Vorschlag), ich lasse es aber erstmal so, bis entschieden ist, was capacityCheck zurückgeben soll.
		boolean allCarriersCapable = isCarrierCapable.values().stream().allMatch(v->v);
		return allCarriersCapable;
	}
	/**
	* this method will check if all existing carriers have vehicles with enough capacity in operation to handle all given jobs.
	*/
	public static boolean vehicleScheduleTest(Carriers carriers) {
		//isCarrierCapable saves carrierIDs and check result (true/false)
		Map<Id<Carrier>, Boolean> isCarrierCapable = new HashMap<>();
		//nonFeasibleJob saves JobIDs and reason why a job can not be handled by a carrier -> not enough capacity at all (=job is too big for all existing vehicles) OR no sufficient vehicle in operation
		Map<Id<? extends CarrierJob>, String> nonFeasibleJob = new HashMap<>();
		//determine the operating hours of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
			Map<Id<Vehicle>, VehicleInfo> vehicleOperationWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ServiceInfo> serviceWindows = new HashMap<>();

			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//read vehicle ID
				Id<Vehicle> vehicleID = carrierVehicle.getId();
				//read earliest start and end of vehicle in seconds after midnight (21600 = 06:00:00 (am), 64800 = 18:00:00)
				var vehicleOperationStart = carrierVehicle.getEarliestStartTime();
				var vehicleOperationEnd = carrierVehicle.getLatestEndTime();
				var vehicleCapacity = carrierVehicle.getType().getCapacity().getOther();
				//create TimeWindow with start and end of operation
				TimeWindow operationWindow = TimeWindow.newInstance(vehicleOperationStart, vehicleOperationEnd);
				//write vehicle ID (key as Id) and time window of operation (value as TimeWindow) in HashMap
				vehicleOperationWindows.put(vehicleID, new VehicleInfo(operationWindow, vehicleCapacity));
			}
			/**
			* SHIPMENTS PART
			*/
			//collects information about all existing shipments: IDs, times for pickup, capacity demand
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentPickupWindows.put(shipment.getId(), new ShipmentPickupInfo(shipment.getPickupStartingTimeWindow(), shipment.getCapacityDemand()));
			}

			//collects information about all existing shipments: IDs, times for delivery, capacity demand
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentDeliveryWindows.put(shipment.getId(), new ShipmentDeliveryInfo(shipment.getDeliveryStartingTimeWindow(), shipment.getCapacityDemand()));
			}

			//checks if a vehicle with enough capacity is in operation to handle given shipments
			for (Id<? extends CarrierJob> shipmentID : shipmentPickupWindows.keySet()) {
				ShipmentPickupInfo pickupInfo = shipmentPickupWindows.get(shipmentID);
				ShipmentDeliveryInfo deliveryInfo = shipmentDeliveryWindows.get(shipmentID);
				boolean capacityFits = false;
				boolean pickupOverlap = false;
				boolean deliveryOverlap = false;

				for (Id<Vehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);

					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), pickupInfo.capacityDemand());

					pickupOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), pickupInfo.pickupWindow());

					deliveryOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), deliveryInfo.deliveryWindow());

					//if vehicle has enough capacity and is in operation during pickup + delivery times, the carrier is capable of handling the shipment
					//@KMT: Aktuell muss EIN Fahrzeug pickup & delivery machen können, ist es theoretisch möglich, dass Fzg A einsammelt und Fzg B ausliefert? Dann müsste ich für die shipments nochmal was an der Logik ändern
					//-> die services betrifft das ja nicht, weil es nur ein Zeitfenster gibt.
					if (capacityFits && pickupOverlap && deliveryOverlap) {
						isCarrierCapable.put(carrier.getId(), true);
					} else {
						//TODO: Logikproblem: Es wird nicht beachtet, ob der Job von einem anderen Fahrzeug erledigt werden kann. Wenn eine der
						// Voraussetzungen nicht erfüllt wird, kommt es in nonFeasibleJob, obwohl es vllt von einem anderen Fzg befördert werden kann
						// Test Ergebnis ist korrekt, weil isCarrierCapable wie vorgesehen funktioniert, aber Fehlermeldungen sind falsch.
						isCarrierCapable.put(carrier.getId(), false);
						if (!capacityFits) {
							nonFeasibleJob.put(shipmentID, "Vehicle(s) in operation is too small.");
						} else if (!pickupOverlap) {
							nonFeasibleJob.put(shipmentID, "No sufficient vehicle in operation");
						} else if (!deliveryOverlap) {
							nonFeasibleJob.put(shipmentID, "No sufficient vehicle in operation");
						} else {
							throw new RuntimeException("Unexpected outcome: vehicleScheduleTest, Line 160.");
						}
					}
				}
			}

			/**
			 * SERVICES PART
			 */
			//collects information about all existing services: IDs, time window, capacity demand
			for (CarrierService service : carrier.getServices().values()) {
				serviceWindows.put(service.getId(), new ServiceInfo(service.getServiceStaringTimeWindow(), service.getCapacityDemand()));
			}

			//checks if a vehicle with enough capacity is in operation to handle given services
			for (Id<? extends CarrierJob> serviceId : serviceWindows.keySet()) {
				ServiceInfo serviceInfo = serviceWindows.get(serviceId);
				boolean capacityFits = false;
				boolean serviceWindow = false;

				for (Id<Vehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);

					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), serviceInfo.capacityDemand());

					serviceWindow = doTimeWindowsOverlap(vehicleInfo.operationWindow(), serviceInfo.serviceWindow());

					//if vehicle has enough capacity and is in operation during service time, the carrier is capable of handling the shipment
					if (capacityFits && serviceWindow) {
						isCarrierCapable.put(carrier.getId(), true);
					} else {
						//TODO: Logikproblem: Es wird nicht beachtet, ob der Job von einem anderen Fahrzeug erledigt werden kann. Wenn eine der
						// Voraussetzungen nicht erfüllt wird, kommt es in nonFeasibleJob, obwohl es vllt von einem anderen Fzg befördert werden kann
						// Test Ergebnis ist korrekt, weil isCarrierCapable wie vorgesehen funktioniert, aber Fehlermeldungen sind falsch.
						isCarrierCapable.put(carrier.getId(), false);
						if (!capacityFits) {
							log.warn(serviceId + ": Vehicle(s) in operation is too small. " + vehicleID);
							nonFeasibleJob.put(serviceId, "Vehicle(s) in operation is too small.");
						} else if (!serviceWindow) {
							log.warn(serviceId + ": No sufficient vehicle in operation "+vehicleID);
							nonFeasibleJob.put(serviceId, "No sufficient vehicle in operation");
						} else {
							throw new RuntimeException("Unexpected outcome: vehicleScheduleTest, Line 160.");
						}
					}
				}
			}
		}
		//if every carrier has at least one vehicle in operation with sufficient capacity for all jobs, allCarriersCapable will be true
		//TODO: hier könnte man statt eines boolean auch ein anderes/besser geeignetes return nutzen
		//theoretisch könnte man den Ausdruck kürzen (s. IntelliJ Vorschlag), ich lasse es aber erstmal so, bis entschieden ist, was vehicleScheduleTest zurückgeben soll.
		boolean allCarriersCapable = isCarrierCapable.values().stream().allMatch(v->v);
		return allCarriersCapable;
	}

	public static void allJobsInTours(Carriers carriers) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Id<? extends CarrierJob>> liste = new LinkedList<>();
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.ServiceActivity serviceActivity){
						serviceActivity.getService().getId(); //TODO abspeichern in Liste...
					}
					if (tourElement instanceof Tour.ShipmentBasedActivity shipmentBasedActivity){
						shipmentBasedActivity.getShipment().getId(); //Todo abspeichern
					}
				}
			}
			//todo Vergleich
		}
	}

}
