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

package playground.anhorni.locationchoice.analysis.mc.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.anhorni.locationchoice.analysis.mc.filters.DateFilter;

public class ZPReader {
	
	DateFilter filter = new DateFilter();
	List<Id> mzFilteredTargetPersons = new Vector<Id>();
	
	public List<Id> read(String file) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				Id<Person> personId = Id.create(HHNR+ZIELPNR, Person.class);
				
				String[] date = entries[2].trim().split("/", -1);
				String day = date[1].trim();
				String month = date[0].trim();
				String year = "2005";
				
				if (this.filter.passedFilter(year, month, day)) {
					this.mzFilteredTargetPersons.add(personId);
				}
			}
				
		} catch (IOException e) {
				throw new RuntimeException(e);
		}
		return this.mzFilteredTargetPersons;
	}
}
