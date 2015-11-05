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
 * Class to return the months that were not useful for the Grain industry 
 * project. That is, all months for which we have longitudinal data, except
 * March 2013 to February 2014.
 * 
 * @author jwjoubert
 */
public class OtherMonths {
	
	/**
	 * Returns all the months in the longitudinal data set except the twelve 
	 * months of March 2013 to February 2014.
	 * @return
	 */
	public static String[] getMonths(){
		String[] months = {
				"201001", "201002", "201003", "201004",
				"201005", "201006", "201007", "201008",
				"201009", "201010", "201011", "201012",
				"201101", "201102", "201103", "201104",
				"201105", "201106", "201107", "201108",
				"201109", "201110", "201111", "201112",
				"201201", "201202", "201203", "201204",
				"201205", "201206", "201207", "201208",
				"201209", "201210", "201211", "201212",
				"201301", "201302", 
//				"201303", "201304",
//				"201305", "201306", "201307", "201308",
//				"201309", "201310", "201311", "201312",
//				"201401", "201402", 
				"201403", "201404",
				"201405"
		};
		return months;
	}

}
