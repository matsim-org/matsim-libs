/* *********************************************************************** *
 * project: org.matsim.*
 * GeneratePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.mz2005;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.johannes.mz2005.io.EscortData;
import playground.johannes.mz2005.io.RawDataToPopulation;

/**
 * @author illenberger
 * 
 */
public class GeneratePopulation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String basedir = "/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/";
		RawDataToPopulation generator = new RawDataToPopulation();
		List<Integer> days = new ArrayList<Integer>();
		for(int i = 1; i < 6; i++)
			days.add(i);
//		days.add(6);
//		days.add(7);
		Population pop = generator.create(basedir, days);
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(basedir + "/07-09-2011/plans.wkday.xml");
		
		EscortData escortData = generator.getEscortData();
		escortData.write(basedir + "/07-09-2011/escort.wkday.txt");
	}

}
