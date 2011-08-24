package playground.wrashid.PSF.energy.charging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.energy.consumption.LinkEnergyConsumptionLog;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.lib.GeneralLib;

public class ChargingTimes {

	private PriorityQueue<ChargeLog> chargingTimes = new PriorityQueue<ChargeLog>();
	private static final int numberOfTimeBins = 96;

	/**
	 * This fills out the SOC of the vehicle. But it does not take energy consumption into account.
	 * The invoker must take this into account himself.
	 * 
	 * @return
	 */
	public double[][] getSOCWithoutEnergyConsumption() {
		double[][] SOCWithoutEnergyConsumption = new double[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];

		// go through all the chargings logs. Find out for each charging log,
		// the SOC of the car (and in the time inbetween).
		for (ChargeLog curChargeLog : chargingTimes) {
			int index = Math.round((float) Math.floor((curChargeLog.getStartChargingTime() / 900)));
			double endSOC = curChargeLog.getEndSOC();

			SOCWithoutEnergyConsumption[index][ParametersPSF.getHubLinkMapping().getHubNumber(curChargeLog.getLinkId().toString())] = endSOC;			
		}

		// fill all zeros with the previous SOC.
		for (int i=0;i<numberOfTimeBins;i++){
			for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
				if (SOCWithoutEnergyConsumption[i][j]==0.0 && i!=0){
					SOCWithoutEnergyConsumption[i][j]=SOCWithoutEnergyConsumption[i-1][j];
				}
			}
		}
		
