/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.demandde.cemdap.input;

import org.apache.log4j.Logger;

/**
 * @author dziemke
 */
public class CensusReaderStarter {
	private static final Logger LOG = Logger.getLogger(CensusReaderStarter.class);

	public static void main(String[] args) {
		String censusFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung.csv";
		String delimiter = ";";
		
		CensusReader censusReader = new CensusReader(censusFile, delimiter);
		
		LOG.info("Test: 434 = " + censusReader.getMunicipalities().getAttribute("16077039", "marriedMale"));
	}
}