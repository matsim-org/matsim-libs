package playground.wrashid.mz.parking;

import org.apache.log4j.chainsaw.Main;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class GetParkingInformation {

	public static void main(String[] args) {
		Matrix table = GeneralLib.readStringMatrix("H:/data/static/MZ2005/Zielpersonen.txt");
		
		System.out.println(table.getColumnIndex("F22A"));
		
	}
	
	private static int getColumnIndexOfPersonWeight(Matrix table){
		return table.getColumnIndex("WP");
	}
	
	private static int getColumnIndexOfWorkingLocationXCoordinate(Matrix table){
		return table.getColumnIndex("W_X");
	}
	
	private static int getColumnIndexOfWorkingLocationYCoordinate(Matrix table){
		return table.getColumnIndex("W_Y");
	}
	
	private static int getColumnIndexOfParkingAvailabilityAtWorkingLocation(Matrix table){
		return table.getColumnIndex("F411");
	}
	
	// percentage free, yes, no (F411)
	// if parking available and costs, what is average cost (F411A)
}
