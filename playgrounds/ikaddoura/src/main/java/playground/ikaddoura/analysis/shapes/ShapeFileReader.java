/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.shapes;

import java.io.File;
import java.io.IOException;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
* @author ikaddoura
*/

public class ShapeFileReader {
	
	private final String shapeFile1 = "../../../shared-svn/studies/ihab/berlin_laermdaten_SenStadt/Gesamtlaerm_FP/Gesamtlaerm_FP_West_Soldner.shp";	

	public static void main(String[] args) throws IOException {
		ShapeFileReader shpReader = new ShapeFileReader();
		shpReader.run();
	}

	private void run() throws IOException {
		
		// read file
		readFile(shapeFile1);
	}

	private void readFile(String shapeFile) throws IOException {
		
		File file = new File(shapeFile);
	    FileDataStore myData = FileDataStoreFinder.getDataStore(file);
	    SimpleFeatureSource source = myData.getFeatureSource();
	    SimpleFeatureType schema = source.getSchema();

	    Query query = new Query(schema.getTypeName());
	    query.setMaxFeatures(2);
	    
	    FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(query);
	    
	    try (FeatureIterator<SimpleFeature> features = collection.features()) {
	        
	    	while (features.hasNext()) {
	            SimpleFeature feature = features.next();
	            System.out.println(feature.getID() + ": ");
	            for (Property attribute : feature.getProperties()) {
	                System.out.println("\t"+attribute.getName()+":"+attribute.getValue() );
	            }
	        }
	    }
	}
}

