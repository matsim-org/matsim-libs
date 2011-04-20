package playground.wrashid.mz.parking;

import org.apache.log4j.chainsaw.Main;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class GetParkingInformation {

	public static void main(String[] args) {
		StringMatrix table = GeneralLib.readStringMatrix("H:/data/static/MZ2005/Zielpersonen.txt");
		
		System.out.println(table.getColumnIndex("F22A"));
		
	}
	
	private static int getColumnIndexOfPersonWeight(StringMatrix table){
		return table.getColumnIndex("WP");
	}
	
	private static int getColumnIndexOfWorkingLocationXCoordinate(StringMatrix table){
		return table.getColumnIndex("W_X");
	}
	
	private static int getColumnIndexOfWorkingLocationYCoordinate(StringMatrix table){
		return table.getColumnIndex("W_Y");
	}
	
	private static int getColumnIndexOfParkingAvailabilityAtWorkingLocation(StringMatrix table){
		return table.getColumnIndex("F411");
	}
	
	// percentage free, yes, no (F411)
	// if parking available and costs, what is average cost (F411A)
}
