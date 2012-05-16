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
package playground.droeder.e.energy;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.DaFileReader;

/**
 * @author droeder
 *
 */
public class EmobEnergyProfileReader {
	private static final Logger log = Logger
			.getLogger(EmobEnergyProfileReader.class);
	
	public static ChargingProfiles readChargingProfiles(String file){
		ChargingProfiles p = new ChargingProfiles();
		Set<String[]> values = DaFileReader.readFileContent(file, "\t", true);
		
		for(String[] s: values){
			p.addValue(new IdImpl(s[0]), Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3]));
		}
		return p;
	}

	public static DisChargingProfiles readDisChargingProfiles(String file){
		DisChargingProfiles p = new DisChargingProfiles();
		Set<String[]> values = DaFileReader.readFileContent(file, "\t", true);
		
		Double f = 500.;
		log.warn("currently a factor of " + f + " is used for the discharging-Profiles, because the given values are to low...");
		for(String[] s: values){
			p.addValue(new IdImpl(s[0]), Double.parseDouble(s[2]), Double.parseDouble(s[1]), f * Double.parseDouble(s[3]));
		}
		return p;
	}
}
