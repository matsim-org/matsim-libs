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
				//@Anton: Das ist ja komisch und mir noch nie unter gekommen. Kannst das eventuell am Montag mit in der Runde vorstellen. Vlt hat da wer ne Idee..
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
			}
		}
		//if every carrier has at least one vehicle with sufficient capacity for all jobs, allCarriersCapable will be true
		//TODO: hier könnte man statt eines boolean auch ein anderes/besser geeignetes return nutzen
		//theoretisch könnte man den Ausdruck kürzen (s. IntelliJ Vorschlag), ich lasse es aber erstmal so, bis entschieden ist, was capacityCheck zurückgeben soll.
		// @Anton: Ja gerne, haben die Tendenz, dass man da mittlerweile Enum-Werte zurückgibt. Dann kann sich das sehr gut erweitern lassen.
		// Das Enum kann ja auch erstmal nur 2 Werte haben: ... z.B. {CAPACITY_SUFFICIENT, CAPACITY_INSUFFICIENT}.
		boolean allCarriersCapable = isCarrierCapable.values().stream().allMatch(v->v);
		return allCarriersCapable;
	}
	/**
	* this method will check if all existing carriers have vehicles with enough capacity in operation to handle all given jobs.
	*/
	//@KMT: ich habe hier einiges umgebaut, beim Erstellen der verschiedenen Tests ist mir ein Logikfehler aufgefallen.
	//@Anton: Muss ich mal in Ruhe ansehen -> eher Montag vormittag ;)
	public static boolean vehicleScheduleTest(Carriers carriers) {
		//isCarrierCapable saves carrierIDs and check result (true/false)
		Map<Id<Carrier>, Boolean> isCarrierCapable = new HashMap<>();
		//determine the operating hours of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
			//vehicleOperationWindows saves vehicle's ID along with its operation hours
			Map<Id<Vehicle>, VehicleInfo> vehicleOperationWindows = new HashMap<>();
			//these three maps save the job's ID along with its time window.
			Map<Id<? extends CarrierJob>, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ServiceInfo> serviceWindows = new HashMap<>();
			//feasibleJob saves jobID and vehicle ID of feasible Jobs
			Map<Id<? extends CarrierJob>, List<Id<Vehicle>>> feasibleJob = new HashMap<>();
			//nonFeasibleJob saves JobIDs and reason why a job can not be handled by a carrier -> not enough capacity at all (=job is too big for all existing vehicles) OR no sufficient vehicle in operation
			Map<Id<? extends CarrierJob>, String> nonFeasibleJob = new HashMap<>();

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
			/*
			* SHIPMENTS PART
			*/
			//collects information about all existing shipments: IDs, times for pickup, capacity demand
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentPickupWindows.put(shipment.getId(), new ShipmentPickupInfo(shipment.getPickupStartingTimeWindow(), shipment.getCapacityDemand()));
			}

			//run through all existing shipments
			for (Id<? extends CarrierJob> shipmentID : shipmentPickupWindows.keySet()) {
				//determine pickup time window
				ShipmentPickupInfo pickupInfo = shipmentPickupWindows.get(shipmentID);
				//possibleVehicles will save all sufficient vehicles (only needed for debug but should remain here)
				List<Id<Vehicle>> possibleVehicles = new ArrayList<>();
				//isTransportable will be true if the current shipment can be transported by at least one vehicle
				boolean isTransportable = false;
				boolean capacityFits = false;
				boolean pickupOverlap = false;

				//runs through all vehicles
				for (Id<Vehicle> vehicleID : vehicleOperationWindows.keySet()) {
					//determines operation hours of current vehicle
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);
					//determines if the capacity of the current vehicle is sufficient for the shipment's demand
					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), pickupInfo.capacityDemand());
					//determines if the operation hours overlap with shipment's time window
					pickupOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), pickupInfo.pickupWindow());

						//if the shipment fits in the current vehicle and the vehicle is in operation: shipment is transportable
						if (capacityFits && pickupOverlap) {
						isTransportable = true;
						possibleVehicles.add(vehicleID);
					}
				}
				//if shipment is transportable => job is feasible
				if (isTransportable) {
					feasibleJob.put(shipmentID, possibleVehicles);
				} else {
					nonFeasibleJob.put(shipmentID, "No sufficient vehicle for pickup");
				}
			}

			//see for-loop above. This loop does the same but for delivery instead of pickup
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentDeliveryWindows.put(shipment.getId(), new ShipmentDeliveryInfo(shipment.getDeliveryStartingTimeWindow(), shipment.getCapacityDemand()));
			}
			for (Id<? extends CarrierJob> shipmentID : shipmentDeliveryWindows.keySet()) {
				ShipmentDeliveryInfo deliveryInfo = shipmentDeliveryWindows.get(shipmentID);
				List<Id<Vehicle>> possibleVehicles = new ArrayList<>();
				boolean isTransportable = false;
				boolean capacityFits = false;
				boolean deliveryOverlap = false;

				for (Id<Vehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);
					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), deliveryInfo.capacityDemand());
					deliveryOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), deliveryInfo.deliveryWindow());

					if (capacityFits && deliveryOverlap) {
						isTransportable = true;
						possibleVehicles.add(vehicleID);
					}
				}
				if (isTransportable) {
					feasibleJob.put(shipmentID, possibleVehicles);
				} else {
					//if current shipment is already saved in nonFeasibleJob, the shipment can neither be picked up nor delivered
					if (nonFeasibleJob.containsKey(shipmentID)) {
						nonFeasibleJob.put(shipmentID, "No sufficient vehicle for pickup and delivery");
					} else {
						nonFeasibleJob.put(shipmentID, "No sufficient vehicle for delivery");
					}

				}
			}

			/**
			 * SERVICES PART
			 */
			//see for-loop above. This loop does the same but for services instead of shipments
			for (CarrierService service : carrier.getServices().values()) {
				serviceWindows.put(service.getId(), new ServiceInfo(service.getServiceStaringTimeWindow(), service.getCapacityDemand()));
			}

			for (Id<? extends CarrierJob> serviceID : serviceWindows.keySet()) {
				ServiceInfo serviceInfo = serviceWindows.get(serviceID);
				List<Id<Vehicle>> possibleVehicles = new ArrayList<>();
				boolean isTransportable = false;
				boolean capacityFits = false;
				boolean serviceOverlap = false;

				for (Id<Vehicle> vehicleID : vehicleOperationWindows.keySet()) {
					VehicleInfo vehicleInfo = vehicleOperationWindows.get(vehicleID);
					capacityFits = doesShipmentFitInVehicle(vehicleInfo.capacity(), serviceInfo.capacityDemand());
					serviceOverlap = doTimeWindowsOverlap(vehicleInfo.operationWindow(), serviceInfo.serviceWindow());

					if (capacityFits && serviceOverlap) {
						isTransportable = true;
						possibleVehicles.add(vehicleID);
					}
				}
				if (isTransportable) {
					feasibleJob.put(serviceID, possibleVehicles);
				} else {
					nonFeasibleJob.put(serviceID, "No sufficient vehicle for service");
				}
			}
			if(nonFeasibleJob.isEmpty()) {
				isCarrierCapable.put(carrier.getId(), true);
				log.info("Carrier " + carrier.getId() + " is able to handle all given jobs.");
			} else {
				isCarrierCapable.put(carrier.getId(), false);
				log.warn("Carrier " + carrier.getId() + " is NOT able to handle given jobs.");
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
