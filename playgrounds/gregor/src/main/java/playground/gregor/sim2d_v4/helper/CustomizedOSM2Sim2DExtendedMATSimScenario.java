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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.gregor.sim2d_v4.io.Sim2DConfigWriter01;
import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter02;
import playground.gregor.sim2d_v4.io.osmparser.OSM;
import playground.gregor.sim2d_v4.io.osmparser.OSMNode;
import playground.gregor.sim2d_v4.io.osmparser.OSMWay;
import playground.gregor.sim2d_v4.io.osmparser.OSMXMLParser;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Parses an OSM-file for sim2d environments and matsim network, writes those environments to environment network and environment gml files, matsim netowrk, and creates sim2d config and matsim config as well.
 * There is already an OSM Reader in matsim, however that reader deals with networks only and is not extensible to the current needs. 
 *  
 * @author laemmel
 *
 */
public class CustomizedOSM2Sim2DExtendedMATSimScenario {

	private static final Logger log = Logger.getLogger(CustomizedOSM2Sim2DExtendedMATSimScenario.class);

	//ped flow params
	private static final double MAX_DENSITY = 5.4;
	static final double BOTTLENECK_FLOW = 1.3;

	//	private static final String TAG_HIGHWAY = "highway";
	private static final String K_M_TRA_MODE = "m_tra_mode";
	private static final String K_M_TYPE = "m_type";
	private static final String K_M_ONEWAY = "m_oneway";
	private static final String V_M_TYPE_ENV = "sim2d_section";
	private static final String V_M_TYPE_NET = "sim2d_link";
	private static final String K_LEVEL = "level";
	private static final String K_NEIGHBORS = "neighbors";
	private static final String K_OPENINGS = "openings";
	private static final String K_ID = "id";
	private static final String ENV_ID = "env_id";
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

	private static final double THRESHOLD = 1;

	private HashMap<Long, OSMNode> nodes;
	private final Map<CoordinateReferenceSystem,MathTransform> transforms = new HashMap<CoordinateReferenceSystem,MathTransform>();
	private final OSM osm;
	private final Sim2DScenario s2dsc;
	private final Scenario sc;



	/*package*/ CustomizedOSM2Sim2DExtendedMATSimScenario(Scenario sc) {
		this(sc,new OSM());
	}


	/*package*/ CustomizedOSM2Sim2DExtendedMATSimScenario(Scenario sc, OSM osm) {
		this.sc = sc;
		this.s2dsc = (Sim2DScenario) sc.getScenarioElement(Sim2DScenario.ELEMENT_NAME);
		this.osm = osm;
	}


	/*package*/ void processOSMFile(String file) {

		this.osm.addKey(K_M_TYPE);
		this.osm.addKey(K_M_TRA_MODE);
		this.osm.addKey(K_M_ONEWAY);
		OSMXMLParser parser = new OSMXMLParser(this.osm);

		parser.setValidating(false);
		parser.parse(file);
		buildEnvironment(this.osm);
	}


	private void buildEnvironment(OSM osm) {
		this.nodes = new HashMap<>();
		for (OSMNode node : osm.getNodes()) {
			this.nodes.put(node.getId(), node);

		}

		Sim2DEnvironment dummyEnv = new Sim2DEnvironment();
		dummyEnv.setNetwork(this.sc.getNetwork());
		try {
			dummyEnv.setCRS(CRS.decode("EPSG:3395"));
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		}

		Set<String> car = new HashSet<String>();
		car.add("car");
		Set<String> walk = new HashSet<String>();
		walk.add("walk");
		walk.add("car");
		Set<String> walkWalk2d = new HashSet<String>();
		walkWalk2d.add("walk");
		walkWalk2d.add("walk2d");
		walkWalk2d.add("car");
		for (OSMWay way : osm.getWays()) {
			String v = way.getTags().get(K_M_TYPE);
			if (v != null) { //sim2d 
				String strEnvId = way.getTags().get(ENV_ID);
				Sim2DEnvironment env = getOrCreateSim2DEnvironment(Id.create(strEnvId, Sim2DEnvironment.class));
				if (v.equals(V_M_TYPE_ENV)) {
					createSection(way,env);
				} else if (v.equals(V_M_TYPE_NET)) {
					createLinks(way,env,walkWalk2d);
				}
			} else if (way.getTags().get(K_M_TRA_MODE) != null) { //standard matsim 
				createLinks(way,dummyEnv,walk);

			}

		}

		mappLinksToSections();

	}

