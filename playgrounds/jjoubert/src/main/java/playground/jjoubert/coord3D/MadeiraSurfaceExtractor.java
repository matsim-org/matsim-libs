/* *********************************************************************** *
 * project: org.matsim.*
 * MadeiraSurfaceExtractor.java
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

/**
 * 
 */
package playground.jjoubert.coord3D;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.coord3D.Utils3D;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

/**
 * Class to extract the SRTM points from the tiles of Madeira.
 * 
 * @author jwjoubert
 */
public class MadeiraSurfaceExtractor {
	final private static Logger LOG = Logger.getLogger(MadeiraSurfaceExtractor.class);
	final private static Double SIZE = 0.001;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MadeiraSurfaceExtractor.class.toString(), args);
		
		String tileFolder = args[0];
		String shapefile = args[1];
		String elevationFile = args[2];
		
		Geometry area = getBufferGeometry(shapefile);
		Collection<Point> points = buildRegularGrid(area);
		writeElevationToFile(points, elevationFile, tileFolder);
		
		Header.printFooter();
	}
	
	private static Geometry getBufferGeometry(String shapefile){
		LOG.info("Extracting surface geometry from Madeira shapefile...");
		Geometry area = null;
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("There are multiple SimpleFeatures... only using first.");
		}
		SimpleFeature firstFeature = features.iterator().next();
		Object o = firstFeature.getDefaultGeometry();
		if(o instanceof MultiPolygon){
			area = (MultiPolygon) o;
		}
		
		LOG.info("Done extracting surface geometry.");
		return area.getEnvelope();
	}
	
	
	private static Collection<Point> buildRegularGrid(Geometry area){
		LOG.info("Building regular grid...");
		
		GeneralGrid grid = new GeneralGrid(SIZE, GridType.SQUARE);
		grid.generateGrid(area);
		
		LOG.info("Done with grid.");
		return grid.getGrid().values();
	}
	
	private static void writeElevationToFile(Collection<Point> points, String filename, String pathToTiles){
		LOG.info("Estimating elevation...");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		Counter counter = new Counter("  point # ");
		try{
			bw.write("x,y,z");
			bw.newLine();
			for(Point p : points){
				Coord c = CoordUtils.createCoord(p.getX(), p.getY());
				double elevation = Utils3D.estimateSrtmElevation(pathToTiles, c);
				bw.write(String.format("%.6f,%.6f,%.1f\n", 
						c.getX(), c.getY(), elevation));
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}
		counter.printCounter();
		LOG.info("Done with elevation estimation.");
	}

}
