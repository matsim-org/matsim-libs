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
package playground.droeder.eMobility.io;

import java.util.Set;

import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.DaFileReader;
import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;

/**
 * @author droeder
 *
 */
public class EmobEnergyProfileReader {
	
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
		
		for(String[] s: values){
			p.addValue(new IdImpl(s[0]), Double.parseDouble(s[2]), Double.parseDouble(s[1]), Double.parseDouble(s[3]));
		}
		return p;
	}
}
