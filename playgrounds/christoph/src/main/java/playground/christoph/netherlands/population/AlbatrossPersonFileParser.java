/* *********************************************************************** *
 * project: org.matsim.*
 * AlbatrossPersonFileParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.netherlands.population;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/*
 * Some details about the data format:
 * The population is described as a set of households. It only contains
 * adult people. Therefore each household consists of one or two persons.
 * 
 * Each line in the text-file describes one activity of a household member.
 * 
 * The day starts at 03:00 (coded as 300) and ends at 03:00 of the following
 * day (coded as 2700).
 * 
 * The Ids of the created population will be coded as HouseholdId + A/B.
 */
public class AlbatrossPersonFileParser {

	private static final Logger log = Logger.getLogger(AlbatrossPersonFileParser.class);
	
	private String inFile;
	private String separator = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	public AlbatrossPersonFileParser(String inFile) {
		this.inFile = inFile;
	}
	
	public Map<String, AlbatrossPerson> readFile() {
		Map<Integer, List<String[]>> households = new TreeMap<Integer, List<String[]>>();
		Map<String, AlbatrossPerson> persons = new TreeMap<String, AlbatrossPerson>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	    
    	try {
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			br.readLine();
			
			/*
			 * First we parse the whole file and create a map entry for each
			 * household, which consists of multiple row describing up to two
			 * persons.
			 */
			String line;
			while((line = br.readLine()) != null) { 
				String[] cols = line.split(separator);
				
				int householdId = parseInteger(cols[0]);
				List<String[]> household = households.get(householdId);
				
				if (household == null) {
					household = new ArrayList<String[]>();
					households.put(householdId, household);
				}
				household.add(cols);
			}
			
			/*
			 * Next we create the persons from the households.
			 */
			for (List<String[]> household : households.values()) {
				
				AlbatrossPerson albatrossPersonA = getNewPerson();
				AlbatrossPerson albatrossPersonB = null;
				AlbatrossPerson albatrossPerson = albatrossPersonA;
				
				int rowCount = 1;
				for (String[] cols : household) {
										
					int actNr = parseInteger(cols[10]);
					if (actNr == 1) {
						
						/*
						 * If the ActivityIndex is 1 but the rowCount is > 1 then
						 * the description of the second person start.
						 */
						if (rowCount > 1) {
							albatrossPersonB = getNewPerson();
							albatrossPerson = albatrossPersonB;
						}
					
						albatrossPerson.HHID = parseInteger(cols[0]);
						albatrossPerson.DAY = parseInteger(cols[1]);
						albatrossPerson.COMP = parseInteger(cols[2]);
						albatrossPerson.SEC = parseInteger(cols[3]);
						albatrossPerson.AGE = parseInteger(cols[4]);
						albatrossPerson.CHILD = parseInteger(cols[5]);
						albatrossPerson.NCARS = parseInteger(cols[6]);
						albatrossPerson.GEND = parseInteger(cols[7]);
						albatrossPerson.WSTAT = parseInteger(cols[8]);
						albatrossPerson.AGEP = parseInteger(cols[9]);						
					}
														
					albatrossPerson.ACTNR.add(parseInteger(cols[10]));
					albatrossPerson.ATYPE.add(parseInteger(cols[11]));
					if (cols[12] == "") cols[12] = "-1";	// not in use...
					albatrossPerson.WITH.add(parseInteger(cols[12]));
					albatrossPerson.BT.add(parseInteger(cols[13]));
					albatrossPerson.ET.add(parseInteger(cols[14]));
					if (cols[15] == "") cols[15] = "-1";	// mode for the Trip to the activity - is undefined for first home activity
					albatrossPerson.HOME.add(parseInteger(cols[15]));
					albatrossPerson.PPC.add(parseInteger(cols[16]));
					albatrossPerson.MODE.add(parseInteger(cols[17]));
					albatrossPerson.TRAVTIME.add(parseInteger(cols[18]));
					
					rowCount++;
				}
				
				if (albatrossPersonA != null) persons.put(albatrossPersonA.HHID + "A", albatrossPersonA);
				if (albatrossPersonB != null) persons.put(albatrossPersonB.HHID + "B", albatrossPersonA);
			}
			
			br.close();
			isr.close();
			fis.close();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Parsed households: " + households.size());
		log.info("Parsed persons: " + persons.size());
		return persons;
	}
	
	private AlbatrossPerson getNewPerson() {
		AlbatrossPerson albatrossPerson = new AlbatrossPerson();
		
		albatrossPerson.ACTNR = new ArrayList<Integer>();
		albatrossPerson.ATYPE = new ArrayList<Integer>();
		albatrossPerson.WITH = new ArrayList<Integer>();
		albatrossPerson.BT = new ArrayList<Integer>();
		albatrossPerson.ET = new ArrayList<Integer>();
		albatrossPerson.HOME = new ArrayList<Integer>();
		albatrossPerson.PPC = new ArrayList<Integer>();
		albatrossPerson.MODE = new ArrayList<Integer>();
		albatrossPerson.TRAVTIME = new ArrayList<Integer>();
		
		return albatrossPerson;
	}
	
	private int parseInteger(String string) {
		if (string == null) return 0;
		else if (string.trim().equals("")) return 0;
		else return Integer.valueOf(string);
	}
	
	private double parseDouble(String string) {
		if (string == null) return 0.0;
		else if (string.trim().equals("")) return 0.0;
		else return Double.valueOf(string);
	}
}
