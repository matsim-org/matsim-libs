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
    //Comments based on HBEFA 4.1 Quick Reference. See https://www.hbefa.net
    // Unit seem to be g/km
        CO,     //carbon monoxide
        CO2_TOTAL, // = carbon dioxide “total”, computed as total CO2 from fuel consumption
        FC,     // fuel consumption. unit = g/km
        HC,     // hydrocarbons [total HC]
        NMHC,   // non-methane hydro carbons
        NOx,    // nitrogen oxide
        NO2,    // provided as g/km, but based on %-values of NOx
        PM,     // = PM10 --> particulate matter of size below 10μm, i.e. equivalent to PM10. unit = g/km
        SO2,    //sulphur dioxide
        FC_MJ,  // fuel consumption in MJ/km
        CO2_rep, // CO2(reported): = carbon dioxide “reported”, i.e. without the biofuel share in the fuel -> input for CO2e calculation
        CO2e,   // CO2 equivalents (WTW basis), CO2 equivalents contain CO2, CH4 and N2O, i.e. the relevant greenhouse gases from the transport sector, multiplied with their respective 100-year Global Warming Potentials and summed up.
        PM2_5,  // particle mass for particles < 2.5 µm. unit = g/km
        PM2_5_non_exhaust, // tire wear!
        PM_non_exhaust, // PM10 from non-exhaust sources(e.g. road, tyre wear)
        BC_exhaust, // black carbon
        BC_non_exhaust, // Black carbon from non-exhaust sources(e.g. road, tyre wear)
        Benzene, //benzene
        PN,     // particle numbers / km
        Pb,     // lead
        CH4,    // methane
        N2O,    // nitrous oxide
        NH3     // ammonia
}

	/* CO2 not directly available for cold emissions; thus it could be calculated through FC, CO, and HC as follows:
	get("FC")*0.865 - get(CO)*0.429 - get("HC")*0.866) / 0.273;*/
// ???? kai, jan'20

