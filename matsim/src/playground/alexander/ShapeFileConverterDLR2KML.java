/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileConverterDLR2KML.java
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

package playground.alexander;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class ShapeFileConverterDLR2KML {
	

    private Map<String,Vector<Geometry>> geometries;
	

	
	public void readDataFile(String fileName) throws Exception {
		
		File dataFile = new File(fileName);
		
		geometries = new HashMap<String,Vector<Geometry>>();
	    Map connect = new HashMap();
	    connect.put( "url", dataFile.toURL() );

	    DataStore dataStore = DataStoreFinder.getDataStore( connect );
	    String[] typeNames = dataStore.getTypeNames ();
	    String typeName = typeNames[0];
	    System.out.println( "Reading content "+ typeName + " ... performing coordinate transformation if needed");

	    FeatureSource featureSource = dataStore.getFeatureSource( typeName );
	    FeatureCollection collection = featureSource.getFeatures();
	    
	    FeatureIterator iterator = collection.features();
    	Shape2Lineu s2l = new Shape2Lineu(collection);

    	try {
		       while( iterator.hasNext() ){
       	   
		    	   Feature feature = iterator.next();
		           String type = feature.getAttribute(2).toString();		  		           
		           
		           if (type.contains("inner-urban")){
		            	Geometry geo = feature.getDefaultGeometry();
		            			            			            	
		            	Coordinate [] c = new Coordinate[]{new Coordinate(650524.261238,9893569.025124,Double.NaN)};
//		            	Coordinate [] c = new Coordinate[]{new Coordinate(650483.842005,9899500.877189,Double.NaN)};
		            	
		            	GeometryFactory geofac = new GeometryFactory();
		        		CoordinateSequence seq = new CoordinateArraySequence(c);
		        		Point point = new Point(seq,geofac);		        		
		        		
		        		if (!geo.contains(point)) {
		        			continue;
		        		}		        		
		            	System.out.println("//////////////////////new link: ");
		            	s2l.createLinie(geo);       			            			            				            			            	
            		}
		       }		        
		    }		    
		    finally {
		       iterator.close();
		    }		
	}

	public static void main( String[] args ) throws Exception
	{
		String fileName;
		if (args.length == 1)
			fileName = args[0] ;
		else
//			fileName=  "jalan_padang/02_text__text.shp" ;
		fileName=  "./padang/padang_streets_new.shp" ;
		
		ShapeFileConverterDLR2KML shapeFileConverter = new ShapeFileConverterDLR2KML();
		shapeFileConverter.readDataFile(fileName);
	    
	}

}

