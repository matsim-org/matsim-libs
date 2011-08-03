/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventHotImpl.java
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
package playground.benjamin.events.emissions;

public enum ColdPollutant {
	
	FC("FC"), NOX("NOx"), NO2("NO2"), PM("PM"),
	CO("CO"), HC("HC");
	
	private String key;

	ColdPollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
	
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
