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

package playground.anhorni.choiceSetGeneration.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.Line;
import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.locationchoice.analysis.mc.MZTrip;

/*	  0			1		2			3			4			5		6		7			8				9
 * ---------------------------------------------------------------------------------------------------------		
 *0	| ...
 * ---------------------------------------------------------------------------------------------------------
 */


public class NelsonTripReader {
	
	private List<ChoiceSet> choiceSets;
	private final static Logger log = Logger.getLogger(NelsonTripReader.class);
	private TreeMap<Id<Person>, MZTrip> mzTrips = null; 
	private final NetworkImpl network;
	private final ZHFacilities facilities;
		
	public NelsonTripReader(NetworkImpl network, ZHFacilities facilities) {
		this.network = network;
		this.facilities = facilities;
	}
	
	public List<ChoiceSet> readFiles(final String file0, final String file1, String mode)  {
		this.mzTrips = new TreeMap<>();
		this.choiceSets = new Vector<ChoiceSet>();
		
		read0(file0, mode);
		read1(file1, mode);
		log.info("Number of " + mode + " trips : " + this.choiceSets.size());
		return this.choiceSets;
	}
				
	// read 810Trips
	private void read1(final String file, String mode) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {	
				String[] entries = curr_line.split("\t", -1);
				
				Line line = new Line();
				if (!line.catchLine(entries)) break;
								
				/*
				String ausmittel = entries[74].trim();
				boolean walk = wmittel.equals("15") && ausmittel.equals("10") && mode.equals("walk") ;
				boolean car = wmittel.equals("9") && ausmittel.equals("6") && mode.equals("car");
				*/
				
				boolean walk = line.getWmittel().equals("15") && mode.equals("walk") ;
				boolean car = line.getWmittel().equals("9")  && mode.equals("car");
				
				if (!(walk || car )) continue;
								
				// get the after shopping trip:	
				MZTrip mzTrip = this.mzTrips.get(Id.create(line.getNextTrip(), Person.class));
				
				// mode change: e.g. auto -> bike 43179022
				if (mzTrip == null) {
					continue;
				}
				
				line.constructTrip(entries, network, this.facilities, mzTrip);
				ChoiceSet choiceSet = new ChoiceSet(Id.create(line.getTripId(), ChoiceSet.class), line.getTrip(), line.getChosenFacilityId());
				choiceSet.setPersonAttributes(line.getPersonAttributes());
				choiceSet.setTravelTimeBudget(line.getTravelTimeBudget());
				
				// filter trips 55534012 and 56751011 (fehlende Geodaten)
				if (!(line.getTripId().equals("55534012") || line.getTripId().equals("56751011"))) {
					this.choiceSets.add(choiceSet);		
				}
			}
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	// add F58, F514 for after shopping act (E_X and E_Y)
	private void read0(String file, String mode) {
				
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);
				
				String m = entries[53].trim();
				boolean walk = m.equals("15") && mode.equals("walk");
				boolean car = m.equals("9") && mode.equals("car");				
				if (!(walk || car )) continue;
								
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				String tripNr = entries[3].trim();
				
				Id<Person> id = Id.create(HHNR + ZIELPNR + tripNr, Person.class);

				Coord coord = new Coord(Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}
					
				MZTrip mzTrip = new MZTrip(id, null, coord, startTime, endTime);
				this.mzTrips.put(id, mzTrip);
			}
			
			bufferedReader.close();
		} catch (IOException e) {
				throw new RuntimeException(e);
		}
	}

}
