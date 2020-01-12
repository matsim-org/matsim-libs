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
	
	CO, FC, HC, NMHC, NOX, NO2, PM
	
	/* CO2 not directly available for cold emissions; thus it could be calculated through FC, CO, and HC as follows:
	get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866) / 0.273;*/
	// ???? kai, jan'20
}
