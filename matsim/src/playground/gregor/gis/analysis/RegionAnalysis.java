/* *********************************************************************** *
 * project: org.matsim.*
 * RegionAnalyzis.java
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

package playground.gregor.gis.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.matsim.basic.v01.Id;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileWriter;
import org.matsim.world.World;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.snapshots.postprocessor.processors.DestinationDependentColorizer;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class RegionAnalysis implements AgentDepartureEventHandler{

	private final static Logger log = Logger.getLogger(RegionAnalysis.class);
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
//	private final Plans population;
	private final CoordinateReferenceSystem crs;
	private final String outfile;
	private final HashMap<Id,Integer> regions = new HashMap<Id,Integer>();
	private final HashMap<Id,ArrayList<Point>> persons = new HashMap<Id,ArrayList<Point>>();
	private final HashMap<String,Point> dests = new HashMap<String,Point>();
	private final QuadTree<Point> tree = new QuadTree<Point>(0,0,690000,9999000);
	private final HashMap<String,String> colorMap = new HashMap<String,String>();


	Collection<Feature> features;
	private FeatureType ft;
	private final GeometryFactory geofac;
	private final String eventsfile;
	private final NetworkLayer network;


	public RegionAnalysis(final CoordinateReferenceSystem targetCRS, final String shapefile, final String eventsfile, final NetworkLayer network) {

		this.network = network;
		initColorMap();
		this.crs = targetCRS;
		this.outfile = shapefile;
		this.geofac = new GeometryFactory();
		this.eventsfile = eventsfile;
		try {
			initFeatureCollection();
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}


	private void initColorMap() {
		this.colorMap.put("63", "63");
		this.colorMap.put("14", "63");
		this.colorMap.put("87", "63");
		this.colorMap.put("13", "63");
		this.colorMap.put("61", "63");

		this.colorMap.put("48", "48");
		this.colorMap.put("30", "48");
		this.colorMap.put("48", "48");
		this.colorMap.put("3", "48");
		this.colorMap.put("38", "48");
		this.colorMap.put("6", "48");
		this.colorMap.put("10", "48");
		this.colorMap.put("71", "48");
		this.colorMap.put("69", "48");

		this.colorMap.put("33", "33");
		this.colorMap.put("24", "33");

		this.colorMap.put("65", "65");
		this.colorMap.put("77", "65");
		this.colorMap.put("26", "65");
		this.colorMap.put("64", "65");

		this.colorMap.put("21", "21");
		this.colorMap.put("58", "21");

		this.colorMap.put("89", "89");
		this.colorMap.put("12", "89");

		this.colorMap.put("18", "18");
		this.colorMap.put("17", "18");
		this.colorMap.put("47", "18");
		this.colorMap.put("86", "18");

		this.colorMap.put("2", "2");
		this.colorMap.put("39", "2");

		this.colorMap.put("76", "76");
		this.colorMap.put("4", "76");
		this.colorMap.put("11", "76");
		this.colorMap.put("5", "76");
		this.colorMap.put("7", "76");
		this.colorMap.put("59", "76");
		this.colorMap.put("36", "76");
		this.colorMap.put("80", "76");

		this.colorMap.put("29", "8");

		this.colorMap.put("22", "88");
		this.colorMap.put("43", "88");

		this.colorMap.put("45", "50");
		this.colorMap.put("19", "50");
		this.colorMap.put("84", "84");
		this.colorMap.put("75", "84");
		this.colorMap.put("53", "84");



	}


	private void initFeatureCollection() throws FactoryRegistryException, SchemaException {

		this.features = new ArrayList<Feature>();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType strId = AttributeTypeFactory.newAttributeType("nodeID", String.class);
		AttributeType count = AttributeTypeFactory.newAttributeType("count", Integer.class);
//		AttributeType shortest = AttributeTypeFactory.newAttributeType("shortest", Integer.class);
//		AttributeType current = AttributeTypeFactory.newAttributeType("current", Integer.class);
//		AttributeType deviance = AttributeTypeFactory.newAttributeType("diffsc", Integer.class);
		this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id,strId,count}, "plansShape");
	}


	public void run() {

		Events events = new Events();
		DestinationDependentColorizer ddc = new DestinationDependentColorizer();
		events.addHandler(ddc);
		events.addHandler(this);
		new EventsReaderTXTv1(events).readFile(this.eventsfile);

		for (String key : this.dests.keySet()) {
			String c = getColor(ddc.getColor(key));
			Point p = this.dests.get(key);
			try {
				this.features.add(this.ft.create(new Object [] {p,Integer.parseInt(c),c,0}));
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


		try {
			ShapeFileWriter.writeGeometries(this.features, this.outfile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private String getColor(final String color) {
		if (this.colorMap.get(color) != null) {
			return this.colorMap.get(color);
		}
		return color;
	}


	public void handleEvent(final AgentDepartureEvent event) {
		String id = event.agentId;
		if (id.contains("guide")) {
			return;
		}
		String lID = event.linkId;
		Link l = this.network.getLink(lID);
		Point p = this.geofac.createPoint(MGC.coord2Coordinate(l.getCenter()));
		if (this.tree.get(p.getX(), p.getY(), 1).size() <= 0) {
			this.dests.put(id,p);
			this.tree.put(p.getX(),p.getY(), p);
		}
	}


	public void reset(final int iteration) {
		// TODO Auto-generated method stub

	}

	public static void main(final String [] args) throws FactoryException {
//		String district_shape_file;

		final String shapefile = "./padang/region.shp";

		if (args.length != 1) {
			throw new RuntimeException("wrong number of arguments! Pleas run DistanceAnalysis config.xml shapefile.shp" );
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});

		}

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");

//		log.info("loading shape file from " + district_shape_file);
//		FeatureSource features = null;
//		try {
//			features = ShapeFileReader.readDataFile(district_shape_file);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		log.info("done");


//		log.info("loading population from " + Gbl.getConfig().plans().getInputFile());
//		Plans population = new Plans();
//		PlansReaderI plansReader = new MatsimPlansReader(population);
//		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
//		log.info("done.");

		String eventsfile = Gbl.getConfig().events().getInputFile();

		final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM47S);

		new RegionAnalysis(targetCRS,shapefile,eventsfile,network).run();
	}


}
