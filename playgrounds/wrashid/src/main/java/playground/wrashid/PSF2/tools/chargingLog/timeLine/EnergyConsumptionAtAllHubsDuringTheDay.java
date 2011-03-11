package playground.wrashid.PSF2.tools.chargingLog.timeLine;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.wrashid.artemis.hubs.LinkHubMapping;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;

public class EnergyConsumptionAtAllHubsDuringTheDay {

	public static void main(String[] args) {
		String chargingLogFileNamePath = "H:/data/experiments/ARTEMIS/zh/dumb charging/output/run6/ITERS/it.0/0.chargingLog.txt";
		String linkHubMappingTable = "H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
		
		HashMap<Id, double[]> energyConsumptionPerLink = EnergyConsumptionAtAllLinksTimeLine.readChargingLog(chargingLogFileNamePath);
		
		LinkHubMapping linkHubMapping=new LinkHubMapping(linkHubMappingTable);
		
		double[] energyConsumptionForAllHubsDuringTheDay = getEnergyConsumptionForAllHubsDuringTheDay(energyConsumptionPerLink,linkHubMapping);
		
		printEnergyConsumptionAtAllHubsDuringTheDay(energyConsumptionForAllHubsDuringTheDay);
	}
	
	private static void printEnergyConsumptionAtAllHubsDuringTheDay(double[] energyConsumptionForAllHubsDuringTheDay) {
		System.out.println("energyConsumptionAtAllHubsDuringTheDay");
		
		for (int i=0;i<EnergyConsumptionAtAllLinksTimeLine.getNumberOfSlotsInBin();i++){
			System.out.println(i + "\t" + energyConsumptionForAllHubsDuringTheDay[i]);
		}		
	}

	private static double[] getEnergyConsumptionForAllHubsDuringTheDay(HashMap<Id, double[]> energyConsumptionPerLink, LinkHubMapping linkHubMapping){
		double[] result=EnergyConsumptionAtAllLinksTimeLine.getNewTimeBinArray();
		
		for (Id linkId:energyConsumptionPerLink.keySet()){
			if (linkHubMapping.getHubIdForLinkId(linkId)!=null){
			for (int i=0;i<EnergyConsumptionAtAllLinksTimeLine.getNumberOfSlotsInBin();i++){
					result[i]+=energyConsumptionPerLink.get(linkId)[i];
				}
			}
		}
		
		return result;
	}
	
}
