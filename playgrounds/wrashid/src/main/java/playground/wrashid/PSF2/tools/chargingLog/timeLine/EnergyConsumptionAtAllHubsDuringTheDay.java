package playground.wrashid.PSF2.tools.chargingLog.timeLine;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.wrashid.artemis.hubs.LinkHubMapping;

public class EnergyConsumptionAtAllHubsDuringTheDay {

	public static void main(String[] args) {
		String chargingLogFileNamePath = "c:/tmp/chargingLog.txt";
		//String chargingLogFileNamePath = "H:/data/experiments/ARTEMIS/zh/dumb charging/output/run7/ITERS/it.0/0.chargingLog.txt";
		
		String linkHubMappingTable = "H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
		
		HashMap<Id<Link>, double[]> energyConsumptionPerLink = EnergyConsumptionAtAllLinksTimeLine.readChargingLog(chargingLogFileNamePath);
		
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

	private static double[] getEnergyConsumptionForAllHubsDuringTheDay(HashMap<Id<Link>, double[]> energyConsumptionPerLink, LinkHubMapping linkHubMapping){
		double[] result=EnergyConsumptionAtAllLinksTimeLine.getNewTimeBinArray();
		
		for (Id<Link> linkId:energyConsumptionPerLink.keySet()){
			if (linkHubMapping.getHubIdForLinkId(linkId)!=null){
			for (int i=0;i<EnergyConsumptionAtAllLinksTimeLine.getNumberOfSlotsInBin();i++){
					result[i]+=energyConsumptionPerLink.get(linkId)[i];
				}
			}
		}
		
		return result;
	}
	
}
