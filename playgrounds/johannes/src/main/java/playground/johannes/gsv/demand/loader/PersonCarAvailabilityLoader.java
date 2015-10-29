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

import gnu.trove.TIntDoubleHashMap;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.LoaderUtils;
import playground.johannes.gsv.demand.tasks.PersonCarAvailability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PersonCarAvailabilityLoader extends AbstractTaskWrapper {

	public PersonCarAvailabilityLoader(String file, Random random) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split(LoaderUtils.FIELD_SEPARATOR);
			int lower = Integer.parseInt(tokens[0]);
			int upper = Integer.parseInt(tokens[1]);
			double frac = Double.parseDouble(tokens[2]);
			for(int i = lower; i < upper+1; i++) {
				map.put(i, frac);
			}
		}
		
		reader.close();
		
		delegate = new PersonCarAvailability(map, random);
	}
}
