/* *********************************************************************** *
 * project: org.matsim.*
 * GrainMonths.java                                                                        *
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
package playground.nmviljoen.grain;

/**
 * Class to return the months that are useful for the Grain industry project.
 * That is, March 2013 to February 2014.
 * 
 * @author jwjoubert
 */
public class GrainMonths {
	
	/**
	 * Returns the twelve months of March 2013 to February 2014.
	 * @return
	 */
	public static String[] getMonths(){
		String[] months = {
				"201303", "201304", "201305", "201306",
				"201307", "201308", "201309", "201310",
				"201311", "201312", "201401", "201402"
		};
		return months;
	}

}
