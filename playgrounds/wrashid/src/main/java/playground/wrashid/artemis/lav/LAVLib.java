package playground.wrashid.artemis.lav;

import java.util.LinkedList;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class LAVLib {

	/**
	 * TODO: write test for this. 
	 * 
	 * @param fileName
	 * @param ignoreFirstLine
	 * @return
	 */
	public static StringMatrix readLAVModelFile(String fileName, boolean ignoreFirstLine){
		LinkedList<String> fileRows = GeneralLib.readFileRows(fileName);
		StringMatrix stringMatrix=new StringMatrix();
		
		int maxNumberOfTabs=0;		
		for (String row:fileRows){
			int numberOfTabs = row.split("\t").length;
			if (numberOfTabs>maxNumberOfTabs){
				maxNumberOfTabs=numberOfTabs;
			}
		}
		
		for (String row:fileRows){
			String[] columns = row.split("\t");
			int numberOfTabs = columns.length;
			if (numberOfTabs==maxNumberOfTabs){
				if (ignoreFirstLine){
					ignoreFirstLine=false;
				} else {
					LinkedList<String> rowList = GeneralLib.convertStringArrayToList(columns);
					stringMatrix.addRow(rowList);
				}
			}
		}
		
		return stringMatrix;
	}
	
	public static int getPHEVPowerTrainClass(){
		return 3;
	}
	
	public static int getGasolineFuelClass(){
		return 1;
	}
	
	public static int getElectricityFuelClass(){
		return 5;
	}
	
	public static int getBatteryElectricPowerTrainClass(){
		return 4;
	}
}
