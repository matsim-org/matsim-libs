/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DExtendedMATSimScenario2CustomizedOSM.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class Sim2DExtendedMATSimScenario2CustomizedOSM {

	private static final String XML_PREAMBLE = "<?xml version='1.0' encoding='UTF-8'?>\n";
	private static final String OSM = "<osm version='0.6' upload='false' generator='MATSim'>\n";
	private static final String OSM_ = "</osm>";



	private final Map<Id,OSMNode> nodes = new HashMap<Id,OSMNode>();
	private final Map<Coordinate,OSMNode> coordinates = new HashMap<Coordinate,OSMNode>();
	private final Map<Id,OSMWay> ways = new HashMap<Id,OSMWay>();

	
	private int nodeId = -1;

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
	private final MathTransform transform;

	private final Scenario sc;
	private final Sim2DScenario sim2dSc;
	private final String osmFile;

	public Sim2DExtendedMATSimScenario2CustomizedOSM(Scenario sc,
			Sim2DScenario sim2dsc, String osmFile) {
		this.sc = sc;
		this.sim2dSc = sim2dsc;
		this.osmFile = osmFile;

		Exception ee;
		try {
			CoordinateReferenceSystem crs = CRS.decode(sc.getConfig().global().getCoordinateSystem());
			this.transform = CRS.findMathTransform(crs,osmCrs);
			return;
		} catch (NoSuchAuthorityCodeException e) {
			ee = e;
		} catch (FactoryException e) {
			ee = e;
		}
		throw new RuntimeException("Could not instanciate because of:" + ee);
	}

	public static void main(String [] args) {
		String osmFile = "/Users/laemmel/devel/hhw_hybrid/input/map.osm";
		String inputDir = "/Users/laemmel/devel/hhw_hybrid/input";
		String configFile = inputDir + "/config.xml";
		String s2dConfigFile = inputDir + "/s2d_config_v0.3.xml";

		Sim2DConfig sim2dc = Sim2DConfigUtils.loadConfig(s2dConfigFile);
		Sim2DScenario sim2dsc = Sim2DScenarioUtils.loadSim2DScenario(sim2dc);
		Config c = ConfigUtils.loadConfig(configFile);
		Scenario sc = ScenarioUtils.loadScenario(c);
		sim2dsc.connect(sc);

		try {
			new Sim2DExtendedMATSimScenario2CustomizedOSM(sc,sim2dsc,osmFile).run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private void run() throws IOException, TransformException {
		prepare();
		BufferedWriter bf = new BufferedWriter(new FileWriter(new File(this.osmFile)));
		bf.append(XML_PREAMBLE);
		bf.append(OSM);
		writeNodes(bf);
		writeWays(bf);
		//		writeRelations(bf);
		bf.append(OSM_);
		bf.close();


	}

	private void prepare() throws TransformException {
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			OSMNode on = new OSMNode();
			on.osmId = this.nodeId--;
			on.matsimId = n.getId().toString();
			Coordinate osmCoord = new Coordinate();
			Coordinate x = JTS.transform(new Coordinate(n.getCoord().getX(),n.getCoord().getY()), osmCoord, this.transform);
			on.lon = x.x;
			on.lat = x.y;
			this.nodes.put(n.getId(), on);
		}

		Set<String> handled = new HashSet<String>();

		for (Sim2DEnvironment e : this.sim2dSc.getSim2DEnvironments()){
			for ( Link l : e.getEnvironmentNetwork().getLinks().values()) {
				String hashRev = l.getToNode().toString() + "___" + l.getFromNode().toString() ;
				if (handled.contains(hashRev)) {
					continue;
				}

				OSMWay way = new OSMWay();
				way.is2d = true;
				way.osmId = this.nodeId--;
				way.matsimId = l.getId().toString();
				way.nodes.add(this.nodes.get(l.getFromNode().getId()).osmId);
				way.nodes.add(this.nodes.get(l.getToNode().getId()).osmId);
				way.envId = e.getId().toString();
				way.mFspeed = l.getFreespeed();
				way.mTraMode = "pedestrian";
				way.mType = "sim2d_link";
				way.mWidth = (l.getCapacity()/CustomizedOSM2Sim2DExtendedMATSimScenario.BOTTLENECK_FLOW)/e.getEnvironmentNetwork().getCapacityPeriod();//l.getNumberOfLanes()*e.getEnvironmentNetwork().getEffectiveLaneWidth();
				this.ways.put(l.getId(), way);

				String hash = l.getFromNode().toString() + "___" + l.getToNode().toString() ;
				handled.add(hash);
			}
			
		}
		for (Link l : this.sc.getNetwork().getLinks().values()) {
			String hashRev = l.getToNode().toString() + "___" + l.getFromNode().toString() ;
			String hash = l.getFromNode().toString() + "___" + l.getToNode().toString() ;
			if (handled.contains(hashRev) || handled.contains(hash)) {
				continue;
			}

			OSMWay way = new OSMWay();
			way.osmId = this.nodeId--;
			way.matsimId = l.getId().toString();
			way.nodes.add(this.nodes.get(l.getFromNode().getId()).osmId);
			way.nodes.add(this.nodes.get(l.getToNode().getId()).osmId);
			way.mFspeed = l.getFreespeed();
			way.mTraMode = "pedestrian";
			way.mWidth = (l.getCapacity()/CustomizedOSM2Sim2DExtendedMATSimScenario.BOTTLENECK_FLOW)/this.sc.getNetwork().getCapacityPeriod();//l.getNumberOfLanes()*e.getEnvironmentNetwork().getEffectiveLaneWidth();
			this.ways.put(l.getId(), way);

			handled.add(hash);			
		}
		
		
		for (Sim2DEnvironment e : this.sim2dSc.getSim2DEnvironments()){
			for (Section sec : e.getSections().values()) {
				for (Coordinate c : sec.getPolygon().getExteriorRing().getCoordinates()) {
					if (this.coordinates.get(c) != null) {
						continue;
					}
					OSMNode n = new OSMNode();
					Coordinate x = JTS.transform(c, new Coordinate(), this.transform);
					n.lon = x.x;
					n.lat = x.y;
					n.osmId = this.nodeId--;
					this.coordinates.put(c, n);
				}
			}
			
		}
		
		for (Sim2DEnvironment e : this.sim2dSc.getSim2DEnvironments()){
			for (Section sec : e.getSections().values()) {
				OSMWay w = new OSMWay();
				w.envId = e.getId().toString();
				w.osmId = this.nodeId--;
				w.is2d = true;
				w.is2dSection = true;
				w.matsimId = sec.getId().toString();
				w.neighbors = sec.getNeighbors();
				w.openings = sec.getOpenings();
				for (Coordinate c : sec.getPolygon().getExteriorRing().getCoordinates()) {
					OSMNode n = this.coordinates.get(c);
					w.nodes.add(n.osmId);
				}
				this.ways.put(sec.getId(), w);
			}
			
		}


	}

	private void writeWays(BufferedWriter bf) throws IOException {
		for (OSMWay w : this.ways.values()) {
			bf.append("<way id='");
			bf.append(Integer.toString(w.osmId));
			bf.append("' action='modify' visible='true'>\n");
			for (Integer n : w.nodes) {
				bf.append("\t<nd ref='");
				bf.append(n.toString());
				bf.append("' />\n");
			}
			if (w.is2d && !w.is2dSection) {
				bf.append("\t<tag k='env_id' v='");
				bf.append(w.envId);
				bf.append("' />\n");
				bf.append("\t<tag k='m_type' v='sim2d_link' />\n");
			}

			if (w.is2d && w.is2dSection) {
				bf.append("\t<tag k='env_id' v='");
				bf.append(w.envId);
				bf.append("' />\n");
				bf.append("\t<tag k='m_type' v='sim2d_section' />\n");
				bf.append("\t<tag k='level' v='0' />\n");

				bf.append("\t<tag k='neighbors' v='");
				for ( Id n : w.neighbors) {
					bf.append(n.toString() + " ");
				}
				bf.append("' />\n");
				bf.append("\t<tag k='openings' v='");
				for ( int n : w.openings) {
					bf.append(Integer.toString(n) + " ");
				}
				bf.append("' />\n");
			} else {
				bf.append("\t<tag k='m_fspeed' v='");
				bf.append(Double.toString(w.mFspeed));
				bf.append("' />\n");
				bf.append("\t<tag k='m_tra_mode' v='pedestrian' />\n");
				bf.append("\t<tag k='m_width' v='");
				bf.append(Double.toString(w.mWidth));
				bf.append("' />\n");
			}
			bf.append("\t<tag k='id' v='");
			bf.append(w.matsimId);
			bf.append("' />\n");
			bf.append("</way>\n");
		}

	}

	private void writeNodes(BufferedWriter bf) throws IOException {
		for (OSMNode n : this.nodes.values()) {
			bf.append("<node id='");
			bf.append(Integer.toString(n.osmId));
			bf.append("' visible='true' lat='");
			bf.append(Double.toString(n.lat));
			bf.append("' lon='");
			bf.append(Double.toString(n.lon));
			bf.append("' mid='");
			bf.append(n.matsimId);
			bf.append("' />\n");
		}
		for (OSMNode n : this.coordinates.values()) {
			bf.append("<node id='");
			bf.append(Integer.toString(n.osmId));
			bf.append("' visible='true' lat='");
			bf.append(Double.toString(n.lat));
			bf.append("' lon='");
			bf.append(Double.toString(n.lon));
			bf.append("' mid='");
			bf.append(n.matsimId);
			bf.append("' />\n");
		}

	}

	private static final class OSMNode {
		double lat;
		double lon;
		String matsimId;
		int osmId;
	}

	private static final class OSMWay {
		public int[] openings;
		public Id[] neighbors;
		int osmId;
		String matsimId;
		String envId;
		double mFspeed;
		String mTraMode;
		String mType;
		double mWidth;
		List<Integer> nodes = new ArrayList<Integer>();
		boolean is2d = false;
		boolean is2dSection = false;
		
	}
}