		// copy the last SOC on all SOCs at the beginning (for each hub)
		for (int i=0;i<ParametersPSF.getNumberOfHubs();i++){
			for (int j=0;j<numberOfTimeBins;j++){
				if (SOCWithoutEnergyConsumption[j][i]==0.0){
					SOCWithoutEnergyConsumption[j][i]=SOCWithoutEnergyConsumption[numberOfTimeBins-1][i];
				} else {
					break;
				}
			}
		}
		
		
		return SOCWithoutEnergyConsumption;
	}

	public double getTotalEnergyCharged() {
		double totalEnergyCharged = 0;

		Object[] iterChargingTimes = chargingTimes.toArray();
		Arrays.sort(iterChargingTimes);

		for (int i = 0; i < iterChargingTimes.length; i++) {
			ChargeLog curItem = (ChargeLog) iterChargingTimes[i];
			totalEnergyCharged += curItem.getEnergyCharged();
		}

		return totalEnergyCharged;
	}

	public void addChargeLog(ChargeLog chargeLog) {
		chargingTimes.add(chargeLog);
	}

	/**
	 * This method does not give back the original list, but a copy.
	 * 
	 * @return
	 */
	public LinkedList<ChargeLog> getChargingTimes() {
		LinkedList<ChargeLog> list = new LinkedList<ChargeLog>();
		Object[] iterChargingTimes = chargingTimes.toArray();
		Arrays.sort(iterChargingTimes);

		for (int i = 0; i < iterChargingTimes.length; i++) {
			ChargeLog curItem = (ChargeLog) iterChargingTimes[i];
			list.add(curItem);
		}

		return list;
	}

	/**
	 * Just prints out sorted after the time (starting with 0:00) Note: This is
	 * not the order in which the charging happened.
	 */
	public void print() {
		Object[] iterChargingTimes = chargingTimes.toArray();
		Arrays.sort(iterChargingTimes);

		for (int i = 0; i < iterChargingTimes.length; i++) {
			ChargeLog curItem = (ChargeLog) iterChargingTimes[i];
			curItem.print();
		}
	}

	/**
	 * TODO: replace default max battery capacity with a class, which maps max
	 * battery of vehicles on an individual vehicle basis.
	 * 
	 * This
	 * 
	 * @param energyConsumption
	 * 
	 */

	public void updateSOCs(EnergyConsumption energyConsumption) {
		double startSOC = ParametersPSF.getDefaultMaxBatteryCapacity();

		LinkedList<LinkEnergyConsumptionLog> consumptionLog = (LinkedList<LinkEnergyConsumptionLog>) energyConsumption
				.getLinkEnergyConsumption().clone();
		LinkEnergyConsumptionLog curConsumptionLog = consumptionLog.poll();

		// take both energy consumption and charging into consideration for
		// calculating the SOC

		Object[] iterChargingTimes = chargingTimes.toArray();
		Arrays.sort(iterChargingTimes);

		// the charging times is ordered after the time and not in the order,
		// the charging actually happned (starting after first activity)
		int firstIndex = 0;

		// the length of iterChargingTimes can be zero, if charging of vehicle
		// was not possible and it needed to drive on gazoline (or if electric
		// vehicle, then quite bad for it...)
		while (iterChargingTimes.length > 0
				&& curConsumptionLog.getEnterTime() > ((ChargeLog) iterChargingTimes[firstIndex]).getStartChargingTime()) {
			firstIndex++;

			if (firstIndex == iterChargingTimes.length) {
				// no charging before mid night.
				// this will be handeled by the second loop.
				break;
			}
		}

		// process first the charging / consumption from first activity to
		// midnight
		for (int i = firstIndex; i < iterChargingTimes.length; i++) {
			ChargeLog curChargeLog = (ChargeLog) iterChargingTimes[i];

			// we must check if curConsumptionLog is not null, because if the
			// agent only charges at home
			// in the evening, then all energy consumption happens before
			// that...
			while (curConsumptionLog != null && curConsumptionLog.getEnterTime() < curChargeLog.getEndChargingTime()) {
				startSOC -= curConsumptionLog.getEnergyConsumption();
				curConsumptionLog = consumptionLog.poll();
			}

			curChargeLog.updateSOC(startSOC);
			startSOC = curChargeLog.getEndSOC();
		}

		// process all charging / consumption after mid night
		for (int i = 0; i < firstIndex; i++) {
			ChargeLog curChargeLog = (ChargeLog) iterChargingTimes[i];

			// we must check if curConsumptionLog is not null, because if the
			// agent only charges at home
			while (curConsumptionLog != null) {
				startSOC -= curConsumptionLog.getEnergyConsumption();
				curConsumptionLog = consumptionLog.poll();
			}

			curChargeLog.updateSOC(startSOC);
			startSOC = curChargeLog.getEndSOC();
		}

	}

	/**
	 * This writes out charging events. TODO: add columns at the end for
	 * SOC_start and SOC_end Dependencies: Of course the link ids etc. must be
	 * available for using this...
	 * 
	 * @param chargingTimes
	 * @param outputFilePath
	 */
	public static void writeChargingTimes(HashMap<Id, ChargingTimes> chargingTimes, String outputFilePath) {

		ArrayList<String> list = new ArrayList<String>();
//TODO: use string buffer...
		list.add("linkId\tagentId\tstartChargingTime\tendChargingTime\tstartSOC\tendSOC");

		for (Id personId : chargingTimes.keySet()) {
			ChargingTimes curChargingTime = chargingTimes.get(personId);
			String line = "";

			for (ChargeLog chargeLog : curChargingTime.getChargingTimes()) {
				line = "";
				line += chargeLog.getLinkId().toString() + "\t";
				line += personId.toString() + "\t";
				line += chargeLog.getStartChargingTime() + "\t";
				line += chargeLog.getEndChargingTime() + "\t";
				line += chargeLog.getStartSOC() + "\t";
				line += chargeLog.getEndSOC();
				list.add(line);
			}
		}

		GeneralLib.writeList(list, outputFilePath);
	}

	public static void writeVehicleEnergyConsumptionStatisticsGraphic(String fileName, double[][] energyUsageStatistics) {
		writeEnergyConsumptionGraphic(fileName, energyUsageStatistics, "Vehicle Energy Consumption");
	}


	
	public static void writeEnergyConsumptionGraphic(String fileName, double[][] energyUsageStatistics, String title) {
		GeneralLib.writeHubGraphic(fileName, GeneralLib.scaleMatrix(energyUsageStatistics, 1.0/3600000) ,title, "Time of Day [s]","Energy Consumption [kWh]");
	}

	// write out data (energy usage at each hub)
	public static void writeEnergyUsageStatisticsData(String fileName, double[][] energyUsageStatistics) {
		PSFGeneralLib.writeEnergyUsageStatisticsData(fileName, energyUsageStatistics);
	}

	public static void printEnergyUsageStatistics(HashMap<Id, ChargingTimes> chargingTimes, HubLinkMapping hubLinkMapping) {
		double[][] energyUsageStatistics = getEnergyUsageStatistics(chargingTimes, hubLinkMapping);

		// write header
		System.out.print("time");
		for (int j = 0; j < hubLinkMapping.getNumberOfHubs(); j++) {
			System.out.print("\tHub");
			System.out.print(j);
		}
		System.out.println();

		// write data
		for (int i = 0; i < numberOfTimeBins; i++) {
			System.out.print(i * 900);
			for (int j = 0; j < hubLinkMapping.getNumberOfHubs(); j++) {
				System.out.print("\t");
				System.out.print(energyUsageStatistics[i][j]);
			}
			System.out.println();
		}

	}

	/**
	 * find out how much energy was used in which time slot at which hub format:
	 * the first index of the result array is the slotIndex (15 min bins), the
	 * second index is the hub id
	 * 
	 * TODO: write tests for it!
	 */
	public static double[][] getEnergyUsageStatistics(HashMap<Id, ChargingTimes> chargingTimes, HubLinkMapping hubLinkMapping) {
		double[][] energyUsageStatistics = new double[numberOfTimeBins][hubLinkMapping.getNumberOfHubs()];

		for (Id personId : chargingTimes.keySet()) {
			ChargingTimes curChargingTime = chargingTimes.get(personId);

			for (ChargeLog chargeLog : curChargingTime.getChargingTimes()) {
				int slotIndex = Math.round((float) Math.floor(chargeLog.getStartChargingTime() / 900));

				if (slotIndex > numberOfTimeBins) {
					System.out.println();
				}

				// doing module 96, just in case there are more than 24 hours in
				// the day...
				energyUsageStatistics[slotIndex % 96][hubLinkMapping.getHubNumber(chargeLog.getLinkId().toString())] += chargeLog
						.getEnergyCharged();
			}
		}
		return energyUsageStatistics;
	}

}
