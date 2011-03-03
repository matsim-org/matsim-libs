package playground.wrashid.PSF2.tools.chargingLog.sum;

import java.util.LinkedList;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

public class GetLinksWithHigherEnergyConsumptionThanThreshold {

	public static void main(String[] args) {
		String chargingLogFileNamePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run5/ITERS/it.0/0.chargingLog.txt";
		DoubleValueHashMap<String> energyConsumptionPerLink = EnergyConsumptionPerLink.readChargingLog(chargingLogFileNamePath);
		
		LinkedList<String> energyConsumptionHigherThresholdValue = energyConsumptionPerLink.getKeysWithHigherValueThanThresholdValue(2*100000000);
		System.out.println("number of links with energy consumption higher than threshold: " + energyConsumptionHigherThresholdValue.size());
		GeneralLib.printLinkedListToConsole(energyConsumptionHigherThresholdValue);
		
	}
	
}
