package playground.wrashid.artemis.smartCharging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.MathLib;
import playground.wrashid.lib.tools.txtConfig.TxtConfig;

// example config: H:\data\experiments\ARTEMIS\nov2011\SR-szenario11\run1\config.txt
public class SmartCharger {

	private static TxtConfig config;

	public static void main(String[] args) {
		config = new TxtConfig(args[0]);

		Matrix parkingTimes = GeneralLib.readStringMatrix(
				config.getParameterValue("parkingTimesFileWithCorrectParkingIds"), "\t");
		removeInconsistentAgents(parkingTimes, 0);

		HashMap<String, Integer> indexOfParkingTimesOfAgent = new HashMap<String, Integer>();

		for (int i = 1; i < parkingTimes.getNumberOfRows(); i++) {
			String agentId = parkingTimes.getString(i, 0);
			if (!indexOfParkingTimesOfAgent.containsKey(agentId)) {
				indexOfParkingTimesOfAgent.put(agentId, i);
			}
		}

		Matrix chargingLog = GeneralLib.readStringMatrix(config.getParameterValue("dumbChargingLogWithCorrectParkingIds"),
				"\t");
		removeInconsistentAgents(chargingLog,1);

		Matrix outputChargingLog = initOutputCharingLogMatrix();
		
		for (int i = 1; i < chargingLog.getNumberOfRows(); i++) {
			String agentId = chargingLog.getString(i, 1);
			String linkId = chargingLog.getString(i, 0);
			double startSOCInJoule=chargingLog.getDouble(i, 4);
			int j = indexOfParkingTimesOfAgent.get(agentId);

			if (agentId.equalsIgnoreCase("6455870")){
				System.out.println();
			}
			
			while (j<parkingTimes.getNumberOfRows() && !MathLib.equals(parkingTimes.getDouble(j, 1), chargingLog.getDouble(i, 2), 1.0)) {
				j++;
			}

			if (j==parkingTimes.getNumberOfRows() || !agentId.equalsIgnoreCase(parkingTimes.getString(j, 0))) {
				
				DebugLib.stopSystemAndReportInconsistency("match not found for agent:" + agentId);
			}

			// don't try smart charging, charging required is bigger than
			// parking duration
			if (MathLib.equals(parkingTimes.getDouble(j, 2), chargingLog.getDouble(i, 3), 1.0)) {

				// TODO: log here just dumb charging.
				continue;
			}

			double parkingArrivalTime = parkingTimes.getDouble(j, 1);
			double parkingDepartureTime = parkingTimes.getDouble(j, 2);
			double chargingDuration = GeneralLib.getIntervalDuration(chargingLog.getDouble(i, 2), chargingLog.getDouble(i, 3));
			double chargingPower = (chargingLog.getDouble(i, 5) - chargingLog.getDouble(i, 4)) / chargingDuration;
			LinkedList<ChargingTime> chargingTimes = ChargingTime.get15MinChargingBins(parkingArrivalTime, parkingDepartureTime);
			LinkedList<ChargingTime> randomChargingTimes = SmartCharger.getRandomChargingTimes(chargingTimes, chargingDuration);
			LinkedList<ChargingTime> sortedChargingTimes = ChargingTime.sortChargingTimes(randomChargingTimes);

			for (ChargingTime chargingTime : sortedChargingTimes) {
				ArrayList<String> row=new ArrayList<String>();
				
				double startChargingTime = GeneralLib.projectTimeWithin24Hours(chargingTime.getStartChargingTime());
				double endChargingTime = GeneralLib.projectTimeWithin24Hours(chargingTime.getEndChargingTime());
				double charingDur=GeneralLib.getIntervalDuration(startChargingTime, endChargingTime);
				double endSOCInJoule=startSOCInJoule+charingDur*chargingPower;
				row.add(linkId);
				row.add(agentId);
				row.add(Double.toString(startChargingTime));
				row.add(Double.toString(endChargingTime));
				row.add(Double.toString(startSOCInJoule));
				row.add(Double.toString(endSOCInJoule));
				outputChargingLog.addRow(row);
				
				startSOCInJoule=endSOCInJoule;
			}
		}
		
		outputChargingLog.writeMatrix(config.getParameterValue("outputChargingLog"));
		parkingTimes.writeMatrix(config.getParameterValue("outputParkingTimes"));
	}

	private static Matrix initOutputCharingLogMatrix() {
		Matrix outputChargingLog=new Matrix();
		ArrayList<String> titleArray=new ArrayList<String>();
		titleArray.add("linkId");
		titleArray.add("agentId");
		titleArray.add("startChargingTime");
		titleArray.add("endChargingTime");
		titleArray.add("startSOCInJoule");
		titleArray.add("endSOCInJoule");
		outputChargingLog.addRow(titleArray);
		return outputChargingLog;
	}

	private static void removeInconsistentAgents(Matrix stringMatrix, int columnIndexOfAgentIds) {
		String removeInconsistentAgents = config.getParameterValue("removeInconsistentAgents");
		System.out.println("removing agents: " + removeInconsistentAgents + " ");
		if (removeInconsistentAgents != null) {
			String[] split=removeInconsistentAgents.split(",");
			LinkedList<String> agentIdsToBeRemoved=new LinkedList<String>();
			
			for (String agentId:split){
				agentId=agentId.trim();
				if (agentId.length()>0){
					agentIdsToBeRemoved.add(agentId);
				}
			}
			
			int i=1;
			while (i < stringMatrix.getNumberOfRows()) {
				String agentId = stringMatrix.getString(i, columnIndexOfAgentIds);
				if (removeInconsistentAgents.contains(agentId)) {
					stringMatrix.deleteRow(i);
					System.out.print(".");
				}
				i++;
			}
			System.out.println();
		}
	}

	public static LinkedList<ChargingTime> getRandomChargingTimes(LinkedList<ChargingTime> chargingTimes,
			double totalChargingTimeNeeded) {
		LinkedList<ChargingTime> result = new LinkedList<ChargingTime>();

		Random rand = new Random();
		PriorityQueue<SortableMapObject<ChargingTime>> priorityQueue = new PriorityQueue<SortableMapObject<ChargingTime>>();

		for (ChargingTime chargingTime : chargingTimes) {
			priorityQueue.add(new SortableMapObject<ChargingTime>(chargingTime, rand.nextDouble()));
		}

		while (!MathLib.equals(totalChargingTimeNeeded, 0.0, 0.1)) {
			ChargingTime chargingTime = null;
			try {
				chargingTime = priorityQueue.poll().getKey();
			} catch (Exception e) {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
			if (chargingTime.getDuration() < totalChargingTimeNeeded) {
				totalChargingTimeNeeded -= chargingTime.getDuration();
				result.add(chargingTime);
			} else {
				ChargingTime tmpChargingTime = new ChargingTime(chargingTime.getStartChargingTime(),
						chargingTime.getStartChargingTime() + totalChargingTimeNeeded);
				totalChargingTimeNeeded -= tmpChargingTime.getDuration();
				result.add(tmpChargingTime);

				if (!MathLib.equals(totalChargingTimeNeeded, 0.0, 0.1)) {
					DebugLib.stopSystemAndReportInconsistency();
				}
			}

		}

		return result;
	}

}
