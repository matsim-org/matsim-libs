/* *********************************************************************** *
 * project: org.matsim.*
 * WarmPollutant.java
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
public enum WarmPollutant {
	
	CO("CO"), CO2_TOTAL("CO2(total)"), FC("FC"), HC("HC"), NMHC("NMHC"), NOX("NOx"), NO2("NO2"), PM("PM"),	SO2("SO2");
	
	private final String key;

	WarmPollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
}
