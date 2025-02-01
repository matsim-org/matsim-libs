package org.matsim.freight.carriers.consistency_checkers;

import com.google.errorprone.annotations.Var;
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
 *
 */
public class CarrierIsVehicleBigEnoughTest {

		static class Pair<T, U> {
			T first;
			U second;

			public Pair(T first, U second) {
				this.first = first;
				this.second = second;
			}}

		public static void main(String[] args){

		// Relativer Pfad zu Freight/Scenarios/CCTestInput/
		String pathToInput = "contribs/freight/scenarios/CCTestInput/"; //Ich zeige dir später, wie wir das in als Input für die Tests ablegen und dann entsprechend nutzen können.

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

		List<Double> vehicleCapacityList = new ArrayList<>();

		//Ermitteln der 'other' capacity aller vehicle types
			for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'vehicle id'
				var vehicleType = carrierVehicle.getType().getId();
				//In CCTestVeh XML: 'capacity other'
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				//#### Test Output:
				System.out.println("Carrier ID: '" + vehicleType + "'. Vehicle capacity: " + capacity);
				vehicleCapacityList.add(capacity);
			}
		}
		//Fahrzeugkapazität wird aufsteigend sortiert (Kleinstes -> Größtes)
		Collections.sort(vehicleCapacityList);
		//#### Test Output:
		System.out.println("Kapa Liste: "+vehicleCapacityList);

		//Ermitteln der größten Sendung aus CCTestCarriers

		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				double shipmentSize = shipment.getSize();
				String shipmentID = shipment.getId().toString();
				System.out.println("Shipment ID: '" + shipmentID + "'. Shipment size: " + shipmentSize);
			}
		}
		}
}

