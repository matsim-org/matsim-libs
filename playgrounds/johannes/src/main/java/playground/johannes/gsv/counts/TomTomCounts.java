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

package playground.johannes.gsv.counts;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author johannes
 *
 */
public class TomTomCounts {

	public static void main(String args[]) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/Schreibtisch/bast_gesamtfluss_zeitabhaengig.csv"));
		
		TObjectIntHashMap<String> counts = new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> hours = new TObjectIntHashMap<String>();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(",");
			
			String season = tokens[1];
			int count = Integer.parseInt(tokens[4]);
			
			counts.adjustOrPutValue(season, count, count);
			
			String bin = tokens[3];
			if(bin.equalsIgnoreCase("6-10"))
				hours.adjustOrPutValue(season, 4, 4);
			else if(bin.equalsIgnoreCase("10-16"))
				hours.adjustOrPutValue(season, 6, 6);
			else if(bin.equalsIgnoreCase("16-20"))
				hours.adjustOrPutValue(season, 4, 4);
			else if(bin.equalsIgnoreCase("20-6"))
				hours.adjustOrPutValue(season, 10, 10);
			else
				throw new RuntimeException();
		}
		
		TObjectIntIterator<String> it = counts.iterator();
		for(int i = 0; i < counts.size(); i++) {
			it.advance();
//			int num = hours.get(it.key());
			int num = 5;
			if(it.key().equalsIgnoreCase("Sommer"))
				num = 7;
			System.out.println(String.format("%s vehicles for season %s", it.value()/num, it.key()));
		}
	}
}
