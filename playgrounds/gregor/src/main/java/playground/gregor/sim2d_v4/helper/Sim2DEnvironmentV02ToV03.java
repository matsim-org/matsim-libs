/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEnvironmentV02ToV03.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.io.Sim2DConfigWriter01;
import playground.gregor.sim2d_v4.io.Sim2DEnvironmentReader02;
import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter03;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DSectionPreprocessor;

import com.vividsolutions.jts.geom.Envelope;

public class Sim2DEnvironmentV02ToV03 {
	
	private final static double EPSILON = 0.1;
	
	private final Sim2DScenario sc;

	public Sim2DEnvironmentV02ToV03(Sim2DScenario sc) {
		this.sc = sc;
	
	}
	
	private void run() {
		for (Sim2DEnvironment env : this.sc.getSim2DEnvironments()) {
			Envelope e = env.getEnvelope();
			QuadTree<Opening> qt = new QuadTree<Opening>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
			for (Section s : env.getSections().values()) {
				Id<Node> [] ids = new Id [s.getOpeningSegments().size()];
				s.setOpeningMATSimIds(ids);
				for (int i = 0; i < s.getOpeningSegments().size(); i++) {
					LineSegment l = s.getOpeningSegments().get(i);
					double x = (l.x0+l.x1)/2;
					double y = (l.y0+l.y1)/2;
					Opening o = qt.getClosest(x, y);
					if (o != null) {
						double dx = o.x-x;
						double dy = o.y-y;
						double dist = Math.sqrt(dx*dx+dy*dy);
						if (dist < EPSILON) {
							ids[i] = o.id;
							continue;
						}
					}
					o = new Opening();
					o.x = x;
					o.y = y;
					qt.put(x, y, o);
					o.id = Id.create("gen_opening_ID_"+s.getId()+"."+i, Node.class);
					ids[i] = o.id;
					for (Id<Link> lid : s.getRelatedLinkIds()) {
						Link link = env.getEnvironmentNetwork().getLinks().get(lid);
						if (touches(l,link.getFromNode())) {
							ids[i] = link.getFromNode().getId();
							break;
						}
						if (touches(l,link.getToNode())) {
							ids[i] = link.getToNode().getId();
							break;
						}
					}
					o.id = ids[i];
					
				}
			}
		}
		
	}

	private boolean touches(LineSegment l, Node n) {
		double x = n.getCoord().getX();
		double y = n.getCoord().getY();
		if ( CGAL.isOnVector(x, y, l.x0, l.y0, l.x1, l.y1)) {
			double ll = (l.x0-l.x1)*(l.x0-l.x1)+(l.y0-l.y1)*(l.y0-l.y1);
			double d1 = (l.x0-x)*(l.x0-x)+(l.y0-y)*(l.y0-y);
			if (d1 > ll) {
				return false;
			}
			double d2 = (l.x1-x)*(l.x1-x)+(l.y1-y)*(l.y1-y);
			if (d2 > ll) {
				return false;
			}
			return true;
		}
		return false;
	}

	public static void main(String [] args) {
//		String inputDir = "/Users/laemmel/devel/gct/floorpl/";
//		String confPath = inputDir + "sim2dConfignextgen_floorplan.xml";
		String inputDir = "/Users/laemmel/devel/tjunction/input/";
		
		String confPath = inputDir + "s2d_config.xml";
		Sim2DConfig conf = Sim2DConfigUtils.loadConfig(confPath);
		Sim2DScenario sc = loadSim2DScenario(conf);
		new Sim2DEnvironmentV02ToV03(sc).run();
		
		Sim2DConfig s2d = Sim2DConfigUtils.createConfig();
		
		//write s2d envs
		for (Sim2DEnvironment env : sc.getSim2DEnvironments()) {
			String envFile = inputDir + "/sim2d_environment_" + env.getId() + "_v0.3.gml.gz";
			new Sim2DEnvironmentWriter03(env).write(envFile);
			s2d.addSim2DEnvironmentPath(envFile);
		}

		new Sim2DConfigWriter01(s2d).write(inputDir + "/s2d_config_v0.3.xml");
		
		System.out.println(sc);
		
	}

	public static  Sim2DScenario loadSim2DScenario(Sim2DConfig conf) {
		Sim2DScenario scenario = new Sim2DScenario(conf);
		for (String envPath : conf.getSim2DEnvironmentPaths()){
			Sim2DEnvironment env = new Sim2DEnvironment();
			new Sim2DEnvironmentReader02(env, false).readFile(envPath);
			scenario.addSim2DEnvironment(env);
			Sim2DSectionPreprocessor.preprocessSections(env);
			String netPath = conf.getNetworkPath(envPath);
			if (netPath != null) {
				Config c = ConfigUtils.createConfig();
				Scenario sc = ScenarioUtils.createScenario(c);
				new MatsimNetworkReader(sc).readFile(netPath);
				Network net = sc.getNetwork();
				env.setNetwork(net);
			}
		}
		return scenario;
	}

	private static class Opening {
		Id<Node> id;
		double x;
		double y;
	}
	
}
