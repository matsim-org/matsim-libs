package org.matsim.freight.carriers.consistency_checkers;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;

import java.util.*;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 *  @author antonstock
 *	This class will check if the given vehicles have enough capacity to meet the capacity demand (size) of given shipments.
 *	Please change the input path and names of xml files below.
 */
public class VehicleCapacityTest {

	public static void main(String[] args){

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
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		/**
		* System.out.println("Starting 'IsVehicleBigEnoughTest'...");
		*/

		List<Double> vehicleCapacityList = new ArrayList<>();
		Map<String, Double> shipmentSizes = new HashMap<>();

		//determine the capacity of all available vehicles
			for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'vehicle id'
				var vehicleType = carrierVehicle.getType().getId();
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
		if(shipmentSizes.isEmpty()){
			System.out.println("At least one available vehicle has sufficient capacity.");
		} else {
			//if map is not empty, these shipments are too large for the existing fleet.
			System.out.println("WARNING: At least one shipment is too large for the available vehicles.");
			for (Map.Entry<String, Double> entry : shipmentSizes.entrySet()) {
				System.out.println("Shipment '" + entry.getKey() + "' sized '" + entry.getValue()+"' is too big for available vehicles.");
			}
		}
	}
}

