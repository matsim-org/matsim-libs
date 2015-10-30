/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.demand.loader;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.LoaderUtils;
import playground.johannes.gsv.demand.tasks.PersonEmployed;

import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PersonEmployedLoader extends AbstractTaskWrapper {

	public PersonEmployedLoader(String file1, String key1, String file2, String key2, Random random) throws IOException {
		TObjectDoubleHashMap<String> employees = LoaderUtils.loadSingleColumn(file1, key1);
		TObjectDoubleHashMap<String> inhabitants = LoaderUtils.loadSingleColumn(file2, key2);
		
		TObjectDoubleHashMap<String> fractions = new TObjectDoubleHashMap<String>();
		TObjectDoubleIterator<String> it = employees.iterator();
		while(it.hasNext()) {
			it.advance();
			String zoneId = it.key();
			double employ = it.value();
			double inhab = inhabitants.get(zoneId);
			double frac = employ/inhab;
			fractions.put(zoneId, frac);
		}
		
		ZoneLayer<Double> zoneLayer = LoaderUtils.mapValuesToZones(fractions);
		
		this.delegate = new PersonEmployed(zoneLayer, random);
	}
}
