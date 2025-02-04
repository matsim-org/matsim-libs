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
		Map<Id, Integer> shipmentsTooBigForVehicle = new HashMap<>();  //@Anton: Warum speicherst du die Id als String und nicht als Id? -> @KMT: Keine Ahnung :D. Fixed.


		// @Anton: Wenn ich es richtig sehe, prüfst du nun gerade Carrier-übergreifend, weil du dir alle Fahrzeuge aus allen Carriern holst und
		// dann die Kapazitäten aus allen aufträgen aller Carrier vergleichst.
		// mMn müsstest du die Prüfung (und Ausgabe) bitte je Carrier machen.
		// Denn 1) hat jeder Carrier SEINE Aufträge und SEINE Fahrzeuge und nur da muss es passen und
		// 2.) können sich die Ids der Fahrzeuge und Aufträge in verschiedenen Carriern wiederholen.

		//@KMT: Done. Code wird jetzt nacheinander für alle vorhandenen Carrier ausgeführt, nicht einmal für alle Carrier auf einen Schlag. Ausgabe erfolgt je Carrier.

		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'capacity other'
				var capacity = carrierVehicle.getType().getCapacity().getOther();
				vehicleCapacityList.add(capacity);
			}

			double maxVehicleCapacity = Collections.max(vehicleCapacityList);

		//determine shipments with capacity demand > maxVehicleCapacity
		//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentsTooBigForVehicle'
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				//double shipmentSize = shipment.getCapacityDemand();
				//Id shipmentID = shipment.getId(); //@Anton: Variablen-Name: Wenn der auf ID endet, würde ich auch eine Id und keine String erwarten -> @KMT: Done.
				//@Anton: Die genutzen Variablen sind so kurz, dass du diese nicht extra erstellen musst, sondern einfach hier direkt verwenden kannst.
				// --> Hier könntest du auch einfach shipmentsTooBigForVehicle.put(shipment.getId().toString(), shipment.getCapacityDemand());
				// Bitte auch meinen Kommentar oben zur Id vs String beachten
				// @KMT: Done.
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


		//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
		// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.
			//@KMT: Hier brauche ich bitte nochmal eine genauere/andere Erklärung. -> to be done

		//if map is empty, there is a sufficient vehicle for every shipment
		if (shipmentsTooBigForVehicle.isEmpty()) {
			log.info("Carrier '{}': At least one vehicle has sufficient capacity ('{}') for all shipments.", carrier.getId().toString(), maxVehicleCapacity);
		} else {
			log.warn("Carrier '{}': '{}' shipment(s) is/are too big!", carrier.getId().toString(), shipmentsTooBigForVehicle.size());
			//if map is not empty, these shipments are too large for the existing fleet.
			for (Map.Entry<Id, Integer> entry : shipmentsTooBigForVehicle.entrySet()) {
				log.warn("Shipment '{}' is too big for the largest available vehicle (capacity: '{}').", entry.getKey().toString(),maxVehicleCapacity);
				//@Anton: Wäre auch gut, die CarrierId mit auszugeben, oder (noch einfacher) eine Zeile vorher einmalig zu schreiben, um welchen Carrier es gerade geht.
				// Gerade weil verschiedene Carrier ja die gleiche Carrier Id haben können.
				// @Anton: Wäre gut, wenn du auhc die Göße des größte Fahrzeugs gleich mit raus schreiben würdest als Service für den Nutzer.
				// @KMT: Done & Done
			}
		}
	vehicleCapacityList.clear();
		}
	}


	public static void scheduleTest(Carriers carriers) {
		//TODO
	}

}
