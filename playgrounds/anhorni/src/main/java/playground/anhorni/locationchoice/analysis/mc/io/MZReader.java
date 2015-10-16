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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;

public class MZReader {
	private List<MZTrip> mzTrips = new Vector<MZTrip>();

	public MZReader(List<MZTrip> mzTrips) {
		this.mzTrips = mzTrips;
	}
	
	public List<MZTrip> read(String file) {
						
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				//String tripNr = entries[3].trim();
				//Id id = Id.create(HHNR + ZIELPNR + tripNr);
				Id<Person> personId = Id.create(HHNR+ZIELPNR, Person.class);
				
				// filter inplausible persons
				// 6177302: unbezahlte Arbeit
				//if (personId.compareTo(Id.create("6177302")) == 0) continue; 

				Coord coordEnd = new Coord(Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));

				Coord coordStart = new Coord(Double.parseDouble(entries[18].trim()), Double.parseDouble(entries[19].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}	
				MZTrip mzTrip = new MZTrip(personId, coordStart, coordEnd, startTime, endTime);

				Coord coordHome = new Coord(Double.parseDouble(entries[6].trim()), Double.parseDouble(entries[7].trim()));
				
				mzTrip.setHome(coordHome);

				mzTrip.setWmittel(entries[52].trim());
				mzTrip.setWzweck2(entries[54].trim());
			
				if (entries[55].trim().equals("4")) {
					mzTrip.setPurposeCode(entries[45].trim());
					mzTrip.setPurpose("shop");
				}
				else if (entries[55].trim().equals("8")) {
					mzTrip.setPurposeCode(entries[44].trim());
					mzTrip.setPurpose("leisure");
				}
				else if (entries[55].trim().equals("2")) {
					mzTrip.setPurpose("work");
					if (mzTrip.getWzweck2().equals("1")) {
						mzTrip.setPurposeCode("1");
					}
					else {
						mzTrip.setPurposeCode("-99");
					}
				}
				// education
				else if (entries[55].trim().equals("3")) {
					mzTrip.setPurpose("education");
					if (mzTrip.getWzweck2().equals("1")) {
						mzTrip.setPurposeCode("1");
					}
					else {
						mzTrip.setPurposeCode("-99");
					}
				}
				else {
					mzTrip.setPurposeCode(entries[55].trim());
					mzTrip.setPurpose("null");	
				}
				this.mzTrips.add(mzTrip);	
			}
		} catch (IOException e) {
				throw new RuntimeException(e);
		}
		return this.mzTrips;
	}
	
	public List<MZTrip> getMzTrips() {
		return mzTrips;
	}
	public void setMzTrips(List<MZTrip> mzTrips) {
		this.mzTrips = mzTrips;
	}
}
