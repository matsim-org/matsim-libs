package playground.wrashid.artemis.checks;

import java.util.HashMap;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

// check, if charging is happening even if vehicle not parked
public class CheckUncontrolledCharging {

	public static void main(String[] args) {
		Matrix parkingTimes = GeneralLib.readStringMatrix(
				"H:/data/experiments/ARTEMIS/nov2011/N-szenario11/run1/output/parkingTimesAndLegEnergyConsumption.txt", "\t");
		// StringMatrix parkingTimes =
		// GeneralLib.readStringMatrix("c:/tmp/parkingTimesAndLegEnergyConsumption.txt",
		// "\t");

		HashMap<String, Integer> indexOfParkingTimesOfAgent = new HashMap<String, Integer>();

		for (int i = 1; i < parkingTimes.getNumberOfRows(); i++) {
			String agentId = parkingTimes.getString(i, 0);
			if (!indexOfParkingTimesOfAgent.containsKey(agentId)) {
				indexOfParkingTimesOfAgent.put(agentId, i);
			}
		}

		Matrix chargingLog = GeneralLib.readStringMatrix(
				"H:/data/experiments/ARTEMIS/nov2011/N-szenario11/run1/output/chargingLog.txt", "\t");
		// StringMatrix chargingLog =
		// GeneralLib.readStringMatrix("c:/tmp/chargingLog.txt", "\t");

		System.out.print("inconsitent Agents: ");
		
		for (int i = 1; i < chargingLog.getNumberOfRows(); i++) {
			String chargingLogAgentId = chargingLog.getString(i, 1);
			int j = indexOfParkingTimesOfAgent.get(chargingLogAgentId);

			double startChargingTime = chargingLog.getDouble(i, 2);
			double endChargingTime = chargingLog.getDouble(i, 3);
			double parkingArrivalTime = parkingTimes.getDouble(j, 1);
			double parkingDeparturetime = parkingTimes.getDouble(j, 2);

			do {
				//DebugLib.traceAgent(new IdImpl(chargingLogAgentId));

				

				String parkingLogAgentId = parkingTimes.getString(j, 0);
				if (!chargingLogAgentId.equalsIgnoreCase(parkingLogAgentId)) {
					// DebugLib.stopSystemAndReportInconsistency("match not found for agent:"
					// + chargingLogAgentId);
					//System.out.println("no charging log found for agent: " + chargingLogAgentId);
					System.out.print(chargingLogAgentId + ", ");
					break;
				}

				if (GeneralLib.isIn24HourInterval(parkingArrivalTime, parkingDeparturetime, startChargingTime + 0.1)) {
					if (GeneralLib.getIntervalDuration(parkingArrivalTime, startChargingTime + 1) > 10) {
						//System.out.println("overlapping charging log (e.g. from prev. day): " + chargingLogAgentId);
						System.out.print(chargingLogAgentId + ", ");
					}
					break;
				}
				
				j++;
				parkingArrivalTime = parkingTimes.getDouble(j, 1);
				parkingDeparturetime = parkingTimes.getDouble(j, 2);

			} while (true);

		}
	}
}
