package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.*;

public class CarrierConsistencyCheckers {

	/**
	 * @author antonstock
	 * This method checks if every carrier is able to handle every given shipment (TODO: services) with the available fleet. This method doesnot check the vehicle's schedule but the capacity only.
	 */
	private static final Logger log = LogManager.getLogger(CarrierConsistencyCheckers.class);

	public static void capacityCheck(Carriers carriers) {
		List<Double> vehicleCapacityList = new ArrayList<>();
		List<CarrierJob> jobTooBigForVehicle = new LinkedList<>();

		// @Anton: Hinter die Id sollte immer noch ein Typ also z.b. Id<CarrierShipment>...
		// wenn du meinen Kommentar unten siehst, würde ich es aber eh zu einer Liste umbauen:
		//ArrayList<CarrierJob> shipmentsTooBigForVehicleList = new LinkedList<>(); // CarrierJob ist das Interface, das von CarrierShipment und CarrierService implementiert wird. Daher kann die Liste beides speichern.
		/** @KMT: ArrayList<CarrierJob> shipmentsTooBigForVehicleList = new LinkedList<>(); erzeugt "incompatible types", ich habe mich für eine normale "List" entschieden, weil sowieso kaum auf die Liste zugegriffen werden muss.
		 *  Passt das?
		 * List jobTooBigForVehicle ersetzt Map shipmentsTooBigForVehicle => allgemeinere Namensgebung
		*
		 *
		 * */
		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				//In CCTestVeh XML: 'capacity other'
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}

			final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

			//determine jobs with capacity demand > maxVehicleCapacity
			//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentsTooBigForVehicle'

			for (CarrierShipment shipment : carrier.getShipments().values()) {
				//TODO: @KMT: Muss ich hier statt CarrierShipment auch CarrierJob nutzen? ich finde nur .getServices und keine .getJobs o.ö.

				//@Anton: siehe unten: Musst du wirklich alle Sendungen speichern oder reicht es nicht, nur die zu speichern, die zu groß sind?
				//@KMT: war das deine Idee? ->done
				System.out.println(shipment.getCapacityDemand());
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(shipment);
				}
			}

			//@Anton: Ich überlege, ob es von der Logik her nicht reicht, einfach nur sich die größte Kapazität der Fahrzeuge zu merken (braucht dann keine Liste
			// und dann zu prüfen, ob die größte Kapazität größer ist als die größte Nachfrage.
			// Und dann einfach nur die Sendungen, bei denen die Nachfrage größer ist als die Kapazität zu speichern.
			// Dann brauchst du auch keine große Map etc.
			// @KMT: Wenn man das so macht, kann man aber nicht ausgeben, wie viele Sendungen zu groß sind. Ich habe die Logik ein bisschen verändert, es wird sofort geprüft, ob die Sendung passt und nur gespeichert, wenn nicht.
			// Gibt bestimmt noch eine andere Möglichkeit, fällt mir gerade nicht ein. -> to be done
			// @Anton: Wenn die nur eine Liste machst, wo du die Sendungen speicherst, dann kannst du am Ende dennoch über liste.size() schauen, wieviele Einträge es sind.
			// Also ne Liste statt der Map. Und die Liste nimmt Objelkte vom Typ <CarrierJob> auf. Dass sind dann Shipments oder Services.
			// Und dann kannst du zur Ausgabe über die Liste iterieren (for schleife) und dann .getId bzw .getCapacityDemand als Ausgabe nutzen.
			//@KMT: -> done


			//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
			// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.
			//@KMT: Hier brauche ich bitte nochmal eine genauere/andere Erklärung. -> to be done
			// @Anton: Es gibt zwei mögliche Arten von Aufträgen für die Carrier: Services und Shipments. Dein Checker muss für beides funktionieren,
			// wobei immer jeder Carrier nur Services oder Shipments hat.
			// Das "nette" ist aber, dass beide das Interface "CarrierJob" implementieren. und du damit eine Liste von CarrierJobs machen kannst. (s. auch Kommentar weiter oben).


			//if map is empty, there is a sufficient vehicle for every job
			if (jobTooBigForVehicle.isEmpty()) {
				log.info("Carrier '{}': At least one vehicle has sufficient capacity ({}) for all jobs.", carrier.getId().toString(), maxVehicleCapacity);
			} else {
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
					//if map is not empty, these jobs are too large for the existing fleet.
			}
			vehicleCapacityList.clear();
			jobTooBigForVehicle.clear();
			}
		}

	public static void scheduleTest(Carriers carriers) {
		//TODO
	}

}
