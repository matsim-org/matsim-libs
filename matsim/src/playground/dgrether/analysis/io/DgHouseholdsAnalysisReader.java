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
package playground.dgrether.analysis.io;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;


public class DgHouseholdsAnalysisReader {

	private static final Logger log = Logger.getLogger(DgHouseholdsAnalysisReader.class);
	
	private DgAnalysisPopulation pop;


	public DgHouseholdsAnalysisReader(DgAnalysisPopulation pop) {
		this.pop = pop;
	}
	
	
	public void readHousholds(String filename) {
		Households hhs = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(hhs);
		reader.readFile(filename);
		for (Household hh : hhs.getHouseholds().values()) {
			Id memberId = hh.getMemberIds().get(0);
			DgPersonData pd = this.pop.getPersonData().get(memberId);
			if (pd != null) {
				pd.setIncome(hh.getIncome());
			}
			else {
				log.warn("No DgPerson found for member id " + memberId + " in household: " + hh.getId());
			}
		}
	}
	
}
