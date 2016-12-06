/* *********************************************************************** *
 * project: org.matsim.*
 * LanduseParser.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.population.census2011.capeTown.facilities;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;

/**
 * Class to parse the land use data provided by the City of Cape Town.
 * 
 * @author jwjoubert
 */
public class LanduseParser {
	final private static Logger LOG = Logger.getLogger(LanduseParser.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(LanduseParser.class.toString(), args);
		
		String shapefile = args[0];
		String facilitiesFile = args[1];
		
		ShapeFileReader sr = new ShapeFileReader();
		sr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sr.getFeatureSet();
		
		LanduseConverter luc = new LanduseConverter();
		Counter counter = new Counter("  parcel # ");
		for(SimpleFeature sf : features){
			luc.convertFeature(sf);
			counter.incCounter();
		}
		counter.printCounter();
		luc.reportLanduseCounts();
		
		ActivityFacilities facilities = luc.convertParcelsToFacilities();
		new FacilitiesWriter(facilities).write(facilitiesFile);
		
		Header.printFooter();
	}
	
	private LanduseParser() {
		/* Hidden constructor. */
	}
	
	
}
