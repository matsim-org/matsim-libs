package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class CarrierConsistencyCheckers {
	public enum capacityCheckResult {
		CAPACITY_SUFFICIENT, CAPACITY_INSUFFICIENT
	}
	public enum scheduleCheckResult {
		VEHICLE_AVAILABLE, NO_VEHICLE_AVAILABLE
	}

	public enum allJobsInTourCheckResult {
		ALL_JOBS_IN_TOURS, NOT_ALL_JOBS_IN_TOURS, JOBS_SCHEDULED_MULTIPLE_TIMES, JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED, JOBS_IN_TOUR_BUT_NOT_LISTED, ERROR
	}

	private static final Logger log = LogManager.getLogger(CarrierConsistencyCheckers.class);

	private static boolean doesShipmentFitInVehicle(Double capacity, Double demand) {
		return demand <= capacity;
	}

	private static boolean doTimeWindowsOverlap(TimeWindow tw1, TimeWindow tw2) {
		return tw1.getStart() <= tw2.getEnd() && tw2.getStart() <= tw1.getEnd();
	}

	private record VehicleInfo(TimeWindow operationWindow, double capacity) {
	}

	private record ShipmentPickupInfo(TimeWindow pickupWindow, double capacityDemand) {
	}

	private record ShipmentDeliveryInfo(TimeWindow deliveryWindow, double capacityDemand) {
	}

	private record ServiceInfo(TimeWindow serviceWindow, double capacityDemand) {
	}

	/**
	 * @author antonstock
	 * This method checks if every carrier is able to handle every given job (services + shipments) with the available fleet. This method does not check the vehicle's schedule but the capacity only.
	 * capacityCheck returns boolean isVehicleSufficient:
	 * = true: the highest vehicle capacity is greater or equal to the highest capacity demand
	 * = false: the highest vehicle capacity is less tan or equal to the highest capacity demand
	 */
	public static capacityCheckResult capacityCheck(Carriers carriers) {

		//this map stores all checked carrier's IDs along with the result. true = carrier can handle all jobs.
		Map<Id<Carrier>, Boolean> isCarrierCapable = new HashMap<>();

		//go through all carriers after one another
		for (Carrier carrier : carriers.getCarriers().values()) {
			//List with all vehicle capacities of the current carrier
			List<Double> vehicleCapacityList = new ArrayList<>();
			//List with all jobs with a higher capacity demand than the largest vehicle's capacity
			List<CarrierJob> jobTooBigForVehicle = new LinkedList<>();
			//iterates through all vehicles of the current carrier and determines the vehicle capacities
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}
			//determine the highest capacity
			final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

			//a carrier has only one job type: shipments OR services
			//checks if the largest vehicle is sufficient for all shipments
			//if not, shipment is added to the jobTooBigForVehicle-List.
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(shipment);
				}
			}
			//checks if the largest vehicle is sufficient for all services
			//if not, service is added to the jobTooBigForVehicle-List.
			for (CarrierService service : carrier.getServices().values()) {
				if (service.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(service);
				}
			}

			//if map is empty, there is a sufficient vehicle for every job
			if (jobTooBigForVehicle.isEmpty()) {
				log.info("Carrier '{}': At least one vehicle has sufficient capacity ({}) for all jobs.", carrier.getId().toString(), maxVehicleCapacity);
				isCarrierCapable.put(carrier.getId(), true);
			} else {
				//if map is not empty, at least one job's capacity demand is too high for the largest vehicle.
				isCarrierCapable.put(carrier.getId(), false);
				//TODO: Für Montag: Wieso kommen .info und .warn durcheinander?
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(), job.getCapacityDemand());
				}
			}
		}
		//if every carrier has at least one vehicle with sufficient capacity for all jobs, return CAPACITY_SUFFICIENT
		if (isCarrierCapable.values().stream().allMatch(v -> v)) {
			return capacityCheckResult.CAPACITY_SUFFICIENT;
		} else {
			return capacityCheckResult.CAPACITY_INSUFFICIENT;
		}
	}

	/**
	 * this method will check if all existing carriers have vehicles with enough capacity in operation to handle all given jobs.
	 */
	//@KMT: ich habe hier einiges umgebaut, beim Erstellen der verschiedenen Tests ist mir ein Logikfehler aufgefallen.
	//@Anton: Muss ich mal in Ruhe ansehen -> eher Montag vormittag ;)
	//KMT: Jetzt macht der Code für meine Begriffe was er soll, bin gespannt, was du so findest ;-)
	public static boolean vehicleScheduleTest(Carriers carriers) {
		//isCarrierCapable saves carrierIDs and check result (true/false)
		Map<Id<Carrier>, Boolean> isCarrierCapable = new HashMap<>();
		//go through all carriers
		for (Carrier carrier : carriers.getCarriers().values()) {
			//vehicleOperationWindows saves vehicle's ID along with its operation hours
			Map<Id<Vehicle>, VehicleInfo> vehicleOperationWindows = new HashMap<>();
			//these three maps save the job's ID along with its time window (pickup, delivery, service)
			Map<Id<? extends CarrierJob>, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();
			Map<Id<? extends CarrierJob>, ServiceInfo> serviceWindows = new HashMap<>();
			//feasibleJob saves job ID and vehicle ID of all feasible Jobs
			Map<Id<? extends CarrierJob>, List<Id<Vehicle>>> feasibleJob = new HashMap<>();
			//nonFeasibleJob saves Job ID and reason why a job can not be handled by a carrier -> not enough capacity at all (=job is too big for all existing vehicles) OR no sufficient vehicle in operation
			Map<Id<? extends CarrierJob>, String> nonFeasibleJob = new HashMap<>();
			//go through all vehicles of the current carrier and determine vehicle ID, operation time window & capacity
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//read vehicle ID
				Id<Vehicle> vehicleID = carrierVehicle.getId();
				//get the start and end times of vehicle
				var vehicleOperationStart = carrierVehicle.getEarliestStartTime();
				var vehicleOperationEnd = carrierVehicle.getLatestEndTime();
				//get vehicle capacity
				var vehicleCapacity = carrierVehicle.getType().getCapacity().getOther();
				//create TimeWindow with start and end of operation
				TimeWindow operationWindow = TimeWindow.newInstance(vehicleOperationStart, vehicleOperationEnd);
				//write in Map: vehicle ID and Map VehicleInfo (time window of operation & capacity)
				vehicleOperationWindows.put(vehicleID, new VehicleInfo(operationWindow, vehicleCapacity));
			}
			/**
			 * SHIPMENTS PART - Pickup
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
			/**
			 * SHIPMENTS PART - Delivery
			 */
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
			if (nonFeasibleJob.isEmpty()) {
				isCarrierCapable.put(carrier.getId(), true);
			} else {
				isCarrierCapable.put(carrier.getId(), false);
			}
			//TODO: Debug only, kann weg wenn alles läuft.
			log.warn(nonFeasibleJob.toString());
		}
		//if every carrier has at least one vehicle in operation with sufficient capacity for all jobs, allCarriersCapable will be true
		//TODO: hier könnte man statt eines boolean auch ein anderes/besser geeignetes return nutzen
		//theoretisch könnte man den Ausdruck kürzen (s. IntelliJ Vorschlag), ich lasse es aber erstmal so, bis entschieden ist, was vehicleScheduleTest zurückgeben soll.
		boolean allCarriersCapable = isCarrierCapable.values().stream().allMatch(v -> v);
		isCarrierCapable.forEach((key, value) -> {
			if (value) {
				log.info("Carrier " + key + " can handle all jobs.");
			} else {
				log.warn("Carrier " + key + " can not handle all jobs.");
			}
		});
		return allCarriersCapable;
	}

	/**
	 * This method will check whether all jobs have been correctly assigned to a tour, i.e. each job only occurs once (if the job is a shipment, pickup and delivery are two different jobs).
	 */
	public static allJobsInTourCheckResult allJobsInTours(Carriers carriers) {
		Map<Id<Carrier>, String> isCarrierCapable = new HashMap<>();
		boolean jobInToursMoreThanOnce = false;
		boolean jobIsMissing = false;
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Id<? extends CarrierJob>> serviceInTour = new LinkedList<>();
			List<String> shipmentInTour = new LinkedList<>();

			List<Id<? extends CarrierJob>> serviceList = new LinkedList<>();
			List<String> shipmentList = new LinkedList<>();

			Map<Id<? extends CarrierJob>, Integer> serviceCount = new HashMap<>();
			Map<String, Integer> shipmentCount = new HashMap<>();
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					//carrier only has one job-type: services or shipments
					//service is saved as an Id
					if (tourElement instanceof Tour.ServiceActivity serviceActivity) {
						serviceInTour.add(serviceActivity.getService().getId());
					}
					//shipment is saved as a string: jobId + activity type
					if (tourElement instanceof Tour.ShipmentBasedActivity shipmentBasedActivity) {
						shipmentInTour.add(shipmentBasedActivity.getShipment().getId()+" | "+shipmentBasedActivity.getActivityType());
					}
				}
			}

			//save all jobs the current carrier should do
			//shipments have to be picked up and delivered. To allow shipmentInTour being properly matched to shipmentList, shipments are saved with suffix " | pickup" or " | delivery"
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentList.add(shipment.getId()+" | pickup");
				shipmentList.add(shipment.getId()+" | delivery");
			}
			//services are saved with id only
			for (CarrierService service : carrier.getServices().values()) {
				serviceList.add(service.getId());
			}
			//count appearance of job ids
			for (Id<? extends CarrierJob> serviceId : serviceInTour) {
				serviceCount.put(serviceId, serviceCount.getOrDefault(serviceId, 0) + 1);
			}

			Iterator<Id<? extends CarrierJob>> serviceIterator = serviceList.iterator();
			while (serviceIterator.hasNext()) {
				Id<? extends CarrierJob> serviceId = serviceIterator.next();
				int count = serviceCount.getOrDefault(serviceId,0);
					if (count == 1) {
						log.info("Carrier: {} | Job {} is scheduled once.", carrier.getId(), serviceId);
						serviceIterator.remove();
					} else if (count > 1){
						log.warn("Carrier: {} | Job {} is scheduled {} times!", carrier.getId(), serviceId, count);
						jobInToursMoreThanOnce = true;
					} else {
						log.warn("Carrier: {} | Job {} is not part of a tour!", carrier.getId(), serviceId);
						jobIsMissing = true;
					}
			}
			//count appearance of job ids
			for (String shipmentId : shipmentInTour) {
				System.out.println(shipmentId);
				shipmentCount.put(shipmentId, shipmentCount.getOrDefault(shipmentId, 0) + 1);
			}
			Iterator<String> shipmentIterator = shipmentList.iterator();
			while (shipmentIterator.hasNext()) {
				String shipmentId = shipmentIterator.next();
				int count = shipmentCount.getOrDefault(shipmentId,0);
				if (count == 1) {
					log.info("Carrier: {} | Job {} is scheduled once.", carrier.getId(), shipmentId);
					shipmentIterator.remove();
				} else if (count > 1){
					log.warn("Carrier: {} | Job {} is scheduled {} times!", carrier.getId(), shipmentId, count);
					jobInToursMoreThanOnce = true;
				} else {
					log.warn("Carrier: {} | Job {} is not part of a tour!", carrier.getId(), shipmentId);
					jobIsMissing = true;
				}
			}
			//if serviceList or shipmentList is NOT empty, at least one job is scheduled multiple times or not at all.
			if(!serviceList.isEmpty()||!shipmentList.isEmpty()) {
				if (jobInToursMoreThanOnce && !jobIsMissing) {
					isCarrierCapable.put(carrier.getId(), "SCHEDULED_MORE_THAN_ONCE");
				} else if (!jobInToursMoreThanOnce && jobIsMissing)  {
					isCarrierCapable.put(carrier.getId(), "NOT_SCHEDULED");
				} else if (jobInToursMoreThanOnce && jobIsMissing)  {
					isCarrierCapable.put(carrier.getId(), "BOTH");
				}
			//if serviceList or shipmentList is empty, all existing jobs (services or shipments) are scheduled only once.
			} else {
				isCarrierCapable.put(carrier.getId(), "SCHEDULED_ONCE");
			}
		}
		//determine which return value is apprpriate, based on the value(s) of isCarrierCapable. Only Return SCHEDULED_ONCE, if all values are SCHEDULED_ONCE.
		//@KMT: Es kann natürlich nur einen Rückgabewert geben, wäre es theoretisch nötig, Kombinationen zurückgeben zu können?
		//-> zwei Carrier haben nicht SCHEDULED_ONCE sondern zwei verschiedene andere, dann wird aktuell nur einer der beiden returned...
		if (isCarrierCapable.values().stream().allMatch(v -> v.equals("SCHEDULED_ONCE"))) {
			return allJobsInTourCheckResult.ALL_JOBS_IN_TOURS;
		} else if (isCarrierCapable.values().stream().anyMatch(v -> v.equals("NOT_SCHEDULED"))) {
			return allJobsInTourCheckResult.NOT_ALL_JOBS_IN_TOURS;
		} else if (isCarrierCapable.values().stream().anyMatch(v -> v.equals("SCHEDULED_MORE_THAN_ONCE"))) {
			return allJobsInTourCheckResult.JOBS_SCHEDULED_MULTIPLE_TIMES;
		} else if (isCarrierCapable.values().stream().anyMatch(v -> v.equals("BOTH"))) {
			return allJobsInTourCheckResult.JOBS_MISSING_AND_OTHERS_MULTIPLE_TIMES_SCHEDULED;
		} else {
			log.warn("Unexpected outcome! Please check all input files.");
			return allJobsInTourCheckResult.ERROR;
		}
	}
}
