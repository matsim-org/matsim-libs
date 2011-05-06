/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkClearanceAnalysis.java
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.helper.GTH;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class NetworkClearanceAnalysis {

	private static final Logger log = Logger.getLogger(NetworkClearanceAnalysis.class);

//	private static final String INPUT_BASE="../../arbeit/svn/shared-svn/runs/";
	private static final String INPUT_BASE="../../outputs/";
	private final String ev2;
	private final String ev1;
	private final NetworkImpl network;
	private final CoordinateReferenceSystem crs;
	private final String outfile;

//	private ArrayList<Polygon> polygons;
	private final GeometryFactory geofac;

	private ArrayList<Feature> features;

	private FeatureType ftRunCompare;

	final static Envelope ENVELOPE = new Envelope(648815,655804,9888424,9902468);
	final static double LENGTH = 500;
	private final QuadTree<PolygonInfo> polygons1 = new QuadTree<PolygonInfo>(ENVELOPE.getMinX(),ENVELOPE.getMinY(),ENVELOPE.getMaxX(),ENVELOPE.getMaxY());
	private final QuadTree<PolygonInfo> polygons2 = new QuadTree<PolygonInfo>(ENVELOPE.getMinX(),ENVELOPE.getMinY(),ENVELOPE.getMaxX(),ENVELOPE.getMaxY());
	private final Map<String,PolygonInfo> linkMapping1 = new HashMap<String,PolygonInfo>();
	private final Map<String,PolygonInfo> linkMapping2 = new HashMap<String,PolygonInfo>();

	public NetworkClearanceAnalysis(final String eventsFile1, final String eventsFile2,
			final NetworkImpl network, final String outfile, final CoordinateReferenceSystem crs) {
		this.ev1 = eventsFile1;
		this.ev2 = eventsFile2;
		this.network = network;
		this.outfile = outfile;
		this.crs = crs;

		this.geofac = new GeometryFactory();
		initFeatures();
	}

	private void initFeatures() {
		this.features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.crs);
		AttributeType tt1 = AttributeTypeFactory.newAttributeType("TT1", Double.class);
		AttributeType tt2 = AttributeTypeFactory.newAttributeType("TT2", Double.class);
		AttributeType tt1DiffTt2 = AttributeTypeFactory.newAttributeType("tt1DiffTt2", Double.class);
		try {
			this.ftRunCompare = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, tt1,tt2,tt1DiffTt2}, "gridShape");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}


	}

	private void run() {
		createPolygons();
		classifyLinks();
		readEvents();
		iteratePolygons();
		writeFeatures();

	}



	private void writeFeatures() {
		ShapeFileWriter.writeGeometries(this.features, this.outfile);

	}

	private void iteratePolygons() {
		for (PolygonInfo p1 : this.polygons1.values()) {
			PolygonInfo p2 = this.polygons2.get(p1.p.getCentroid().getX(), p1.p.getCentroid().getY());
			double t1t2 = p1.clearanceTime - p2.clearanceTime;
			try {
				this.features.add(this.ftRunCompare.create(new Object[]{p1.p,p1.clearanceTime,p2.clearanceTime,t1t2}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}

		}

	}

	private void readEvents() {
		EventsManager events1 = (EventsManager) EventsUtils.createEventsManager();
		EventsManager events2 = (EventsManager) EventsUtils.createEventsManager();
		EventsHandler e1 = new EventsHandler(this.linkMapping1);
		EventsHandler e2 = new EventsHandler(this.linkMapping2);
		events1.addHandler(e1);
		events2.addHandler(e2);

		log.info("reading events file 1 from: " + this.ev1);
		new EventsReaderTXTv1(events1).readFile(this.ev1);
		log.info("done.");
		log.info("reading events file 2 from: " + this.ev2);
		new EventsReaderTXTv1(events2).readFile(this.ev2);
		log.info("done.");

	}

	private void classifyLinks() {
		for (Link link : this.network.getLinks().values()) {
			PolygonInfo pi1 = this.polygons1.get(link.getCoord().getX(),link.getCoord().getY());

			this.linkMapping1.put(link.getId().toString(),pi1);
			PolygonInfo pi2 = this.polygons2.get(link.getCoord().getX(),link.getCoord().getY());
			this.linkMapping2.put(link.getId().toString(),pi2);
		}
	}

	private void createPolygons() {
		GTH gth = new GTH(this.geofac);

		for (double x = ENVELOPE.getMinX(); x < ENVELOPE.getMaxX(); x += LENGTH) {
			for (double y = ENVELOPE.getMinY(); y < ENVELOPE.getMaxY(); y+= LENGTH) {
				Polygon p = gth.getSquare(new Coordinate(x,y), LENGTH);
				this.polygons1.put(p.getCentroid().getX(), p.getCentroid().getY(), new PolygonInfo(p));
				this.polygons2.put(p.getCentroid().getX(), p.getCentroid().getY(), new PolygonInfo(p));
			}

		}


	}

	private static class PolygonInfo {
		Polygon p;
		int agents = 0;
		double clearanceTime = 0;
		public PolygonInfo(final Polygon p) {
			this.p = p;
		}
	}

	private static class EventsHandler implements LinkEnterEventHandler, LinkLeaveEventHandler{

		private final Map<String, PolygonInfo> linkMapping;

		public EventsHandler(final Map<String,PolygonInfo> linkMapping) {
			this.linkMapping = linkMapping;
		}

		public void handleEvent(final LinkEnterEvent event) {
			if (event.getLinkId().toString().contains("el")) {
				return;
			}

			PolygonInfo pi = this.linkMapping.get(event.getLinkId().toString());
			pi.agents++;
			if (pi.agents > 1) {
				pi.clearanceTime = 0;
			}
		}

		public void reset(final int iteration) {
			// TODO Auto-generated method stub
		}

		public void handleEvent(final LinkLeaveEvent event) {
			if (event.getLinkId().toString().contains("el")) {
				return;
			}
			PolygonInfo pi = this.linkMapping.get(event.getLinkId().toString());
			pi.agents--;
			if (pi.agents <= 1 && pi.clearanceTime == 0) {
				pi.clearanceTime = event.getTime();
			}
		}

	}

	public static void main (final String [] args) {
//		String eventsFile1 = INPUT_BASE + "run316/output/ITERS/it.201/201.events.txt.gz";
//		String eventsFile2 = INPUT_BASE + "run317/output/ITERS/it.200/200.events.txt.gz";
		String eventsFile1 = INPUT_BASE + "output_100m_so/ITERS/it.0/0.events.txt.gz";
		String eventsFile2 = INPUT_BASE + "output_100m/ITERS/it.0/0.events.txt.gz";
		String network = "../../inputs/networks/padang_net_evac_v20080618.xml";
		String outfile = INPUT_BASE + "output_100m_so/analysis/runComp.shp";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(network);

		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		new NetworkClearanceAnalysis(eventsFile1, eventsFile2, net, outfile, crs).run();
	}




}
