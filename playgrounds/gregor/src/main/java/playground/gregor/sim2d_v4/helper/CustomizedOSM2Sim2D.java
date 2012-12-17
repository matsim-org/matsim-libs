/* *********************************************************************** *
 * project: org.matsim.*
 * CustomizedOSM2Sim2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.helper;

import java.util.HashMap;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter02;
import playground.gregor.sim2d_v4.io.osmparser.OSM;
import playground.gregor.sim2d_v4.io.osmparser.OSMNode;
import playground.gregor.sim2d_v4.io.osmparser.OSMWay;
import playground.gregor.sim2d_v4.io.osmparser.OSMXMLParser;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Parses an OSM-file for sim2d environments, writes those environments to matsim network, sim2d config, and environment gml files.
 * There is already an OSM Reader in matsim, however these reader deals with networks only and is not extensible to the current needs. 
 *  
 * @author laemmel
 *
 */
public class CustomizedOSM2Sim2D {

	//ped flow params
	private static final double MAX_DENSITY = 5.4;
	private static final double BOTTLENECK_FLOW = 1.3;
	
	private final Envelope e = new Envelope();
	//	private static final String TAG_HIGHWAY = "highway";
	//	private static final String K_M_TRA_MODE = "m_tra_mode";
	private static final String K_M_TYPE = "m_type";
	private static final String V_M_TYPE_ENV = "sim2d_section";
	private static final String V_M_TYPE_NET = "sim2d_link";
	private static final String K_LEVEL = "level";
	private static final String K_NEIGHBORS = "neighbors";
	private static final String K_OPENINGS = "openings";
	private static final String K_ID = "id";
	private static final String TAG_M_WITDTH = "m_width";
	private static final String TAG_M_FSPEED = "m_fspeed";

	private static final String LINK_ID_PREFIX = "sim2d_";
	
	private static final CoordinateReferenceSystem osmCrs;
	static {
		try {
			osmCrs = CRS.decode("EPSG:4326", true); //WGS84
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		} 
	}
	
	private static final GeometryFactory geofac = new GeometryFactory();

	private final Sim2DEnvironment env;
	private HashMap<Id, OSMNode> nodes;
	private final MathTransform transform;
	private final OSM osm;

	//TODO we need the possibility to have several environments. This means that we parse a scenario and not an environment 
	/*package*/ CustomizedOSM2Sim2D(Sim2DEnvironment env) {
		this(env,new OSM());
	}
	

