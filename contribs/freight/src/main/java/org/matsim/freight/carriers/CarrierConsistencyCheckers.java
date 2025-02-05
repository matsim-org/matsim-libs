package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.*;

public class CarrierConsistencyCheckers {

	private static final Logger log = LogManager.getLogger(CarrierConsistencyCheckers.class);

	/**
	 * @author antonstock
	 * This method checks if every carrier is able to handle every given shipment (TODO: services) with the available fleet. This method doesnot check the vehicle's schedule but the capacity only.
	 */
	public static void capacityCheck(Carriers carriers) {
		//determine the capacity of all available vehicles (carrier after carrier)
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<Double> vehicleCapacityList = new ArrayList<>();
			List<CarrierJob> jobTooBigForVehicle = new LinkedList<>();

			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
				vehicleCapacityList.add(carrierVehicle.getType().getCapacity().getOther());
			}

			final double maxVehicleCapacity = Collections.max(vehicleCapacityList);

			//determine jobs with capacity demand > maxVehicleCapacity
			//Shipment ID (key as string) and capacity demand (value as double) are being stored in HashMap 'shipmentsTooBigForVehicle'

			for (CarrierShipment shipment : carrier.getShipments().values()) {
				//TODO: @KMT: Muss ich hier statt CarrierShipment auch CarrierJob nutzen? ich finde nur .getServices und keine .getJobs o.ö.
				//@Anton: einfach ne andere Schleife mit carrier.getServices().values() machen.

				//@Anton: siehe unten: Musst du wirklich alle Sendungen speichern oder reicht es nicht, nur die zu speichern, die zu groß sind?
				//@KMT: war das deine Idee? ->done
				System.out.println(shipment.getCapacityDemand()); //kann dann spöter weg /alternativ in log.debug nutzen.
				if (shipment.getCapacityDemand() > maxVehicleCapacity) {
					jobTooBigForVehicle.add(shipment);
				}
			}

			//Todo: @Anton: Bitte noch dran denken, dass der Test auch für Services (als alternative Auftragsdefinition) funktionieren muss.
			// Hinweis CarrierShipment und CarrierJob haben ein gemeinsames Interface, das du nutzen kannst, um nur einen "Container" für beide zu haben.
			//@KMT: Hier brauche ich bitte nochmal eine genauere/andere Erklärung. -> to be done
			// @Anton: Es gibt zwei mögliche Arten von Aufträgen für die Carrier: Services und Shipments. Dein Checker muss für beides funktionieren,
			// wobei immer jeder Carrier nur Services oder Shipments hat.
			// Das "nette" ist aber, dass beide das Interface "CarrierJob" implementieren. und du damit eine Liste von CarrierJobs machen kannst. (s. auch Kommentar weiter oben).


			//if map is empty, there is a sufficient vehicle for every job
			if (jobTooBigForVehicle.isEmpty()) {
				log.info("Carrier '{}': At least one vehicle has sufficient capacity ({}) for all jobs.", carrier.getId().toString(), maxVehicleCapacity);
				//Todo: @ Anton: Return True
				// Würde vremutlich auf log.debug gehen, damit er dann nicht die Konsole voll schreibt. Aber ist auch erstmal ok so :)
			} else {
				log.warn("Carrier '{}': Demand of {} job(s) too high!", carrier.getId().toString(), jobTooBigForVehicle.size());
				for (CarrierJob job : jobTooBigForVehicle) {
					log.info("Demand of Job '{}' is too high: '{}'", job.getId().toString(),job.getCapacityDemand());
				}
				//if map is not empty, these jobs are too large for the existing fleet.
				//Todo: @ Anton: Return false
			}
		}
	}

	public static void scheduleTest(Carriers carriers) {
		//TODO
	}

}
