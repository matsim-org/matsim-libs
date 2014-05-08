package playground.wrashid.artemis.lav;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class LAVLib {

	/**
	 * TODO: write test for this. 
	 * 
	 * @param fileName
	 * @param ignoreFirstLine
	 * @return
	 */
	public static Matrix readLAVModelFile(String fileName, boolean ignoreFirstLine){
		LinkedList<String> fileRows = GeneralLib.readFileRows(fileName);
		Matrix stringMatrix=new Matrix();
		
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
					ArrayList<String> rowList = GeneralLib.convertStringArrayToArrayList(columns);
					stringMatrix.addRow(rowList);
				}
			} else {
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
		}
		
		if (stringMatrix.getNumberOfRows() ==0){
			DebugLib.stopSystemAndReportInconsistency("maxNumberOfTabs:"+ maxNumberOfTabs);
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
