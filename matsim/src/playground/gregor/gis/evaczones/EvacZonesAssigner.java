/* *********************************************************************** *
 * project: org.matsim.*
 * EvacZonesAssigner.java
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

package playground.gregor.gis.evaczones;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.ShapeFileWriter;
import org.matsim.world.World;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.convexer.Concaver;
import playground.gregor.snapshots.postprocessor.processors.DestinationDependentColorizer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class EvacZonesAssigner implements AgentDepartureEventHandler{

	private final static Logger log = Logger.getLogger(EvacZonesAssigner.class);
	
	private final HashMap<String,String> colorMap = new HashMap<String,String>();

	private final CoordinateReferenceSystem targetCRS;

	private final String shapefile;

	private final String eventsfile;

	private final NetworkLayer network;
	
	private final HashMap<String,Point> dests = new HashMap<String,Point>();
	
	private final QuadTree<Point> tree = new QuadTree<Point>(0,0,690000,9999000);

	private final Concaver concaver = new Concaver();
	
	private final GeometryFactory geofac;

	private ArrayList<Feature> features;

	private FeatureType ft;
	
	public EvacZonesAssigner(final CoordinateReferenceSystem targetCRS, final String shapefile, final String eventsfile, final NetworkLayer network){
		this.targetCRS = targetCRS;
		this.shapefile = shapefile;
		this.eventsfile = eventsfile;
		this.network = network;
		this.geofac = new GeometryFactory();
		try {
			initFeatureCollection();
		} catch (FactoryRegistryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initColorMap();
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
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
//		AttributeType strId = AttributeTypeFactory.newAttributeType("sign", String.class);
//		AttributeType count = AttributeTypeFactory.newAttributeType("count", Integer.class);
//		AttributeType angle1 = AttributeTypeFactory.newAttributeType("angle1", Double.class);
//		AttributeType angle2 = AttributeTypeFactory.newAttributeType("angle2", Double.class);
//		AttributeType angle3 = AttributeTypeFactory.newAttributeType("angle3", Double.class);
//		AttributeType shortest = AttributeTypeFactory.newAttributeType("shortest", Integer.class);
//		AttributeType current = AttributeTypeFactory.newAttributeType("current", Integer.class);
//		AttributeType deviance = AttributeTypeFactory.newAttributeType("diffsc", Integer.class);
		this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id}, "Shape");
	}

	private void run() {



		HashMap<String,ArrayList<Point>> exits = new HashMap<String, ArrayList<Point>>();
		
		Events events = new Events();
		DestinationDependentColorizer ddc = new DestinationDependentColorizer();
		events.addHandler(ddc);
		events.addHandler(this);
		new EventsReaderTXTv1(events).readFile(this.eventsfile);

		for (String key : this.dests.keySet()) {
			
			
			String eId = "el" + ddc.getColor(key);
			Link link = this.network.getLink(eId);
			
			if (link == null) {
				continue;
			}
			
			
			String c = getColor(ddc.getColor(key));
			Point p = this.dests.get(key);

			ArrayList<Point> list = exits.get(c);
			if (list == null) {
				list = new ArrayList<Point>();
				exits.put(c, list);
			}
			list.add(p);
			
			
		}
		
		ArrayList<Geometry> hulls = new ArrayList<Geometry>();
		for (Map.Entry<String, ArrayList<Point>> e : exits.entrySet()) {
			ArrayList<Point> l = e.getValue();
			Point [] points = new Point [l.size()];
			for (int i = 0; i < l.size(); i++) {
				points[i] = l.get(i);
			}
			MultiPoint mp = this.geofac.createMultiPoint(points);
			Geometry g = mp.convexHull();
			
			
			Polygon concave = this.concaver.getConcaveHull(mp);

			if (concave instanceof Polygon) {
				try {
					this.features.add(this.ft.create(new Object [] {concave,Integer.parseInt(e.getKey())}));
				} catch (IllegalAttributeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			hulls.add(g);
		}
		try {
			ShapeFileWriter.writeGeometries(this.features, this.shapefile);
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

		
		
	public static void main(final String [] args) {
		
		if (args.length != 1) {
			throw new RuntimeException("wrong number of arguments! Pleas run DistanceAnalysis config.xml shapefile.shp" );
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});

		}
		
		String shapefile = "../outputs/tmp/shape.shp";

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");
		
		String eventsfile = Gbl.getConfig().events().getInputFile();

		final CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);

		new EvacZonesAssigner(targetCRS,shapefile,eventsfile,network).run();
	}


}
