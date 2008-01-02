/* *********************************************************************** *
 * project: org.matsim.*
 * MedianStripShapeFileWriter.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.Hints;
import org.geotools.feature.DefaultFeature;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureType;

import org.geotools.geometry.jts.FactoryFinder;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.StringUtils;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

public class MedianStripShapeFileWriter {
	
	private ArrayList<QueueLink> links;

	private NetworkLayer network;
	
	private String inputFile;
	
	private Geometry [] geos;
	
	private FeatureSource fs; 
	
	ShapefileDataStore ds = null;
	
	public MedianStripShapeFileWriter(String filename) {
		
		
		File inFile = new File("./padang/vd10_streetnetwork_padang_v0.5_utm47s.shp");
		
	      // Load shapefile
	      
		try {
			ds = new ShapefileDataStore(inFile.toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      try {
			this.fs = ds.getFeatureSource();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		links = new ArrayList<QueueLink>();
		String netfile = "./networks/padang_net.xml";
		System.out.println("reading network xml file... ");
		this.network = new QueueNetworkLayer();
		new MatsimNetworkReader(this.network).readFile(netfile);
		System.out.println("done. ");
		this.inputFile = filename;
		
	}

	public void run(){
		readFile(this.inputFile);
		createGeometries();
		try {
			writeGeometries();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void writeGeometries() throws Exception {
		FeatureType ft = this.fs.getSchema();
		File out = new File("./padang/street_correction.shp");
	      
		String name = ds.getTypeNames()[0];
		
		ShapefileDataStore outStore = new ShapefileDataStore(out.toURL());
        outStore.createSchema(fs.getSchema());
        FeatureSource newFeatureSource = outStore.getFeatureSource(name);
        FeatureStore newFeatureStore = (FeatureStore)newFeatureSource;
        
        // accquire a transaction to create the shapefile from FeatureStore
        Transaction t = newFeatureStore.getTransaction();
        
        FeatureCollection collect = new PFeatureCollection(ft);
        
	      for (int i = 0; i < geos.length; i++){
	    	  Geometry geo = geos[i];
	    	Feature feature = ft.create(null);
	    	feature.setDefaultGeometry(geo);
	    	collect.add(feature);
	    	
	    	
	    	  
	      }
	      newFeatureStore.addFeatures(collect);
	      
	      t.commit();
	      t.close();
	      
		
	}

	private void createGeometries() {
		
		CoordinateTransformationI transform = new GeotoolsTransformation("WGS84_UTM47S","WGS84_UTM47S");
		
		geos = new Geometry[this.links.size()];
		PrecisionModel pm = new PrecisionModel(10);
		GeometryFactory geofac = new GeometryFactory(pm);
		int i = 0;
		for (QueueLink link : this.links){
			
			CoordI a = transform.transform(link.getFromNode().getCoord());
			CoordI b = transform.transform(link.getToNode().getCoord());
			
			Coordinate [] coords = {MGC.coord2Coordinate(a),MGC.coord2Coordinate(b)};
			LineString ls = new LineString(coords,pm,i);
			
			
			MultiLineString lss = new MultiLineString(new LineString[]{ls},geofac);
			geos[i++] = lss;
			
		}
	}

	public void readFile(final String filename) {
		
		BufferedReader  infile;
		try {
			infile = IOUtils.getBufferedReader(filename);
			String line = infile.readLine();
			if (line != null && line.charAt(0) >= '0' && line.charAt(0) <= '9') {
				/* The line starts with a number, so assume it's an event and parse it.
				 * Otherwise it is likely the header.  */
				parseLine(line);
			}
			// now all other lines should contain events
			while ( (line = infile.readLine()) != null) {
				parseLine(line);
			}
			infile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	
	
	private void parseLine(String line) {
		String[] result = StringUtils.explode(line, ',', 7);
		links.add((QueueLink) network.getLink(result[0]));
		
	}

	  public static class PFeatureCollection extends DefaultFeatureCollection {
		    public PFeatureCollection(FeatureType ft) {
		      super("error",ft );
		    }
		  }

	public static void main(String [] args){
		String configFile = "./configs/evacuationConf.xml";
		
		
		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});
		
		String filename = "./padang/mittelstreifen.csv";
		MedianStripShapeFileWriter mss = new MedianStripShapeFileWriter(filename);
		mss.run();
		
	}
}
