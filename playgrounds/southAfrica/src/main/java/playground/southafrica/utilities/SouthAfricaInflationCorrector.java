/* *********************************************************************** *
 * project: org.matsim.*
 * SouthAfricaInflationCorrector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Class to convert monetary values using the Consumer Price Index (CPI)
 * using the average year-on-year rates as provided by Statistics South 
 * Africa. Source: <a href=http://beta2.statssa.gov.za/?page_id=1854&PPN=P0141>CPI History - 1960 Onwards</a>
 * @author jwjoubert
 */
public class SouthAfricaInflationCorrector {
	private final static Logger LOG = Logger.getLogger(SouthAfricaInflationCorrector.class);
	private static Map<Integer, Double> inflationMap;

	private static void populateInflationMap(){
		inflationMap = new TreeMap<Integer, Double>();
		inflationMap.put(1981, 0.152);
		inflationMap.put(1982, 0.147);
		inflationMap.put(1983, 0.124);
		inflationMap.put(1984, 0.116);
		inflationMap.put(1985, 0.161);
		inflationMap.put(1986, 0.187);
		inflationMap.put(1987, 0.161);
		inflationMap.put(1988, 0.129);
		inflationMap.put(1989, 0.147);
		inflationMap.put(1990, 0.144);
		inflationMap.put(1991, 0.153);
		inflationMap.put(1992, 0.139);
		inflationMap.put(1993, 0.097);
		inflationMap.put(1994, 0.090);
		inflationMap.put(1995, 0.087);
		inflationMap.put(1996, 0.074);
		inflationMap.put(1997, 0.086);
		inflationMap.put(1998, 0.069);
		inflationMap.put(1999, 0.051);
		inflationMap.put(2000, 0.053);
		inflationMap.put(2001, 0.057);
		inflationMap.put(2002, 0.092);
		inflationMap.put(2003, 0.058);
		inflationMap.put(2004, 0.014);
		inflationMap.put(2005, 0.034);
		inflationMap.put(2006, 0.047);
		inflationMap.put(2007, 0.071);
		inflationMap.put(2008, 0.115);
		inflationMap.put(2009, 0.071);
		inflationMap.put(2010, 0.043);
		inflationMap.put(2011, 0.050);
		inflationMap.put(2012, 0.056);
		inflationMap.put(2013, 0.057);
	}
	
	/**
	 * Corrects a given value for (South African) inflation.
	 * @param value the currency value to correct;
	 * @param fromYear the base year of the given value; 
	 * @param toYear the year to which the value must be converted/corrected for inflation.
	 * @return
	 */
	public static double convert(double value, int fromYear, int toYear){
		populateInflationMap();
		if(!inflationMap.containsKey(fromYear)){
			throw new IllegalArgumentException("There is no year " + fromYear + " in the inflation map.");
		}
		if(!inflationMap.containsKey(toYear)){
			throw new IllegalArgumentException("There is no year " + toYear + " in the inflation map.");
		}
		double factor = 1.0;
		if(fromYear == toYear){
			/* Return the default 1.0 */
		} else if(fromYear < toYear){
			for(int year = fromYear+1; year <= toYear; year++){
				if(!inflationMap.containsKey(year)){
					throw new IllegalArgumentException("Did not find an inflation rate for " + year);
				}
				factor *= (1 + inflationMap.get(year));
			}
		} else{
			for(int year = fromYear; year >= toYear+1; year--){
				if(!inflationMap.containsKey(year)){
					throw new IllegalArgumentException("Did not find an inflation rate for " + year);
				}
				factor /= (1 + inflationMap.get(year));
			}
		}
		return value*factor;
	}
	
}