	/*package*/ CustomizedOSM2Sim2D(Sim2DEnvironment env, OSM osm) {
		this.env = env;
		this.env.setEnvelope(this.e);
		try {
			this.transform = CRS.findMathTransform(osmCrs, env.getCRS());
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
		this.osm = osm;
	}


	/*package*/ void processOSMFile(String file) {
		
		this.osm.addKey(K_M_TYPE);
		OSMXMLParser parser = new OSMXMLParser(this.osm);
		
		parser.setValidating(false);
		parser.parse(file);
		buildEnvironment(this.osm);
	}


	private void buildEnvironment(OSM osm) {
		this.nodes = new HashMap<Id,OSMNode>();
		for (OSMNode node : osm.getNodes()) {
			this.nodes.put(node.getId(), node);
			
		}

		for (OSMWay way : osm.getWays()) {
			String v = way.getTags().get(K_M_TYPE);
			if (v.equals(V_M_TYPE_ENV)) {
				createSection(way);
			} else if (v.equals(V_M_TYPE_NET)) {
				createLinks(way);
			}

		}

	}

	private void createLinks(OSMWay way) {

		Network net = this.env.getEnvironmentNetwork();
		NetworkFactory fac = net.getFactory();
		
		String smw = way.getTags().get(TAG_M_WITDTH);
		String sfs = way.getTags().get(TAG_M_FSPEED);
		double fs = Double.parseDouble(sfs);
		double mw = Double.parseDouble(smw);
		double fc = BOTTLENECK_FLOW * mw; 
		double capacity = net.getCapacityPeriod() * fc;
		
		double cellSize = ((NetworkImpl)net).getEffectiveCellSize();
		double nofLanes = mw * MAX_DENSITY * cellSize;
		
		String IdSuffix = way.getId().toString();
		for (int i = 0; i < way.getNodeRefs().size()-1; i++) {
			
		  Id nid1 = way.getNodeRefs().get(i);
		  Id nid2 = way.getNodeRefs().get(i+1);
		  Node n1 = getOrCreateNode(nid1);
		  Node n2 = getOrCreateNode(nid2);
		  
		  Id id0 = new IdImpl(LINK_ID_PREFIX+i+"_"+ IdSuffix);
		  Link l0 = fac.createLink(id0, n1, n2);
		  l0.setCapacity(capacity);
		  l0.setFreespeed(fs);
		  l0.setNumberOfLanes(nofLanes);
		  double l = ((CoordImpl)n1.getCoord()).calcDistance(n2.getCoord());
		  l0.setLength(l);
		  net.addLink(l0);
		  Id id1 = new IdImpl(LINK_ID_PREFIX+i+"_rev_"+ IdSuffix);
		  Link l1 = fac.createLink(id1, n2, n1);
		  l1.setCapacity(capacity);
		  l1.setFreespeed(fs);
		  l1.setNumberOfLanes(nofLanes);
		  l1.setLength(l);
		  net.addLink(l1);
		
		}
			
	}

	private Node getOrCreateNode(Id nid1) {
		Node n1 = this.env.getEnvironmentNetwork().getNodes().get(nid1);
		  if (n1 == null) {
			  OSMNode node = this.nodes.get(nid1);
			  Coordinate c = new Coordinate(node.getLon(),node.getLat());
			  try {
				JTS.transform(c, c, this.transform);
			} catch (TransformException e) {
				throw new IllegalArgumentException(e);
			}
			 Coord cc = MGC.coordinate2Coord(c);
			 n1 = this.env.getEnvironmentNetwork().getFactory().createNode(nid1, cc);
			 this.env.getEnvironmentNetwork().addNode(n1);
		  }
		
		return n1;
	}

	private void createSection(OSMWay way) {
		Coordinate[] coords = new Coordinate[way.getNodeRefs().size()];
		for (int i = 0; i < way.getNodeRefs().size(); i++) {
			Id ref = way.getNodeRefs().get(i);
			OSMNode node = this.nodes.get(ref);
			coords[i] = new Coordinate(node.getLon(),node.getLat());
		}
		
		LinearRing lr = geofac.createLinearRing(coords);
		try {
			lr = (LinearRing) JTS.transform(lr, this.transform);
		} catch (MismatchedDimensionException e) {
			throw new RuntimeException(e);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
		
		Id id = new IdImpl(way.getTags().get(K_ID));
		Polygon p = geofac.createPolygon(lr, null);
		for (Coordinate bc : p.getBoundary().getCoordinates()) {
			this.e.expandToInclude(bc);
		}
		String oString = way.getTags().get(K_OPENINGS);
		int[] o = null;
		if (oString != null) {
			String [] oStringA = StringUtils.explode(oString, ' ');
			o = new int[oStringA.length];
			for (int i = 0; i < oStringA.length; i++) {
				o[i] = Integer.parseInt(oStringA[i]);
			}
		}
		String nString = way.getTags().get(K_NEIGHBORS);
		Id[] n = null;
		if (nString != null) {
			String[] nStringA = StringUtils.explode(nString, ' ');
			n = new Id[nStringA.length];
			for (int i = 0; i < nStringA.length; i++) {
				n[i] = new IdImpl(nStringA[i]);
			}
		}
		
		int l = Integer.parseInt(way.getTags().get(K_LEVEL));
		this.env.createAndAddSection(id, p, o, n, l);
	}

	public static void main (String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		String osmFile = "/Users/laemmel/devel/burgdorf2d/osm/sim2d.osm";
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setCRS(CRS.decode("EPSG:3395"));
		env.setNetwork(NetworkImpl.createNetwork());
		CustomizedOSM2Sim2D osm2sim2d = new CustomizedOSM2Sim2D(env);
		osm2sim2d.processOSMFile(osmFile);
		
		new Sim2DEnvironmentWriter02(env).write("/Users/laemmel/devel/burgdorf2d/input/sim2dEnv_0.gml.gz");
		new NetworkWriter(env.getEnvironmentNetwork()).write("/Users/laemmel/devel/burgdorf2d/input/network2d_0.xml");
	}

}
