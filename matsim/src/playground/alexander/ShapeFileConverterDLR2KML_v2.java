/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileConverterDLR2KML_v2.java
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
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkWriter;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.Geometry;

public class ShapeFileConverterDLR2KML_v2 {
	
    public FeatureCollection readDataFile(String fileName) throws Exception {
		
		File dataFile = new File(fileName);
		
		new HashMap<String,Vector<Geometry>>();
	    Map connect = new HashMap();
	    connect.put( "url", dataFile.toURL() );

	    DataStore dataStore = DataStoreFinder.getDataStore( connect );
	    String[] typeNames = dataStore.getTypeNames ();
	    String typeName = typeNames[0];
	    System.out.println( "Reading content "+ typeName + " ... performing coordinate transformation if needed");

	    FeatureSource featureSource = dataStore.getFeatureSource( typeName );
	    FeatureCollection collection = featureSource.getFeatures();
	    return(collection);
	}   
	   	
	public static void main( String[] args ) {
		
		String fileNamePolygon;
		String fileNameLinks;
		String fileNameNet;
		
		if (args.length == 3){
			fileNamePolygon = args[0];
			fileNameLinks = args[1];
			fileNameNet = args[2];
		}
		
		else{
			
		fileNamePolygon = "./padang/padang_streets.shp";
		fileNameLinks = "/padang/vd10_streetnetwork_padang_v0.5_utm47s.shp";
		fileNameNet = "./padang/padang_net.xml";		
				
		}
		
		World world = Gbl.createWorld();
		Config config = Gbl.createConfig(new String[] {"./evacuationConf.xml"});
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(fileNameNet);
		
		ShapeFileConverterDLR2KML_v2 shapeFileConverter = new ShapeFileConverterDLR2KML_v2();
		FeatureCollection polygonCollection = null;
		try {
			polygonCollection = shapeFileConverter.readDataFile(fileNamePolygon);
		} catch (Exception e) {
			e.printStackTrace();
		}
		FeatureCollection linksCollection = null;
		try {
			linksCollection = shapeFileConverter.readDataFile(fileNameLinks);
		} catch (Exception e) {
			e.printStackTrace();
		}
    			
		Line2Link l2l = new Line2Link(polygonCollection, linksCollection, network);
		l2l.writeWidth();		
		
		NetworkWriter nw = new NetworkWriter(network,"./padang/padang_net_new.xml");
		nw.write();
				
	}

}

