/* *********************************************************************** *
 * project: org.matsim.*
 * StaticSnapshotGenerator.java
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

package playground.gregor.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.convexer.Concaver;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class StaticSnapshotGenerator implements LinkEnterEventHandler {

	private static final String CVSROOT = "../vsp-cvs";
	private final static Logger log = Logger.getLogger(StaticSnapshotGenerator.class);
	private final static double SNAPSHOT_PERIOD = 15 * 60;

	private final static double MAX_X = 657313;
	private final static double MAX_Y = 9901062;
	private final static double MIN_X = 650623;
	private final static double MIN_Y = 9892835;


	private final NetworkImpl network;
	private final String eventsFile;
	private double oldTime = 3 * 3600;

	private final HashMap<String,Link> agentsOnLink = new HashMap<String, Link>();
	private final String shapeFilePrefix;
	private final GeometryFactory geofac;
	private final CoordinateReferenceSystem targetCRS;
	private FeatureType ft;
	private final Concaver concaver;

	public StaticSnapshotGenerator(final NetworkImpl network, final String inputFile, final String shapeFilePrefix, final CoordinateReferenceSystem targetCRS) {
		this.network = network;
		this.eventsFile = inputFile;
		this.shapeFilePrefix = shapeFilePrefix;
		this.geofac = new GeometryFactory();
		this.targetCRS = targetCRS;

		this.concaver = new Concaver();
		initFeatureCollection();
	}


	private void initFeatureCollection() {

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.targetCRS);
		AttributeType dblTime = AttributeTypeFactory.newAttributeType("dblTime", Double.class);
		AttributeType strgTime = AttributeTypeFactory.newAttributeType("strgTime", String.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, dblTime, strgTime}, "EvacZone");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}

	public void run(){
		this.agentsOnLink.clear();
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(this);
		new EventsReaderTXTv1(events).readFile(this.eventsFile);
	}




	@Override
	public void handleEvent(final LinkEnterEvent event) {
		double time = event.getTime();
		if (time > this.oldTime + SNAPSHOT_PERIOD) {
			this.oldTime = time;
			doSnapshot();
		}
		String agentId = event.getPersonId().toString();
		Link link = this.network.getLinks().get(new IdImpl(event.getLinkId().toString()));
		this.agentsOnLink.put(agentId, link);

	}


	private void doSnapshot() {
		Collection<Feature> ft = new ArrayList<Feature>();

		String time = Time.writeTime(this.oldTime,'-');
		String fileName = this.shapeFilePrefix + time + ".shp";
		MultiPoint mp = getMultiPoint();
		Polygon p = this.concaver.getConcaveHull(mp);
		try {
			ft.add(this.ft.create(new Object [] {p,this.oldTime,time}));
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
		try {
			ShapeFileWriter.writeGeometries(ft, fileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private MultiPoint getMultiPoint() {
		HashSet<Link> links = new HashSet<Link>();
		for (Link link : this.agentsOnLink.values()) {
			if (isInBoundingBox(link.getCoord())) {
				links.add(link);
			}
		}
		Point [] points = new Point[links.size()];
		int c = 0;
		for (Link link : links) {
			points[c++] = MGC.coord2Point(link.getCoord());
		}

		return this.geofac.createMultiPoint(points);
	}


	@Override
	public void reset(final int iteration) {

	}

	public boolean isInBoundingBox(final Coord c) {
		if (c.getX() < MIN_X || c.getX() > MAX_X) {
			return false;
		}

		if (c.getY() < MIN_Y || c.getY() > MAX_Y) {
			return false;
		}

		return true;
	}

	public static void main(final String [] args) {

		if (args.length != 1) {
			throw new RuntimeException("Error using StaticSnapshotGenerator!\n\tUsage:StaticSnapshotGenerator /path/to/configFile");
		}
		ScenarioImpl scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]).getScenario();
		Config config = scenario.getConfig();

		final String shapeFilePrefix =  CVSROOT + "/runs/run314/qgis/evacProgress";

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());

		final CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);

		new StaticSnapshotGenerator(network,null /*filename not specified*/,shapeFilePrefix,targetCRS).run();
	}
}
