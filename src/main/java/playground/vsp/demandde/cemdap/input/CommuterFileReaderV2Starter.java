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
public class CommuterFileReaderV2Starter {
	private static final Logger LOG = Logger.getLogger(CommuterFileReaderV2Starter.class);

	public static void main(String[] args) {
		String commuterFileOutgoing = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
		String delimiter = "\t";
		
		CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, delimiter);
		
		int origin = 12051000; // "Brandenburg an der Havel, St."
		int destination = 11000000; // "Berlin, Stadt"
		LOG.info("Test: 1513 = " + commuterFileReader.getRelationsMap().get(origin).get(destination).getTrips());
	}
}