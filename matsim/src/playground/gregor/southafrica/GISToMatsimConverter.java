/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeMatsimConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.southafrica;

import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Envelope;

/**
 * Converter class for street network shape files 
 * @author laemmel
 *
 */
public class GISToMatsimConverter {


	private String linestringFile = null;

	

	FeatureSource linestrings = null;
	private Envelope envelope;
	private CoordinateReferenceSystem crs;
	static final double CATCH_RADIUS = 0.5;
	public static final double CAPACITY_COEF = 1.33;
	
	
	public GISToMatsimConverter(final String lineFile){

		this.linestringFile = lineFile;
	}

	
	public void processData() throws Exception {
		
		final FeatureSource fs = ShapeFileReader.readDataFile(this.linestringFile);			
		this.envelope = fs.getBounds();
//		this.crs = fs.getSchema().getDefaultGeometry().getCoordinateSystem();
		
		
		final GraphGenerator gg = new GraphGenerator(fs);
		final Collection<Feature> graph =  gg.createGraph();
		ShapeFileWriter.writeGeometries(graph, "./southafrica/net_graph.shp");


	}
	
	public static void main(final String [] args){
		final String filename = "./southafrica/gt_str_h_transformed.shp";
		final GISToMatsimConverter converter = new GISToMatsimConverter(filename);
		try {
			converter.processData();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
