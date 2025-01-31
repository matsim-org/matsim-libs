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
		String pathToInput = "contribs/freight/scenarios/CCTestInput/"; //Ich zeige dir später, wie wir das in als Input für die Tests ablegen und dann entsprechend nutzen können.

		/** Einlesen der Konfigurationsdatei
		 * @KMT: hier wird folgender Fehler geworfen:
		 *
		 * C:\Users\anton\Desktop\AdvancedMATSim\matsim\src\main\java\org\matsim\core\config\ConfigUtils.java:51:37
		 * java: Inkompatible Typen: org.matsim.core.controler.Controler.DefaultFiles kann nicht in org.matsim.core.config.Config konvertiert werden
		 *
		 * Die Art und Weise, die Config zu deklarieren habe ich von hier ab Zeile 47: https://github.com/matsim-org/matsim-code-examples/blob/2024.x/src/main/java/org/matsim/codeexamples/config/RunFromConfigfileExample.java
		 * Alternativen funktionieren auch nicht, es kommt immer zu einem Fehler innerhalb der ConfigUtils.java...
		 * */
		/**
		 * @Anton:
		 * 1.) Ich habe oben mal den Pfad korrigert. Das ist nicht relativ zu der aktuellen Klasse, sondern relativ zum Projekt. Das sollte so funktionieren. (mir tut es das)
		 * 2.) Ich habe die ConfigUtils.createConfig() Methode verwendet, um eine Config zu erstellen. Lesen ja eh nichts über die Kommandozeile als Argument ein und brauchen auch keine vorgefertigte Config.
		 * 3.) Dein o.g. FEHLER kam daher, dass du aus Versehen in den ConfigUtils den Rückgabetyp verändert hattest von Config auf DefaultFiles.  -> Habe ich rückgängig gemacht.
		 * 4.) unten habe ich noch das CarrierUtils.loadCarriersAccordingToFreightConfig( scenario ); hinzugefügt, damit die Carrier geladen werden. Das fehlte bisher (ist aber auch in den Code-Examples so drin
		 * 5.) Ich habe die verbliebenen XML Dateien (Carrier und vehicleTypes) mal von einigen Fehlern befreit, u.a.
		 *  - vehicleTypes: Es fehlte die schließende Klammer </vehicleDefinitions>
		 *  - carriers: es fehltes das übergreifende <carriers>  ... </carriers> tag (Plural
		 */

		Config config = ConfigUtils.createConfig();


		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(pathToInput + "CCTestCarriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		System.out.println("Starting 'IsVehicleBigEnoughTest'...");

		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				System.out.println("Carrier ID: " + carrier.getId() + " Other Vehicle capacity: " + capacity);
			}
		}
	}
}