	private void mappLinksToSections() {
		for (Sim2DEnvironment  env : this.s2dsc.getSim2DEnvironments() ) {
			QuadTree<Section> qt = new QuadTree<Section>(env.getEnvelope().getMinX(),env.getEnvelope().getMinY(),env.getEnvelope().getMaxX(),env.getEnvelope().getMaxY());
			fillQuadtTree(env,qt);
			Network net = env.getEnvironmentNetwork();

			int mapped = 0;

			for (Link l : net.getLinks().values()) {
				Point p = MGC.coord2Point(l.getCoord());
				Section sec = qt.get(p.getX(), p.getY());
				if (!sec.getPolygon().contains(p)) {
					log.info("could not find link section mapping in quadtree using linear search");
					for (Section sec2 : env.getSections().values()) {
						if (sec2.getPolygon().contains(p)){
							//							env.addLinkSectionMapping(l, sec2);
							sec2.addRelatedLinkId(l.getId());
							mapped++;
							break;
						}
					}
				} else {
					//					env.addLinkSectionMapping(l, sec);
					sec.addRelatedLinkId(l.getId());
					mapped++;
				}
			}
			log.warn("there are " + (net.getLinks().size()-mapped) + " unmapped links! This is not necessarily an error");
		}

	}

	private void fillQuadtTree(Sim2DEnvironment env, QuadTree<Section> qt) {
		for (Section sec : env.getSections().values()) {
			Polygon p = sec.getPolygon();
			Coordinate [] coords = p.getExteriorRing().getCoordinates();
			Coordinate old = coords[0];

			qt.put(old.x, old.y, sec);
			for (int i = 1; i < coords.length-1; i++) {
				Coordinate c = coords[i];
				qt.put(c.x, c.y, sec);
				double dist = old.distance(c);
				if (dist > THRESHOLD) {
					double incr = dist/THRESHOLD;
					double dx = (c.x - old.x)/dist;
					double dy = (c.y - old.y)/dist;
					double l = incr;
					while (l < dist) {
						double x = old.x + dx*l;
						double y = old.y + dy*l;
						qt.put(x, y, sec);
						l += incr;
					}
				}
				old = c;
			}

		}


	}

	private void createLinks(OSMWay way, Sim2DEnvironment env, Set<String> modes) {

		Network net = env.getEnvironmentNetwork();
		//		Network net = this.env.getEnvironmentNetwork();
		NetworkFactory fac = net.getFactory();

		String smw = way.getTags().get(TAG_M_WITDTH);
		String sfs = way.getTags().get(TAG_M_FSPEED);
		double fs = Double.parseDouble(sfs);
		double mw = Double.parseDouble(smw);
		double fc = BOTTLENECK_FLOW * mw; 
		double capacity = net.getCapacityPeriod() * fc;

		double cellSize = ((NetworkImpl)net).getEffectiveCellSize();
		double nofLanes = mw * MAX_DENSITY * cellSize/2;

		String IdSuffix = Long.toString(way.getId());
		for (int i = 0; i < way.getNodeRefs().size()-1; i++) {

			Id<Node> nid1 = Id.create(way.getNodeRefs().get(i), Node.class);
			Id<Node> nid2 = Id.create(way.getNodeRefs().get(i+1), Node.class);
			Node n1 = getOrCreateNode(nid1,env);
			Node n2 = getOrCreateNode(nid2,env);

			Id<Link> id0 = Id.create(LINK_ID_PREFIX+i+"_"+ IdSuffix, Link.class);
			Link l0 = fac.createLink(id0, n1, n2);
			l0.setCapacity(capacity);
			l0.setFreespeed(fs);
			l0.setNumberOfLanes(nofLanes);
			double l = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
			l0.setLength(l);
			l0.setAllowedModes(modes);
			net.addLink(l0);
			if (way.getTags().get(K_M_ONEWAY) == null || !way.getTags().get(K_M_ONEWAY).equals("true")) {
				Id<Link> id1 = Id.create(LINK_ID_PREFIX+i+"_rev_"+ IdSuffix, Link.class);
				Link l1 = fac.createLink(id1, n2, n1);
				l1.setCapacity(capacity);
				l1.setFreespeed(fs);
				l1.setNumberOfLanes(nofLanes);
				l1.setLength(l);
				l1.setAllowedModes(modes);
				net.addLink(l1);
			} 
		}

	}

	private Sim2DEnvironment getOrCreateSim2DEnvironment(Id<Sim2DEnvironment> id) {
		Sim2DEnvironment env = this.s2dsc.getSim2DEnvironment(id);
		if (env == null) {
			env = new Sim2DEnvironment();
			env.setEnvelope(new Envelope());
			env.setId(id);
			try {
				env.setCRS(CRS.decode("EPSG:3395"));
			} catch (NoSuchAuthorityCodeException e) {
				throw new IllegalArgumentException(e);
			} catch (FactoryException e) {
				throw new IllegalArgumentException(e);
			}
			env.setNetwork(NetworkImpl.createNetwork());
			this.s2dsc.addSim2DEnvironment(env);
		}
		return env;
	}


