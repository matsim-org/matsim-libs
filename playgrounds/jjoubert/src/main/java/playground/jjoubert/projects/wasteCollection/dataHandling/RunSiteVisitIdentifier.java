/* *********************************************************************** *
 * project: org.matsim.*
 * RunSiteVisitIdentifier.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
/**
 * 
 */
package playground.jjoubert.projects.wasteCollection.dataHandling;

/**
 * Class to run different instances of the {@link SiteVisitIdentifier} class.
 * 
 * @author jwjoubert
 */
public class RunSiteVisitIdentifier {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SiteVisitIdentifier.main(getOneDayArgumentsForMacMini());
//		SiteVisitIdentifier.main(getOneDayArgumentsForMacbook());
	}
	
	
	public static String[] getOneDayArgumentsForMacMini(){
		String[] result = {
				"/Users/jwjoubert/Documents/Projects/CapeTownWaste/data/waste.txt.gz",
				"1000000000",
				"/Users/jwjoubert/Documents/Projects/CapeTownWaste/wasteSiteCoordinates.csv",
				"/Users/jwjoubert/workspace/r-CapeTownWaste/data/wasteSiteVisits.csv",
		};
		
		return result;
	}

	public static String[] getOneDayArgumentsForMacbook(){
		String[] result = {
				"/Users/jwjoubert/Downloads/wasteSample.txt",
				"200000",
				"/Volumes/Nifty/workspace/coct-data/WasteSiteCoordinates.csv",
				"/Volumes/Nifty/workspace/coct-data/WasteSiteVisits.csv"
		};
		
		return result;
	}
	
}
