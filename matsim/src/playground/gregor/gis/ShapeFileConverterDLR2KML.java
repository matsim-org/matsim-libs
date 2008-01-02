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

package playground.gregor.gis;

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
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.geotools.geometry.jts.JTS;

public class ShapeFileConverterDLR2KML {
	

    private Map<String,Vector<Geometry>> geometries;
	
	private final static String WGS84 = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	
	private MathTransform transform;
	
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

	    
		try {
			this.transform = CRS.findMathTransform(CRS.parseWKT(WGS84_UTM47S), CRS.parseWKT(WGS84),true);
		} catch (FactoryException e) {
			e.printStackTrace();
			System.exit(-1);	
		}
	    
    	try {
		       while( iterator.hasNext() ){
       	   
		    	   Feature feature = iterator.next();
//		           String type = feature.getAttribute(2).toString();		  		           
		           

		    	   MultiPolygon geo = (MultiPolygon) feature.getDefaultGeometry();
		    	   
		    	   int polys = geo.getNumGeometries();	
		    	   for (int i = 0; i < polys; i++){
		    		   Polygon poly = (Polygon) JTS.transform(geo.getGeometryN(i), this.transform);
		    			   
		    			   
		            	Coordinate [] c = new Coordinate[]{new Coordinate(650483.842005,9899500.877189,Double.NaN)};
		            	
		            	GeometryFactory geofac = new GeometryFactory();
		        		CoordinateSequence seq = new CoordinateArraySequence(c);
		        		Point point = new Point(seq,geofac);		        		
		        		
		        		if (!geo.contains(point)) {
		        			continue;
		        		}		        		
		            	System.out.println("//////////////////////new link: ");
       			            			            				            			            	
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
		fileName=  "./padang/padang_water.shp" ;
		
		ShapeFileConverterDLR2KML shapeFileConverter = new ShapeFileConverterDLR2KML();
		shapeFileConverter.readDataFile(fileName);
	    
	}

}

