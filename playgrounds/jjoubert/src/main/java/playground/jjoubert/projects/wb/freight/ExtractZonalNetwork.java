/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractZonalNetwork.java
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
package playground.jjoubert.projects.wb.freight;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

/**
 * @author jwjoubert
 *
 */
public class ExtractZonalNetwork {
	final private static Logger LOG = Logger.getLogger(ExtractZonalNetwork.class);
	final private static GridType GRID_TYPE = GridType.HEX;
	final private static Double GRID_WIDTH = 2000.0;

	/**
	 * Class to build a complex network of connectivity, not between clustered
	 * facilities, but rather zones. For this implementation for the World Bank,
	 * the zones will be a hexagonal grid with width 2km.
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ExtractZonalNetwork.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String vehiclesFile = args[0];
		String shapefile = args[1];
		String networkFile = args[2];
		
		/* Build the grid from the shapefile */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		Iterator<SimpleFeature> iterator = features.iterator();
		Object o = iterator.next().getDefaultGeometry();
		Geometry sa = null;
		if(o instanceof MultiPolygon){
			sa = (Geometry) o;
		}
		GeneralGrid grid = new GeneralGrid(GRID_WIDTH, GRID_TYPE);
		grid.generateGrid(sa);
	}

}
