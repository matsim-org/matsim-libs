/* *********************************************************************** *
 * project: org.matsim.*
 * DgHouseholdsAnalysisReader
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
package playground.dgrether.analysis.population;

import org.matsim.households.basic.BasicHousehold;
import org.matsim.households.basic.BasicHouseholds;
import org.matsim.households.basic.BasicHouseholdsImpl;
import org.matsim.households.basic.BasicHouseholdsReaderV10;


public class DgHouseholdsAnalysisReader {

	
	private DgAnalysisPopulation pop;


	public DgHouseholdsAnalysisReader(DgAnalysisPopulation pop) {
		this.pop = pop;
	}
	
	
	public void readHousholds(String filename) {
		BasicHouseholds<BasicHousehold> hhs = new BasicHouseholdsImpl();
		BasicHouseholdsReaderV10 reader = new BasicHouseholdsReaderV10(hhs);
		reader.readFile(filename);
		for (BasicHousehold hh : hhs.getHouseholds().values()) {
			DgPersonData pd = this.pop.getPersonData().get(hh.getMemberIds().get(0));
			pd.setIncome(hh.getIncome());
		}
	}
	
}
