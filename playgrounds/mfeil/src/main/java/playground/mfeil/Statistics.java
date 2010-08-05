package playground.mfeil;

import java.util.ArrayList;

/** 
 * Class that stores and makes available to ScheduleRecycling whether some agents do not hold
 * attributes that are required for agents' distance calculation.
 * @author Matthias Feil
 *
 */
public class Statistics {
	
	public static ArrayList<ArrayList<String>> list; 
	public static boolean prt = true;
	public static boolean noSexAssignment, noLicenseAssignment, noCarAvailAssignment, noEmploymentAssignment, noMunicipalityAssignment;
	
	public Statistics (){
		list = new ArrayList<ArrayList<String>>();
	}

}
