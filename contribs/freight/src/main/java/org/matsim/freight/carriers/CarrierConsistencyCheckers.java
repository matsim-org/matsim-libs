package org.matsim.freight.carriers;

import java.util.*;

public class CarrierConsistencyCheckers {
	public static void capacityCheck(Carriers carriers) {
		List<Double> vehicleCapacityList = new ArrayList<>();
		Map<String, Double> shipmentSizes = new HashMap<>();

		//determine the capacity of all available vehicles
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'capacity other'
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				vehicleCapacityList.add(capacity);
			}
		}

		//determine capacity demand of all shipments
		//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentSizes'
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				double shipmentSize = shipment.getCapacityDemand();
				String shipmentID = shipment.getId().toString();
				shipmentSizes.put(shipmentID, shipmentSize);
			}
		}
		//is there a sufficient vehicle for every shipment?
		double maxCapacity = Collections.max(vehicleCapacityList);
		shipmentSizes.entrySet().removeIf(entry -> entry.getValue() <= maxCapacity);


		//if map is empty, there is a sufficient vehicle for every shipment
		if (shipmentSizes.isEmpty()) {
			System.out.println("At least one available vehicle has sufficient capacity.");
		} else {
			//if map is not empty, these shipments are too large for the existing fleet.
			System.out.println("WARNING: At least one shipment is too large for the available vehicles.");
			for (Map.Entry<String, Double> entry : shipmentSizes.entrySet()) {
				System.out.println("Shipment '" + entry.getKey() + "' sized '" + entry.getValue() + "' is too big for available vehicles.");
			}
		}
	}
}
