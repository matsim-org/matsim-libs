package playground.wrashid.PSF2.tools.chargingLog.timeLine;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class EnergyConsumptionAtAllLinksTimeLine {

	static int timeBinSizeInSeconds=900;
	
	public static void main(String[] args) {
		String chargingLogFileNamePath = "c:/tmp/chargingLog.txt";
		//String chargingLogFileNamePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run6/ITERS/it.0/0.chargingLog.txt";
		HashMap<Id<Link>, double[]> energyConsumptionPerLink = readChargingLog(chargingLogFileNamePath);
		
		printEnergyConsumptionAtAllLinksDuringTheDay(energyConsumptionPerLink);
	}

	private static void printEnergyConsumptionAtAllLinksDuringTheDay(HashMap<Id<Link>, double[]> energyConsumptionPerLink) {
		System.out.println("energyConsumptionAtAllLinksDuringTheDay");
		double[] sumOfAllChargingsAtAllLinksDuringTheDay = getSumOfAllChargingsAtAllLinksDuringTheDay(energyConsumptionPerLink);
		
		for (int i=0;i<getNumberOfSlotsInBin();i++){
			System.out.println(i + "\t" + sumOfAllChargingsAtAllLinksDuringTheDay[i]);
		}
	}
	
	public static double[] getSumOfAllChargingsAtAllLinksDuringTheDay(HashMap<Id<Link>, double[]> energyConsumptionPerLink){
		double[] result=getNewTimeBinArray() ;
		
		for (Id<Link> linkId:energyConsumptionPerLink.keySet()){
			for (int i=0;i<getNumberOfSlotsInBin();i++){
				result[i]+=energyConsumptionPerLink.get(linkId)[i];
			}
		}
		return result;
	}
	
	

	public static HashMap<Id<Link>,double[]> readChargingLog(String chargingLogFileNamePath) {
		Matrix matrix=GeneralLib.readStringMatrix(chargingLogFileNamePath);
		HashMap<Id<Link>,double[]> energyConsumptionPerLinkDuringTheDay=new HashMap<>();
		
		// starting with index 1 (ignoring first line)
		for (int i=1;i<matrix.getNumberOfRows();i++){
			Id<Link> linkId=Id.create(matrix.getString(i, 0), Link.class);
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
	
	private static void initializeEnergyConsumptionPerLink(HashMap<Id<Link>, double[]> energyConsumptionPerLinkDuringTheDay,
			Id<Link> linkId) {
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
