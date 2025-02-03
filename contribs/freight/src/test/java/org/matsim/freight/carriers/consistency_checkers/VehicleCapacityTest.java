package org.matsim.freight.carriers.consistency_checkers;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;

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

		CarrierConsistencyCheckers.capacityCheck(carriers);
	}

}

