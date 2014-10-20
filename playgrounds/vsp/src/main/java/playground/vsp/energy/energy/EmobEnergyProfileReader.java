/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.vsp.energy.energy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class EmobEnergyProfileReader {
	private static final Logger log = Logger
			.getLogger(EmobEnergyProfileReader.class);
	
	public static ChargingProfiles readChargingProfiles(String file){
		ChargingProfiles p = new ChargingProfiles();
		Set<String[]> values = readFileContent(file, "\t", true);
		
		for(String[] s: values){
			p.addValue(Id.create(s[0], ChargingProfile.class), Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3]));
		}
		return p;
	}

	public static DisChargingProfiles readDisChargingProfiles(String file){
		DisChargingProfiles p = new DisChargingProfiles();
		Set<String[]> values = readFileContent(file, "\t", true);
		
		Double f = 500.;
		log.warn("currently a factor of " + f + " is used for the discharging-Profiles, because the given values are to low...");
		for(String[] s: values){
			p.addValue(Id.create(s[0], DisChargingProfile.class), Double.parseDouble(s[2]), Double.parseDouble(s[1]), f * Double.parseDouble(s[3]));
		}
		return p;
	}
	
	private static Set<String[]> readFileContent(String inFile, String splitByExpr, boolean hasHeader){
		
		boolean first = hasHeader;
		Set<String[]> lines = new LinkedHashSet<String[]>();
		
		String line;
		try {
			log.info("start reading content of " + inFile);
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split(splitByExpr);
					if(first == true){
						first = false;
					}else{
						lines.add(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			log.info("finished...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
}
