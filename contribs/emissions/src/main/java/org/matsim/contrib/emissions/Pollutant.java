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
package org.matsim.contrib.emissions;

/**
 * @author benjamin
 *
 */
public enum Pollutant{
        CO,
        CO2_TOTAL,
        FC, // fuel consumption. yyyy unit = ??
        HC, // hydro carbons
        NMHC,  // non-methane hydro carbons
        NOx,
        NO2,
        PM,
        SO2,
        FC_MJ,  // fuel consumption in MJ
        CO2_rep, // yyyyyy ???
        CO2e, // CO2 equivalent (WTW basis)
        PM2_5, // yyyy unit = ??
        PM2_5_non_exhaust, // tire wear!
        PM_non_exhaust, // tire wear!
        BC_exhaust, // black carbon
        BC_non_exhaust,
        Benzene,
        PN, // yyyyyy ????
        Pb, // lead
        CH4, // methane
        N2O,
        NH3
}

	/* CO2 not directly available for cold emissions; thus it could be calculated through FC, CO, and HC as follows:
	get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866) / 0.273;*/
// ???? kai, jan'20

