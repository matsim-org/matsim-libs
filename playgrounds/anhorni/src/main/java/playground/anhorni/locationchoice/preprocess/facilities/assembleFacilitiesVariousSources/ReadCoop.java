/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class ReadCoop {
	
	private final static Logger log = Logger.getLogger(ReadCoop.class);
	
	public void completeWithCoop(String file, TreeMap<String, ZHFacilityComposed> zhfacilities) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
			int attributeAdded = 0;
			while ((curr_line = bufferedReader.readLine()) != null) {
				String[] entries = curr_line.split(";", -1);
				
				String PLZ = entries[10].trim();
				String streetAndNumber = entries[9].trim();			
				String [] addressParts = streetAndNumber.split(" ", -1);
				String lastElement = addressParts[addressParts.length -1];
				
				String street = "";
				if (lastElement.matches("\\d{1,7}") || lastElement.contains("-") || lastElement.contains("/") ||
						lastElement.endsWith("a") || lastElement.endsWith("A") || lastElement.contains("+") || 
						lastElement.endsWith("c") || lastElement.endsWith("b"))  {
					for (int i = 0; i < addressParts.length-1; i++) {
						street +=  addressParts[i].toUpperCase() + " ";
					}
				}
				else {
					street = streetAndNumber.toUpperCase();
				}
				String key = entries[7].trim()+ PLZ + street.trim();
				if (zhfacilities.get(key) != null && zhfacilities.get(key).getRetailerCategory().equals("Coop")) {
					zhfacilities.get(key).setShopType(entries[5].trim());
					log.info(key + " attribute added");
					attributeAdded++;
				}
			}
			log.info("Attributes added :" + attributeAdded);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
