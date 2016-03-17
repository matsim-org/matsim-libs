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

package playground.dziemke.analysis;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 * This class parses a cemdap person file and creates (Person) ObjectAttributes
 * contain the relevant information
 */
public class CemdapPersonInputFileReader {
	private final static Logger log = Logger.getLogger(CemdapPersonInputFileReader.class);
	private ObjectAttributes personAttributes = new ObjectAttributes();
	
	
	public CemdapPersonInputFileReader() {
	}
	
	
	public final void parse(String cemdapPersonFile) {
		int lineCount = 0;
		
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapPersonFile);
			
			String currentLine = null;
						
			// read data and write ...
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 100000 == 0) {
					log.info(lineCount+ " lines read in so far.");
					Gbl.printMemoryUsage();
				}
								
				String householdAndPersonId = entries[0] + "_" + entries[1];
				Integer employed = new Integer(entries[2]);
				Integer student = new Integer(entries[3]);
				Integer driversLicence = new Integer(entries[4]);
				//locationOfWork 5
				//locationOfSchool 6
				Integer sex = new Integer(entries[7]); // 1 if female
				Integer age = new Integer(entries[8]);
				//parent 9

			
				if (age >= 0) {
					personAttributes.putAttribute(householdAndPersonId, "age", age);
				} else {
					throw new RuntimeException("Variable for age may not be smaller than 0.");
				}
				
				if (sex == 0 || sex == 1) {
					personAttributes.putAttribute(householdAndPersonId, "sex", sex);
				} else {
					throw new RuntimeException("Variable for sex may only be 0 or 1.");
				}
				
				if (employed == 1) {
					personAttributes.putAttribute(householdAndPersonId, "employed", 1);
				} else if (employed == 0){
					personAttributes.putAttribute(householdAndPersonId, "employed", 0);
				} else {
					throw new RuntimeException("Variable for employment may only be 0 or 1.");
				}
				
				if (student == 1) {
					personAttributes.putAttribute(householdAndPersonId, "student", 1);
				} else if (student == 0) {
					personAttributes.putAttribute(householdAndPersonId, "student", 0);
				} else {
					throw new RuntimeException("Variable for being student may only be 0 or 1.");
				}
				
				if (driversLicence == 1) {
					personAttributes.putAttribute(householdAndPersonId, "driversLicence", 1);
				} else if (driversLicence == 0) {
					personAttributes.putAttribute(householdAndPersonId, "driversLicence", 0);
				} else {
					throw new RuntimeException("Variable for driver's licence may only be 0 or 1.");
				}
			}
		} catch (IOException e) {
			log.error(new Exception(e));
		}
	}
	
	public ObjectAttributes getPersonAttributes() {
		return this.personAttributes;
	}
}