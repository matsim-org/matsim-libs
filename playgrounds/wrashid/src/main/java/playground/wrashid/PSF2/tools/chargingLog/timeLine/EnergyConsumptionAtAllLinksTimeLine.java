package playground.wrashid.PSF2.tools.chargingLog.timeLine;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class EnergyConsumptionAtAllLinksTimeLine {

	static int timeBinSizeInSeconds=900;
	
	public static void main(String[] args) {
		String chargingLogFileNamePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run6/ITERS/it.0/0.chargingLog.txt";
		HashMap<Id, double[]> energyConsumptionPerLink = readChargingLog(chargingLogFileNamePath);
		
		printEnergyConsumptionAtAllLinksDuringTheDay(energyConsumptionPerLink);
	}

	private static void printEnergyConsumptionAtAllLinksDuringTheDay(HashMap<Id, double[]> energyConsumptionPerLink) {
		System.out.println("energyConsumptionAtAllLinksDuringTheDay");
		double[] sumOfAllChargingsAtAllLinksDuringTheDay = getSumOfAllChargingsAtAllLinksDuringTheDay(energyConsumptionPerLink);
		
		for (int i=0;i<getNumberOfSlotsInBin();i++){
			System.out.println(i + "\t" + sumOfAllChargingsAtAllLinksDuringTheDay[i]);
		}
	}
	
	public static double[] getSumOfAllChargingsAtAllLinksDuringTheDay(HashMap<Id, double[]> energyConsumptionPerLink){
		double[] result=getNewTimeBinArray() ;
		
		for (Id linkId:energyConsumptionPerLink.keySet()){
			for (int i=0;i<getNumberOfSlotsInBin();i++){
				result[i]+=energyConsumptionPerLink.get(linkId)[i];
			}
		}
		return result;
	}
	
	

	public static HashMap<Id,double[]> readChargingLog(String chargingLogFileNamePath) {
		StringMatrix matrix=GeneralLib.readStringMatrix(chargingLogFileNamePath);
		HashMap<Id,double[]> energyConsumptionPerLinkDuringTheDay=new HashMap<Id, double[]>();
		
		// starting with index 1 (ignoring first line)
		for (int i=1;i<matrix.getNumberOfRows();i++){
			Id linkId=new IdImpl(matrix.getString(i, 0));
			Double startChargingTime=matrix.getDouble(i, 2);
			Double endChargingTime=matrix.getDouble(i, 3);
			Double startSOC=matrix.getDouble(i, 4);
			Double endSOC=matrix.getDouble(i, 5);
			double energyCharged=endSOC-startSOC;
			double timeDelta=GeneralLib.getIntervalDuration(startChargingTime, endChargingTime);
			
			DebugLib.assertTrue(energyCharged>0, "startSOC:" + startSOC + " - endSOC:" + endSOC);
			
			initializeEnergyConsumptionPerLink(energyConsumptionPerLinkDuringTheDay, linkId);
			
			double[] energyConsumptionAtLink = energyConsumptionPerLinkDuringTheDay.get(linkId);
			updateEnergyConsumptionWithRoundingErrorsAtBorders(startChargingTime,endChargingTime,energyConsumptionAtLink,energyCharged/timeDelta*timeBinSizeInSeconds);
			
		}
		return energyConsumptionPerLinkDuringTheDay;
	}

	//TODO: test this
	public static void updateEnergyConsumptionWithRoundingErrorsAtBorders(double startTime, double endTime, double[] energyConsumptionAtLink, double chargePerTimeBin) {
		int startBinIndex = getBinIndex(startTime);
		int endBinIndex = getBinIndex(endTime);

		if (startBinIndex > endBinIndex) {

			for (int i = startBinIndex; i < 96; i++) {
				energyConsumptionAtLink[i]+=chargePerTimeBin;
			}

			for (int i = 0; i <= endBinIndex; i++) {
				energyConsumptionAtLink[i]+=chargePerTimeBin;
			}

		} else {
			for (int i = startBinIndex; i <= endBinIndex; i++) {
				energyConsumptionAtLink[i]+=chargePerTimeBin;
			}
		}
	}
	
	public static int getBinIndex(double time) {
		return GeneralLib.getTimeBinIndex(time,timeBinSizeInSeconds);
	}
	
	private static void initializeEnergyConsumptionPerLink(HashMap<Id, double[]> energyConsumptionPerLinkDuringTheDay,
			Id linkId) {
		if (!energyConsumptionPerLinkDuringTheDay.containsKey(linkId)){
			energyConsumptionPerLinkDuringTheDay.put(linkId, getNewTimeBinArray());
		}
	}

	public static double[] getNewTimeBinArray() {
		return new double[getNumberOfSlotsInBin()];
	}

	public static int getNumberOfSlotsInBin() {
		return 24*3600/timeBinSizeInSeconds;
	}
	
}
