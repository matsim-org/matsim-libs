package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.HashMap;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity.PrivateParkingsIndoorWriter_v0;

public class PrivatParking {

	public static void main(String[] args) {
		String inputDir="C:/data/My Dropbox/ETH/static data/parking/zürich city/Private Parkplätze/";
		
		printPrivateOutdoorParkingTotalCapacity(inputDir);
		
		printPrivatIndoorParkingTotalCapacitiesWithMainBuildingPurpose(inputDir);
		
	}

	private static void printPrivatIndoorParkingTotalCapacitiesWithMainBuildingPurpose(String inputDir) {
		HashMap<Integer, String> mainBuildingUsagePurpose = PrivateParkingsIndoorWriter_v0.getMainBuildingUsagePurpose();
		
		StringMatrix privateParkingIndoor = GeneralLib.readStringMatrix(inputDir+"PrivateParkingIndoor.TXT");
		
		
		DoubleValueHashMap<String> parkingCapacities=new DoubleValueHashMap<String>();
		for (int i=1;i<privateParkingIndoor.getNumberOfRows();i++){
			Integer egId = privateParkingIndoor.getInteger(i, 0);
			double x=privateParkingIndoor.getDouble(i, 1);
			double y=privateParkingIndoor.getDouble(i, 2);
			double capacity=privateParkingIndoor.getDouble(i, 3);
			
			if (Config.isInsideStudyArea(x,y)){
				parkingCapacities.increment(mainBuildingUsagePurpose.get(egId));
			}
		}
		
		System.out.println("indoor parkings (mit hauptnutzung des gebaeudes):");
		
		parkingCapacities.printToConsole();
		
	}

	private static void printPrivateOutdoorParkingTotalCapacity(String inputDir) {
		StringMatrix privateParkingOutdoor = GeneralLib.readStringMatrix(inputDir+"PrivateParkingOutdoor.txt");
		
		double totalNumberOfOutdoorPrivateParking=0;
		for (int i=1;i<privateParkingOutdoor.getNumberOfRows();i++){
			double x=privateParkingOutdoor.getDouble(i, 1);
			double y=privateParkingOutdoor.getDouble(i, 2);
			
			if (Config.isInsideStudyArea(x,y)){
				totalNumberOfOutdoorPrivateParking+=privateParkingOutdoor.getDouble(i, 3);
			}
		}
		
		System.out.println("total number of outdoor private parking in scenario area: " + totalNumberOfOutdoorPrivateParking);
	}
	
}
