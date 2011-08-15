package playground.wrashid.artemis.lav;

import java.util.HashMap;

import playground.wrashid.lib.obj.StringMatrix;

public class FleetCompositionReader {

	public static void main(String[] args) {
	
		String fleetCompositionFileName = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/data 11. Aug. 2011/scenarien als tabellen/2020_Basic";
		HashMap<VehicleTypeLAV, Integer> vehicleFleet = getVehicleFleet(fleetCompositionFileName);
		
		
		System.out.println(getTotalNumberOfVehicles(vehicleFleet));
	}
	
	
	public static int getTotalNumberOfVehicles(HashMap<VehicleTypeLAV,Integer> vehicleFleet){
		int totalNumberOfVehicles=0;
		for (int numberOfVehicles:vehicleFleet.values()){
			totalNumberOfVehicles+=numberOfVehicles;
		}
		return totalNumberOfVehicles;
	}
	
	public static HashMap<VehicleTypeLAV,Integer> getVehicleFleet(String fileName){
		HashMap<VehicleTypeLAV, Integer> hashMap=new HashMap<VehicleTypeLAV, Integer>();
		StringMatrix modelFile = LAVLib.readLAVModelFile(fileName, true);
		
		for (int i=0;i<modelFile.getNumberOfRows();i++){
			VehicleTypeLAV vehicleType=new VehicleTypeLAV();
			vehicleType.powerTrainClass=modelFile.getInteger(i, 0);
			vehicleType.fuelClass=modelFile.getInteger(i, 1);
			vehicleType.powerClass=modelFile.getInteger(i, 2);
			vehicleType.massClass=modelFile.getInteger(i, 3);
			int numberOfVehicles=modelFile.getInteger(i, 4);
			
			// need to only enter one type of PHEVs to avoid counting them twice
			if (!(vehicleType.powerTrainClass==LAVLib.getPHEVPowerTrainClass() && vehicleType.fuelClass==LAVLib.getGasolineFuelClass())){
				hashMap.put(vehicleType, numberOfVehicles);
			}
		}
		
		return hashMap;
	}
	
	
	
	
	
}
