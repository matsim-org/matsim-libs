package playground.wrashid.artemis.checks;

import java.util.HashMap;

import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.MathLib;
import playground.wrashid.lib.obj.StringMatrix;

// check, if charging is happening even if vehicle not parked
public class CheckUncontrolledCharging {

	public static void main(String[] args) {
//		StringMatrix parkingTimes = GeneralLib.readStringMatrix("H:/data/experiments/ARTEMIS/nov2011/N-szenario11/output/parkingTimesAndLegEnergyConsumption_n.txt", "\t");
		StringMatrix parkingTimes = GeneralLib.readStringMatrix("c:/tmp/parkingTimesAndLegEnergyConsumption.txt", "\t");

		HashMap<String, Integer> indexOfParkingTimesOfAgent = new HashMap<String, Integer>();

		for (int i = 1; i < parkingTimes.getNumberOfRows(); i++) {
			String agentId = parkingTimes.getString(i, 0);
			if (!indexOfParkingTimesOfAgent.containsKey(agentId)) {
				indexOfParkingTimesOfAgent.put(agentId, i);
			}
		}

//		StringMatrix chargingLog = GeneralLib.readStringMatrix("H:/data/experiments/ARTEMIS/nov2011/N-szenario11/output/chargingLog_n.txt", "\t");
		StringMatrix chargingLog = GeneralLib.readStringMatrix("c:/tmp/chargingLog.txt", "\t");

		for (int i = 1; i < chargingLog.getNumberOfRows(); i++) {
			String chargingLogAgentId = chargingLog.getString(i, 1);
			int j = indexOfParkingTimesOfAgent.get(chargingLogAgentId);

			double startChargingTime=chargingLog.getDouble(i, 2);
			double endChargingTime=chargingLog.getDouble(i, 3);
			double parkingArrivalTime=parkingTimes.getDouble(j, 1);
			double parkingDeparturetime=parkingTimes.getDouble(j, 2);
			
			while (!GeneralLib.isIn24HourInterval(parkingArrivalTime,parkingDeparturetime,startChargingTime+0.1) || !GeneralLib.isIn24HourInterval(parkingArrivalTime,parkingDeparturetime,endChargingTime-0.1)) {
				DebugLib.traceAgent(new IdImpl(chargingLogAgentId));
				
				j++;
				parkingArrivalTime=parkingTimes.getDouble(j, 1);
				parkingDeparturetime=parkingTimes.getDouble(j, 2);
				//DebugLib.stopSystemAndReportInconsistency("charging not possible, when vehicle not parked");
				
				String parkingLogAgentId = parkingTimes.getString(j, 0);
				if (!chargingLogAgentId.equalsIgnoreCase(parkingLogAgentId)) {
					//DebugLib.stopSystemAndReportInconsistency("match not found for agent:" + chargingLogAgentId);
					System.out.println("inconsistency found for agent: " + chargingLogAgentId);
					break;
				}
			
			}

			

		}
	}
}
