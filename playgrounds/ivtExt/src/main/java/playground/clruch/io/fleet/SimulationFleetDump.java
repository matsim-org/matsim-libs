package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.Random;

import playground.clruch.export.AVStatus;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjects;
import playground.clruch.net.VehicleContainer;

enum SimulationFleetDump {
	;
	public static void of(DayTaxiRecord dayTaxiRecord) {
		Random random = new Random();
		for (int now : dayTaxiRecord.keySet()) {
			SimulationObject simulationObject = new SimulationObject();
			simulationObject.now = now;
			simulationObject.vehicles = new ArrayList<>();
			for (TaxiStamp timeStamp : dayTaxiRecord.get(now)) {
				VehicleContainer vc = new VehicleContainer();
				vc.vehicleIndex = timeStamp.id;
				vc.linkIndex = random.nextInt(500);
				vc.avStatus = AVStatus.REBALANCEDRIVE;
				simulationObject.vehicles.add(vc);
			}
			SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);
			// new StorageSubscriber().handle(simulationObject);
		}
		dayTaxiRecord.get(22210);

	}

}
