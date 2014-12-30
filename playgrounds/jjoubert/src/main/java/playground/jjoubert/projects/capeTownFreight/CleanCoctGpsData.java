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
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;

/**
 * Class to read in City of Cape Town GPS records and removing all points that
 * are not within the boundaries of the city.
 * 
 * @author jwjoubert
 */
public class CleanCoctGpsData {
	final private static Logger LOG = Logger.getLogger(CleanCoctGpsData.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CleanCoctGpsData.class.toString(), args);
		String gpsFile = args[0];
		String shapefile = args[1];
		String outputFile = args[2];
		
		/* Read in shapefile. */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> set = sfr.getFeatureSet();
		Iterator<SimpleFeature> it = set.iterator();
		LOG.info("Number of features ---> " + set.size());
		
		MultiPolygon coct = null;
		SimpleFeature sf = it.next();
		if(sf.getDefaultGeometry() instanceof MultiPolygon){
			coct = (MultiPolygon)sf.getDefaultGeometry();
		}
		Geometry envelope = coct.getEnvelope(); 
		
		/* Parse GPS file. */
		Counter counter = new Counter("   lines # ");
		int pointsDropped = 0;
		GeometryFactory gf = new GeometryFactory();
		BufferedReader br = IOUtils.getBufferedReader(gpsFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			String line = null;
			while((line=br.readLine()) != null){
				String[] sa = line.split(",");
				double lon = Double.parseDouble(sa[2]);
				double lat = Double.parseDouble(sa[3]);
				Point p = gf.createPoint(new Coordinate(lon, lat));
				if(envelope.contains(p)){
					if(coct.contains(p)){
						bw.write(line);
						bw.newLine();
					} else{
						pointsDropped++;
					}
				} else{
					pointsDropped++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + gpsFile);
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + gpsFile);
			}
		}
		counter.printCounter();
		LOG.info("Number of points dropped: " + pointsDropped);
		
		Header.printFooter();
	}

}
