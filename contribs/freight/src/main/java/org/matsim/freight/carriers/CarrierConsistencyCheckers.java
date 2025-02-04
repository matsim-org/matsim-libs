package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.*;

public class CarrierConsistencyCheckers {

	/**
	 * @author antonstock
	 * TODO: @Anton: Bitte hier noch kurz ausführen, was die Methode macht.
	 *
	 */
	private static final Logger log = LogManager.getLogger(CarrierConsistencyCheckers.class);

	public static void capacityCheck(Carriers carriers) {
		List<Double> vehicleCapacityList = new ArrayList<>();
		Map<Id, Integer> shipmentsTooBigForVehicle = new HashMap<>(); // Todo @Anton: Hinter die Id sollte immer noch ein Typ also z.b. Id<CarrierShipment>...
		// wenn du meinen Kommentar unten siehst, würde ich es aber eh zu einer Liste umbauen:
		//ArrayList<CarrierJob> shipmentsTooBigForVehicleList = new LinkedList<>(); // CarrierJob ist das Interface, das von CarrierShipment und CarrierService implementiert wird. Daher kann die Liste beides speichern.

		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'capacity other'
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}

		final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

		//determine shipments with capacity demand > maxVehicleCapacity
		//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentsTooBigForVehicle'
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				//shipmentsTooBigForVehicle.put(shipment.getId(), shipment.getCapacityDemand());
				// Todo: @Anton: siehe unten: Musst du wirklich alle Sendungen speichern oder reicht es nicht, nur die zu speichern, die zu groß sind?
				System.out.println(shipment.getCapacityDemand());
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					shipmentsTooBigForVehicle.put(shipment.getId(), shipment.getCapacityDemand());
				}
			}
		//is there a sufficient vehicle for every shipment?
		//shipmentsTooBigForVehicle.entrySet().removeIf(entry -> entry.getValue() <= maxVehicleCapacity);
		//TODO: @Anton: Ich überlege, ob es von der Logik her nicht reicht, einfach nur sich die größte Kapazität der Fahrzeuge zu merken (braucht dann keine Liste
		// und dann zu prüfen, ob die größte Kapazität größer ist als die größte Nachfrage.
		// Und dann einfach nur die Sendungen, bei denen die Nachfrage größer ist als die Kapazität zu speichern.
		// Dann brauchst du auch keine große Map etc.
			// @KMT: Wenn man das so macht, kann man aber nicht ausgeben, wie viele Sendungen zu groß sind. Ich habe die Logik ein bisschen verändert, es wird sofort geprüft, ob die Sendung passt und nur gespeichert, wenn nicht.
			// Gibt bestimmt noch eine andere Möglichkeit, fällt mir gerade nicht ein. -> to be done
				// @Anton: Wenn die nur eine Liste machst, wo du die Sendungen speicherst, dann kannst du am Ende dennoch über liste.size() schauen, wieviele Einträge es sind.
			    // Also ne Liste statt der Map. Und die Liste nimmt Objelkte vom Typ <CarrierJob> auf. Dass sind dann Shipments oder Services.
				// Und dann kannst du zur Ausgabe über die Liste iterieren (for schleife) und dann .getId bzw .getCapacityDemand als Ausgabe nutzen.


		//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
		// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.
			//@KMT: Hier brauche ich bitte nochmal eine genauere/andere Erklärung. -> to be done
			  // @Anton: Es gibt zwei mögliche Arten von Aufträgen für die Carrier: Services und Shipments. Dein Checker muss für beides funktionieren,
			  // wobei immer jeder Carrier nur Services oder Shipments hat.
			  // Das "nette" ist aber, dass beide das Interface "CarrierJob" implementieren. und du damit eine Liste von CarrierJobs machen kannst. (s. auch Kommentar weiter oben).


		//if map is empty, there is a sufficient vehicle for every shipment
		if (shipmentsTooBigForVehicle.isEmpty()) {
			log.info("Carrier '{}': At least one vehicle has sufficient capacity ('{}') for all shipments.", carrier.getId().toString(), maxVehicleCapacity);
		} else {
			log.warn("Carrier '{}': '{}' shipment(s) is/are too big!", carrier.getId().toString(), shipmentsTooBigForVehicle.size());
			//if map is not empty, these shipments are too large for the existing fleet.
			for (Map.Entry<Id, Integer> entry : shipmentsTooBigForVehicle.entrySet()) {
				log.warn("Shipment '{}' is too big for the largest available vehicle (capacity: '{}').", entry.getKey().toString(),maxVehicleCapacity);
			}
		}
	vehicleCapacityList.clear();
		}
	}


	public static void scheduleTest(Carriers carriers) {
		//TODO
	}

}
