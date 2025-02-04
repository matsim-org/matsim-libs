package org.matsim.freight.carriers;

import java.util.*;

public class CarrierConsistencyCheckers {

	/**
	 * @author antonstock
	 * TODO: @Anton: Bitte hier noch kurz ausführen, was die Methode macht.
	 *
	 */
	public static void capacityCheck(Carriers carriers) {
		List<Double> vehicleCapacityList = new ArrayList<>();
		Map<String, Double> shipmentSizes = new HashMap<>();  //Todo: @Anton: Warum speicherst du die Id als String und nicht als Id?


		//Todo: @Anton: Wenn ich es richtig sehe, prüfst du nun gerade Carrier-übergreifend, weil du dir alle Fahrzeuge aus allen Carriern holst und
		// dann die Kapazitäten aus allen aufträgen aller Carrier vergleichst.
		// mMn müsstest du die Prüfung (und Ausgabe) bitte je Carrier machen.
		// Denn 1) hat jeder Carrier SEINE Aufträge und SEINE Fahrzeuge und nur da muss es passen und
		// 2.) können sich die Ids der Fahrzeuge und Aufträge in verschiedenen Carriern wiederholen.

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
				String shipmentID = shipment.getId().toString(); //Todo: (( @Anton: Variablen-Name: Wenn der auf ID endet, würde ich auch eine Id und keine String erwarten ))
				//Todo: @Anton: Die genutzen Variablen sind so kurz, dass du diese nicht extra erstellen musst, sondern einfach hier direkt verwenden kannst.
				// --> Hier könntest du auch einfach shipmentSizes.put(shipment.getId().toString(), shipment.getCapacityDemand());
				// Bitte auch meinen Kommentar oben zur Id vs String beachten
				shipmentSizes.put(shipmentID, shipmentSize);
				// Todo: @Anton: siehe unten: Musst du wirklich alle Sendungen speichern oder reicht es nicht, nur die zu speichern, die zu groß sind?
			}
		}
		//is there a sufficient vehicle for every shipment?
		double maxCapacity = Collections.max(vehicleCapacityList);
		shipmentSizes.entrySet().removeIf(entry -> entry.getValue() <= maxCapacity);

		//TODO: @Anton: Ich überlege, ob es von der Logik her nicht reicht, einfach nur sich die größte Kapazität der Fahrzeuge zu merken (braucht dann keine Liste
		// und dann zu prüfen, ob die größte Kapazität größer ist als die größte Nachfrage.
		// Und dann einfach nur die Sendungen, bei denen die Nachfrage größer ist als die Kapazität zu speichern.
		// Dann brauchst du auch keine große Map etc.

		//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
		// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.

		//if map is empty, there is a sufficient vehicle for every shipment
		if (shipmentSizes.isEmpty()) {
			System.out.println("At least one available vehicle has sufficient capacity.");
		} else {
			//if map is not empty, these shipments are too large for the existing fleet.
			System.out.println("WARNING: At least one shipment is too large for the available vehicles.");
			for (Map.Entry<String, Double> entry : shipmentSizes.entrySet()) {
				//Todo: @Anton: Wäre auch gut, die CarrierId mit auszugeben, oder (noch einfacher) eine Zeile vorher einmalig zu schreiben, um welchen Carrier es gerade geht.
				// Gerade weil verschiedene Carrier ja die gleiche Carrier Id haben können.
				// Todo: @Anton: Wäre gut, wenn du auhc die Göße des größte Fahrzeugs gleich mit raus schreiben würdest als Service für den Nutzer.
				System.out.println("Shipment '" + entry.getKey() + "' sized '" + entry.getValue() + "' is too big for available vehicles.");
			}
		}
	}


	public static void scheduleTest(Carriers carriers) {
		//TODO
	}

}
