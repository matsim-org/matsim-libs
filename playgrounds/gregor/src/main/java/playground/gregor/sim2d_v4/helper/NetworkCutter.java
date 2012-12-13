/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCutter.java
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.helper.Algorithms;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class NetworkCutter {

	private static final Logger log = Logger.getLogger(NetworkCutter.class);
	
	
	private static final double THRESHOLD = 1;
	private final Sim2DScenario sc;

	/*package*/ NetworkCutter(Sim2DScenario sc) {
		this.sc = sc;
	}

	/*package*/ void run() {
		for (Sim2DEnvironment env : this.sc.getSim2DEnvironments()) {
			run(env);
		}
	}

	/*package*/ void run(Sim2DEnvironment env) {
		QuadTree<Section> qt = new QuadTree<Section>(env.getEnvelope().getMinX(),env.getEnvelope().getMinY(),env.getEnvelope().getMaxX(),env.getEnvelope().getMaxY());
		fillQuadtTree(env,qt);
		Network net = env.getEnvironmentNetwork();
		NetworkFactory fac = net.getFactory();
		Queue<Link> open = new LinkedList<Link>(net.getLinks().values());
		int id = 0;
		while (open.size() > 0) {
			Link l = open.poll();
			if (l.getLength() < THRESHOLD) {
				log.warn("Link with ID:" + l.getId() + " is shorter than the THRESHOLD (=" + THRESHOLD +"). Therefor the correctniss of this algorithm can not be guaranteed!");
			}
//			log.info(open.size());
			
			Intersect intersection = getFirstIntersection(l,qt);
			if (intersection != null) {
				net.removeLink(l.getId());
				Id id0 = new IdImpl(l.getId()+"a");
				Id id1 = new IdImpl(l.getId()+"b");
				Coordinate c = new Coordinate();
				Algorithms.computeLineIntersection(MGC.coord2Coordinate(l.getFromNode().getCoord()), MGC.coord2Coordinate(l.getToNode().getCoord()), intersection.sec.getPolygon().getCoordinates()[intersection.edge], intersection.sec.getPolygon().getCoordinates()[intersection.edge+1], c);
				double len0 = c.distance(MGC.coord2Coordinate(l.getFromNode().getCoord()));
				double len1 = c.distance(MGC.coord2Coordinate(l.getToNode().getCoord()));
				Id nid = new IdImpl("a"+id++);
				
				Node n = fac.createNode(nid, MGC.coordinate2Coord(c));
				net.addNode(n);
				Link l0 = fac.createLink(id0,l.getFromNode() , n);
				Link l1 = fac.createLink(id1,n , l.getToNode());
				l0.setFreespeed(l.getFreespeed());
				l0.setCapacity(l.getCapacity());
				l0.setNumberOfLanes(l.getNumberOfLanes());
				l0.setLength(len0);
				l1.setFreespeed(l.getFreespeed());
				l1.setCapacity(l.getCapacity());
				l1.setNumberOfLanes(l.getNumberOfLanes());
				l1.setLength(len1);
				
				net.addLink(l0);
				net.addLink(l1);
				open.add(l0);
				open.add(l1);
			}
		}
	}

	private Intersect getFirstIntersection(Link l, QuadTree<Section> qt) {
		Set<Section> assoccSecs01 = new HashSet<Section>();
		Set<Section> assoccSecs02 = new HashSet<Section>();
		Rect rec01 = new Rect(l.getFromNode().getCoord().getX()-l.getLength(),l.getFromNode().getCoord().getY()-l.getLength(),
				l.getFromNode().getCoord().getX()+l.getLength(),l.getFromNode().getCoord().getY()+l.getLength());
		Rect rec02 = new Rect(l.getToNode().getCoord().getX()-l.getLength(),l.getToNode().getCoord().getY()-l.getLength(),
				l.getToNode().getCoord().getX()+l.getLength(),l.getToNode().getCoord().getY()+l.getLength());
		qt.get(rec02, assoccSecs02);
		qt.get(rec01, assoccSecs01);
		assoccSecs01.addAll(assoccSecs02);
		Coordinate c0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
		Coordinate c1 = MGC.coord2Coordinate(l.getToNode().getCoord());
		for (Section sec : assoccSecs01) {
			for (int i = 0; i < sec.getPolygon().getExteriorRing().getCoordinates().length-1;i++){
				Coordinate d0 = sec.getPolygon().getExteriorRing().getCoordinates()[i];
				Coordinate d1 = sec.getPolygon().getExteriorRing().getCoordinates()[i+1];
				double l0 = Algorithms.isLeftOfLine(c0, d0, d1);
				double l1 = Algorithms.isLeftOfLine(c1, d0, d1);
				if (l0 * l1 < -0.01) {
					double m0 = Algorithms.isLeftOfLine(d0, c0, c1);
					double m1 = Algorithms.isLeftOfLine(d1, c0, c1);
					if (m0*m1 < -0.01) {
						Intersect intersect = new Intersect();
						intersect.sec = sec;
						intersect.edge = i;
						return intersect;
					}
				}
				
			}
		}
		return null;
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
	private static final class Intersect {
		Section sec;
		int edge;
		
	}
	
	public static void main(String [] args) {
		Sim2DConfig conf = Sim2DConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d/input/sim2dConfig.xml");
		Sim2DScenario sc = Sim2DScenarioUtils.loadSim2DScenario(conf);
		new NetworkCutter(sc).run();
		String n = sc.getSim2DConfig().getNetworkPath(conf.getSim2DEnvironmentPaths().iterator().next());
		new NetworkWriter(sc.getSim2DEnvironments().iterator().next().getEnvironmentNetwork()).write(n);
//		new Sim2DEnvironmentWriter02(sc.getSim2DEnvironments().iterator().next()).write("/Users/laemmel/devel/burgdorf);
		
	}
}
