/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.Utilities.roadpricing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * Class to parse the Gauteng Freeway Improvement Project toll gantry locations
 * from known shapefiles.
 * 
 * @author jwjoubert
 */
public class GautengGantryParser {
	private final static Logger LOG = Logger.getLogger(GautengGantryParser.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GautengGantryParser.class.toString(), args);
		
		String csvfile = args[0];
		Map<Id<Coord>, Coord> coordMap = new TreeMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(csvfile);
		try{
			String line = br.readLine(); /* header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Double lon = Double.parseDouble(sa[0]);
				Double lat = Double.parseDouble(sa[1]);
				String name = sa[2];
				coordMap.put(Id.create(name, Coord.class), new Coord(lon, lat));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + csvfile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + csvfile);
			}
		}
		
		/* Just write coordinates out to console. */
		for(Id<Coord> id : coordMap.keySet()){
			LOG.info(String.format("%s: (%.4f, %.4f)", id.toString(), coordMap.get(id).getX(), coordMap.get(id).getY()));
		}
		
		Header.printFooter();
	}

}
