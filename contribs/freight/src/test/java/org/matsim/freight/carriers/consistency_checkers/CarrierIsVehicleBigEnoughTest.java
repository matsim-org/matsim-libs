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
 *
 */
public class CarrierIsVehicleBigEnoughTest {
	public static void main(String[] args){

		// Relativer Pfad zu Freight/Scenarios/CCTestInput/
		String pathToInput = "../../../scenarios/CCTestInput/";

		/** Einlesen der Konfigurationsdatei
		 * @KMT: hier wird folgender Fehler geworfen:
		 *
		 * C:\Users\anton\Desktop\AdvancedMATSim\matsim\src\main\java\org\matsim\core\config\ConfigUtils.java:51:37
		 * java: Inkompatible Typen: org.matsim.core.controler.Controler.DefaultFiles kann nicht in org.matsim.core.config.Config konvertiert werden
		 *
		 * Die Art und Weise, die Config zu deklarieren habe ich von hier ab Zeile 47: https://github.com/matsim-org/matsim-code-examples/blob/2024.x/src/main/java/org/matsim/codeexamples/config/RunFromConfigfileExample.java
		 * Alternativen funktionieren auch nicht, es kommt immer zu einem Fehler innerhalb der ConfigUtils.java...
		 * */

		Config config;
		if (args != null && args.length >= 1) {
			config = ConfigUtils.loadConfig(args); // note that this may process command line options
		} else {
			config = ConfigUtils.loadConfig("../../../scenarios/CCTestInput/config.xml");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		System.out.println("Starting 'IsVehicleBigEnoughTest'...");

		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				System.out.println("Carrier ID: " + carrier.getId() + " Other Vehicle capacity: " + capacity);
			}
		}
	}
}
