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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

/**
 * Aggregate the path-dependent network to hexagonal zones and report a number
 * of network statistics, for example:
 * <ul>
 * 		<li> number of network nodes in the zone;
 * 		<li> total node degree in the zone.
 * </ul>
 * 
 * @author jwjoubert
 */
public class AggregateNetworkDegreeToHexagons {
	final private static Logger LOG = Logger.getLogger(AggregateNetworkDegreeToHexagons.class);
	private static Map<Point, Integer> zoneMap = new HashMap<>();
	private static Map<Point, Integer> facilityCountMap = new HashMap<>();
	private static  Map<Point, Polygon> polyMap = new HashMap<>();
	final private static double WIDTH = 1000;
	final private static double HEIGHT = Math.sqrt(3.0)*WIDTH/2.0;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AggregateNetworkDegreeToHexagons.class.toString(), args);
		
		String network = args[0];
		String shapefile = args[1];
		String outputfolder = args[2];
		double zoneWidth = Double.parseDouble(args[3]);
		
		/* Parse the network. */
		DigicorePathDependentNetworkReader_v2 nr = new DigicorePathDependentNetworkReader_v2();
		nr.parse(network);
		PathDependentNetwork pdn = nr.getPathDependentNetwork();
		pdn.writeNetworkStatisticsToConsole();
		
		/* Convert the geometry to hexagons. */
		ShapeFileReader ctReader = new ShapeFileReader();
		ctReader.readFileAndInitialize(shapefile);
		SimpleFeature ctFeature = ctReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		MultiPolygon ct;
		if(ctFeature.getDefaultGeometry() instanceof MultiPolygon){
			ct = (MultiPolygon)ctFeature.getDefaultGeometry();
		} else{
			LOG.error("Geomtery is not of type MultiPolygon");
			throw new RuntimeException("Cannot proceed without MultiPolygon.");
		}
		GeneralGrid gg = new GeneralGrid(zoneWidth, GridType.HEX);
		gg.generateGrid(ct);
		gg.writeGrid(outputfolder, "WGS84_SA_Albers");
		QuadTree<Point> grid = gg.getGrid();
		
		/* Initialise the grid's map with zero counts, and the actual geomtery. */
		GeometryFactory gf = new GeometryFactory();
		for(Point p : grid.values()){
			zoneMap.put(p, 0);
			facilityCountMap.put(p, 0);
			
			/* Create a hexagonal polygon. */
			double w = 0.5*zoneWidth;
			double h = Math.sqrt(3.0)/2 * w;
			double x = p.getX();
			double y = p.getY();
			Coordinate c1 = new Coordinate(x-w, y);
			Coordinate c2 = new Coordinate(x-0.5*w, y+h);
			Coordinate c3 = new Coordinate(x+0.5*w, y+h);
			Coordinate c4 = new Coordinate(x+w, y);
			Coordinate c5 = new Coordinate(x+0.5*w, y-h);
			Coordinate c6 = new Coordinate(x-0.5*w, y-h);
			Coordinate[] ca = {c1, c2, c3, c4, c5, c6, c1};
			
			Polygon hex = gf.createPolygon(ca);
			polyMap.put(p, hex);
		}
		
		/* Process each node in the network. */
		LOG.info("Processing the points...");
		Counter counter = new Counter("   nodes # ");
		for(PathDependentNode node : pdn.getPathDependentNodes().values()){
			Coord coord = node.getCoord();
			Point nodePoint = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));

			Point zonePoint = grid.getClosest(coord.getX(), coord.getY());
			Polygon zone = polyMap.get(zonePoint);
			int degree = 0;
			if(zone.contains(nodePoint)){
				degree = node.getInDegree() + node.getOutDegree();
				int oldDegree = zoneMap.get(zonePoint);
				zoneMap.put(zonePoint, oldDegree+degree);
				
				int oldCount = facilityCountMap.get(zonePoint);
				facilityCountMap.put(zonePoint, oldCount+1);
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Generating the shapefile of aggregated values. */
		SimpleFeatureCollection collection = createFeatureCollection();
		try {
			String outputShapefile = outputfolder + (outputfolder.endsWith("/") ? "" : "/") + "DegreeShapefile.shp";
			writeShapefile(outputShapefile, collection);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write shapefile.");
		}

		Header.printFooter();
	}
	
	
	private static SimpleFeatureType createFeatureType(){
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Zone");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		
		/* Add the attributes in order. */
		builder.add("Zone", Polygon.class);
		builder.length(6).add("Id", Integer.class);
		builder.length(6).add("Facilities", Integer.class);
		builder.length(8).add("Total_Degree", Integer.class);
		
		/* build the type. */
		final SimpleFeatureType ZONE = builder.buildFeatureType();
		return ZONE;
	}
	
	private static SimpleFeatureCollection createFeatureCollection(){
		LOG.info("Creating feature collection for shapefile...");
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
		
		Counter counter = new Counter("   feature # ");
		for(Point p : polyMap.keySet()){
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(createFeatureType());

			/* Build polygon. */
			builder.add(getPolygonWgs84(p));
			builder.add(counter.getCounter());
			
			/* Add the number of facilities values. */
			builder.add(facilityCountMap.get(p));
			
			/* Add the weighted degree. */
			builder.add(zoneMap.get(p));
			SimpleFeature feature = builder.buildFeature(null);
			collection.add(feature);
			
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done creating feature collection.");
		
		return collection;
	}
	
	private static Polygon getPolygonWgs84(Point p){
		GeometryFactory gf = new GeometryFactory();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		/* Create the polygon. */
		double x = p.getX();
		double y = p.getY();
		double w = WIDTH/2.0;
		double h = HEIGHT/2.0;
		List<Coord> cl = new ArrayList<Coord>();
		cl.add(new Coord(x - w, y));
		cl.add(new Coord(x - 0.5 * w, y + h));
		cl.add(new Coord(x + 0.5 * w, y + h));
		cl.add(new Coord(x + w, y));
		cl.add(new Coord(x + 0.5 * w, y - h));
		cl.add(new Coord(x - 0.5 * w, y - h));
		List<Coordinate> clc = new ArrayList<Coordinate>();
		for(Coord c : cl){
			Coord cc = ct.transform(c);
			clc.add(new Coordinate(cc.getX(), cc.getY()));
		}
		clc.add(clc.get(0));
		Coordinate[] ca = new Coordinate[clc.size()];
		
		return gf.createPolygon(clc.toArray(ca));
	}

	
	private static void writeShapefile(String filename, FeatureCollection collection) throws IOException{
		LOG.info("Writing shapefile to " + filename);
		File file = new File(filename);
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", file.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		dataStore.createSchema(createFeatureType());
		
		Transaction transaction = new DefaultTransaction("create");
		
		String typeName = dataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
		if(featureSource instanceof SimpleFeatureSource){
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try{
				featureStore.addFeatures(collection);
				transaction.commit();
			} finally{
				transaction.close();
			}
		} else{
			System.exit(1);
		}
	}

}
