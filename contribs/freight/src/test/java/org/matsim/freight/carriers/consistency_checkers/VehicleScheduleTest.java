package org.matsim.freight.carriers.consistency_checkers;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.*;

import java.util.*;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

	/**
	 *
	 *  @author antonstock
	 *	This class will check if at least one of the given vehicles are in operation while the shipments have to be picked up or delivered.
	 *
	 */

public class VehicleScheduleTest {

	public static boolean doTimeWindowsOverlap(TimeWindow tw1, TimeWindow tw2) {
		//if function returns true: given time windows overlap
		//System.out.println("Do time windows overlap: " + tw1 + " and " + tw2);
		//System.out.println(tw1.getStart() <= tw2.getEnd() && tw2.getStart() <= tw1.getEnd());
		return tw1.getStart() <= tw2.getEnd() && tw2.getStart() <= tw1.getEnd();
	}

	public static boolean doesShipmentFitInVehicle(Double capacity, Double demand) {
		return demand <= capacity;
	}

	public static class VehicleInfo {
		private TimeWindow operationWindow;
		private double capacity;

		public VehicleInfo(TimeWindow operationWindow, double capacity) {
			this.operationWindow = operationWindow;
			this.capacity = capacity;
		}
		public TimeWindow getOperationWindow() {
			return operationWindow;
		}
		public double getCapacity() {
			return capacity;
		}
	}
		public static class ShipmentPickupInfo {
			private TimeWindow pickupWindow;
			private double capacityDemand;

			public ShipmentPickupInfo(TimeWindow operationWindow, double capacityDemand) {
				this.pickupWindow = operationWindow;
				this.capacityDemand = capacityDemand;
			}
			public TimeWindow getpickupWindow() {
				return pickupWindow;
			}
			public double getcapacityDemand() {
				return capacityDemand;
			}
		}
		public static class ShipmentDeliveryInfo {
			private TimeWindow deliveryWindow;
			private double capacityDemand;

			public ShipmentDeliveryInfo(TimeWindow deliveryWindow, double capacityDemand) {
				this.deliveryWindow = deliveryWindow;
				this.capacityDemand = capacityDemand;
			}
			public TimeWindow getdeliveryWindow() {
				return deliveryWindow;
			}
			public double getcapacityDemand() {
				return capacityDemand;
			}
		}

	public static void main(String[] args) {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = "contribs/freight/scenarios/CCTestInput/";
		//names of xml-files
		String carriersXML = "CCTestCarriers.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		/**
		 * System.out.println("Starting 'IsVehicleBigEnoughTest'...");
		 */

		Map<String, VehicleInfo> vehicleOperationWindows = new HashMap<>();

		Map<String, ShipmentPickupInfo> shipmentPickupWindows = new HashMap<>();

		Map<String, ShipmentDeliveryInfo> shipmentDeliveryWindows = new HashMap<>();

		//determine the operating hours of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
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
		}

		//determine pickup hours of shipments
		//Shipment ID (key as string) and times (value as double) are being stored in HashMaps
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				String shipmentID = shipment.getId().toString();
				TimeWindow shipmentPickupWindow = shipment.getPickupStartingTimeWindow();
				var shipmentSize = shipment.getCapacityDemand();
				shipmentPickupWindows.put(shipmentID, new ShipmentPickupInfo(shipmentPickupWindow, shipmentSize));
			}
		}

		//determine delivery hours of shipments
		//Shipment ID (key as string) and times (value as double) are being stored in HashMaps
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				String shipmentID = shipment.getId().toString();
				TimeWindow shipmentDeliveryWindow = shipment.getDeliveryStartingTimeWindow();
				var shipmentSize = shipment.getCapacityDemand();
				shipmentDeliveryWindows.put(shipmentID, new ShipmentDeliveryInfo(shipmentDeliveryWindow, shipmentSize));
			}
		}

		//check if operating hours of vehicles overlap with pickup hours of shipments
		Iterator<Map.Entry<String, ShipmentPickupInfo>> iteratorP = shipmentPickupWindows.entrySet().iterator();
		//iteration trough whole HashMap
		while (iteratorP.hasNext()) {
			Map.Entry<String, ShipmentPickupInfo> shipmentEntry = iteratorP.next();
			TimeWindow shipmentTimeWindow = shipmentEntry.getValue().getpickupWindow();
			Double demand = shipmentEntry.getValue().getcapacityDemand();
			//use doesShipmentFitInVehicle (see above)
			//check if shipment fits in vehicle
			boolean vehicleIsSufficient = vehicleOperationWindows.values().stream().map(VehicleInfo::getCapacity).anyMatch(capacity -> doesShipmentFitInVehicle(capacity, demand));
			//use doTimeWindowsOverlap function (see above)
			//if windows overlap, shipment can be picked up
			boolean isOverlaping = vehicleOperationWindows.values().stream().map(VehicleInfo::getOperationWindow).anyMatch(vehicleTimeWindow -> doTimeWindowsOverlap(vehicleTimeWindow, shipmentTimeWindow));
			//vehicle in operation must be large enough to carry shipment
			if (vehicleIsSufficient && isOverlaping) {
				iteratorP.remove();
			}else {
				System.out.println("---->>>>WARNING: Shipment cannot be picked up! Shipment ID: "+shipmentPickupWindows.keySet()+"| Sufficient capacity: *"+vehicleIsSufficient+"* | Matching time slot: *"+isOverlaping+"*");
			}
		}

		//check if operating hours of vehicles overlap with delivery hours of shipments
		Iterator<Map.Entry<String, ShipmentDeliveryInfo>> iteratorD = shipmentDeliveryWindows.entrySet().iterator();
		//iteration trough whole HashMap
		while (iteratorD.hasNext()) {
			Map.Entry<String, ShipmentDeliveryInfo> shipmentEntry = iteratorD.next();
			TimeWindow shipmentTimeWindow = shipmentEntry.getValue().getdeliveryWindow();
			Double demand = shipmentEntry.getValue().getcapacityDemand();
			//use doesShipmentFitInVehicle (see above)
			//check if shipment fits in vehicle
			boolean vehicleIsSufficient = vehicleOperationWindows.values().stream().map(VehicleInfo::getCapacity).anyMatch(capacity -> doesShipmentFitInVehicle(capacity, demand));
			//use doTimeWindowsOverlap function (see above)
			boolean isOverlaping = vehicleOperationWindows.values().stream().map(VehicleInfo::getOperationWindow).anyMatch(vehicleTimeWindow -> doTimeWindowsOverlap(vehicleTimeWindow, shipmentTimeWindow));
			//if windows overlap, shipment is covered and can be removed from map
			if (vehicleIsSufficient && isOverlaping) {
				iteratorD.remove();
			} else {
				System.out.println("---->>>>WARNING: Shipment cannot be delivered! Shipment ID: "+shipmentDeliveryWindows.keySet()+"| Sufficient capacity: *"+vehicleIsSufficient+"* | Matching time slot: *"+isOverlaping+"*");
			}
		}
	}
}