	private Node getOrCreateNode(Id<Node> nid1, Sim2DEnvironment env) {
		Node n1 = env.getEnvironmentNetwork().getNodes().get(nid1);
		MathTransform transform = this.transforms.get(env.getCRS());
		if (transform == null) {
			try {
				transform = CRS.findMathTransform(osmCrs, env.getCRS());
			} catch (FactoryException e) {
				throw new IllegalArgumentException(e);
			}
		}
		if (n1 == null) {
			long longNid1 = Long.parseLong(nid1.toString());
			OSMNode node = this.nodes.get(longNid1);
			Coordinate c = new Coordinate(node.getLon(),node.getLat());
			try {
				JTS.transform(c, c, transform);
			} catch (TransformException e) {
				throw new IllegalArgumentException(e);
			}
			Coord cc = MGC.coordinate2Coord(c);
			n1 = env.getEnvironmentNetwork().getFactory().createNode(nid1, cc);
			env.getEnvironmentNetwork().addNode(n1);
		}

		return n1;
	}

	private void createSection(OSMWay way, Sim2DEnvironment env) {
		MathTransform transform = this.transforms.get(env.getCRS());
		if (transform == null) {
			try {
				transform = CRS.findMathTransform(osmCrs, env.getCRS());
			} catch (FactoryException e) {
				throw new IllegalArgumentException(e);
			}
		}


		Coordinate[] coords = new Coordinate[way.getNodeRefs().size()];
		for (int i = 0; i < way.getNodeRefs().size(); i++) {
			long ref = way.getNodeRefs().get(i);
			OSMNode node = this.nodes.get(ref);
			coords[i] = new Coordinate(node.getLon(),node.getLat());
		}

		LinearRing lr = geofac.createLinearRing(coords);
		try {
			lr = (LinearRing) JTS.transform(lr, transform);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}

		Id<Section> id = Id.create(way.getTags().get(K_ID), Section.class);
		Polygon p = geofac.createPolygon(lr, null);
		for (Coordinate bc : p.getBoundary().getCoordinates()) {
			env.getEnvelope().expandToInclude(bc);
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
		Id<Section>[] n = null;
		if (nString != null) {
			String[] nStringA = StringUtils.explode(nString, ' ');
			n = new Id[nStringA.length];
			for (int i = 0; i < nStringA.length; i++) {
				n[i] = Id.create(nStringA[i], Section.class);
			}
		}

		int l = Integer.parseInt(way.getTags().get(K_LEVEL));
		env.createAndAddSection(id, p, o, n, l);
	}

	public static void main (String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		//		String osmFile = "/Users/laemmel/devel/gr90_sim2d_v4/raw_input_stage3/env.osm";
		//		String inputDir = "/Users/laemmel/devel/gr90_sim2d_v4/input";
		//		String outputDir = "/Users/laemmel/devel/gr90_sim2d_v4/output";

		String osmFile = "/Users/laemmel/devel/gct/osm/map.osm";
		String inputDir = "/Users/laemmel/devel/gct/input";
		String outputDir = "/Users/laemmel/devel/gct/output";

		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario s2dsc = Sim2DScenarioUtils.createSim2dScenario(s2d);

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.71);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, s2dsc);


		CustomizedOSM2Sim2DExtendedMATSimScenario osm2sim2d = new CustomizedOSM2Sim2DExtendedMATSimScenario(sc);
		osm2sim2d.processOSMFile(osmFile);

		//write s2d envs
		for (Sim2DEnvironment env : s2dsc.getSim2DEnvironments()) {
			String envFile = inputDir + "/sim2d_environment_" + env.getId() + ".gml.gz";
			String netFile = inputDir + "/sim2d_network_" + env.getId() + ".xml.gz";
			new Sim2DEnvironmentWriter02(env).write(envFile);
			new NetworkWriter(env.getEnvironmentNetwork()).write(netFile);
			s2d.addSim2DEnvironmentPath(envFile);
			s2d.addSim2DEnvNetworkMapping(envFile, netFile);
		}

		new Sim2DConfigWriter01(s2d).write(inputDir + "/s2d_config.xml");


		c.network().setInputFile(inputDir + "/network.xml.gz");

		//		c.strategy().addParam("Module_1", "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "250");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(500);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);
		
		ActivityParams ticket = new ActivityParams("ticket");
		ticket.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		ticket.setMinimalDuration(49);
		ticket.setClosingTime(49);
		ticket.setEarliestEndTime(49);
		ticket.setLatestStartTime(49);
		ticket.setOpeningTime(49);


		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(30*60); // dito
		post.setMinimalDuration(10*60);
		post.setClosingTime(14*3600+30*60);
		post.setEarliestEndTime(11*3600+10*60);
		post.setLatestStartTime(14*3600);
		post.setOpeningTime(11*3600);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(ticket);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);


		QSimConfigGroup qsim = c.qsim();
		qsim.setEndTime(2*3600);
		c.controler().setMobsim("hybridQ2D");

		c.global().setCoordinateSystem("EPSG:3395");

		new ConfigWriter(c).write(inputDir+ "/config.xml");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());



	}

}
