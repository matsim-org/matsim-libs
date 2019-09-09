/* *********************************************************************** *
 * project: org.matsim.*
 * ColdPollutant.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.types;

/**
 * @author benjamin
 *
 */
public enum ColdPollutant {
	
	/*TODO: CO2 not directly available for cold emissions; thus it could be calculated through FC, CO, and HC as follows:
	get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866) / 0.273;*/
	CO("CO"), FC("FC"), HC("HC"), NMHC("NMHC"), NOX("NOx"), NO2("NO2"), PM("PM");
	
	private final String key;

	ColdPollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
	
	//TODO: Is this really neccessary? WarmPollutant does not have this method...jk, bk mai'13
	public static ColdPollutant getValue(String key){
		for(ColdPollutant cp : ColdPollutant.values()){
			String cpString = cp.getText();
			if(cpString.equals(key)){
				return cp;
			}
		}
		return null;
	}
}
