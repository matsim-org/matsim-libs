/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreeRouting.java
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

package playground.gregor.quadtreerouting;

import com.vividsolutions.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD.Quad;
import playground.gregor.sim2d_v4.cgal.TwoDObject;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.events.debug.RectEvent;
import playground.gregor.sim2d_v4.experimental.Dijkstra;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuadTreeRouting {

	//Envelope
	private static final double minX = 0;
	private static final double maxX = 20;
	private static final double minY = 0;
	private static final double maxY = 20;
	private static final Envelope env = new Envelope(minX,maxX,minY,maxY);

	public static void main(String [] args) {

		//Dummy MATSim scenario including dummy network
		Config conf = ConfigUtils.createConfig();
		conf.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(conf);
//		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(Id.create("0", Node.class), new Coord(minX, minY));
		Node n1 = fac.createNode(Id.create("1", Node.class), new Coord(minX, maxY));
		Node n2 = fac.createNode(Id.create("2", Node.class), new Coord(maxX, maxY));
		Node n3 = fac.createNode(Id.create("3", Node.class), new Coord(maxX, minY));
		net.addNode(n0);net.addNode(n1);net.addNode(n2);net.addNode(n3);

		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		em.addHandler(vis);


		//routing algorithm - for now standard Dijkstra could be replaced by A*  in future
		Dijkstra d = new Dijkstra(env);

		//dummy data
		int number = 20;
		for (double time = 0; time <= 1000; time++ ){
			List<XYVxVyEventImpl>  events = createDummyData(MatsimRandom.getRandom().nextInt(21),time);

			List<TwoDObject> objs = new ArrayList<TwoDObject>();
			for (XYVxVyEventImpl e : events) {
				em.processEvent(e); //needed for visualization
				objs.add(e);
			}

			System.out.println("");
			LinearQuadTreeLD quad = new LinearQuadTreeLD(objs, env, em);
			
			//routing
			List<Quad> fromL = quad.query(new Envelope(0,0.1,0,0.1));
			Quad from = fromL.get(0);
			List<Quad> toL = quad.query(new Envelope(19.9,20,19.9,20));
			Quad to = toL.get(0);
			LinkedList<Quad> path = d.computeShortestPath(quad, from, to);
			path.addFirst(from);
			draw(path,em);
//			to.getEnvelope().get
			try {
				Thread.sleep(2000); //to slow it down a bit
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	
	private static void draw(LinkedList<Quad> path, EventsManager em) {
		if (path.size() == 0) {
			return;
		}
		for (Quad q : path) {
			draw(q,em);
		}
		
		Quad start = path.get(0);
		double x = start.getEnvelope().getMinX()+start.getEnvelope().getWidth()/2;
		double y = start.getEnvelope().getMinY()+start.getEnvelope().getHeight()/2;
	
		for (int i = 1; i < path.size(); i++) {
			Quad next = path.get(i);
			LineSegment ls = new LineSegment();
			ls.x0 = x;
			ls.y0 = y;
			x = next.getEnvelope().getMinX()+next.getEnvelope().getWidth()/2;
			y = next.getEnvelope().getMinY()+next.getEnvelope().getHeight()/2;
			ls.x1 = x;
			ls.y1 = y;
			em.processEvent(new LineEvent(0, ls, false, 0, 255,255, 255, 0));
		}
		
	}
	private static void draw(Quad quad, EventsManager em) {
		RectEvent re = new RectEvent(0, quad.getEnvelope().getMinX(), quad.getEnvelope().getMaxY(), quad.getEnvelope().getWidth(), quad.getEnvelope().getHeight(), false);
		em.processEvent(re);
		
	}

	private static List<XYVxVyEventImpl> createDummyData(int number, double time) {
		List<XYVxVyEventImpl> ret = new ArrayList<XYVxVyEventImpl>();
		double width = maxX-minX;
		double height = maxY-minY;
		for (int i = 0; i < number; i++) {
			double x = minX + MatsimRandom.getRandom().nextDouble() * width;
			double y = minY + MatsimRandom.getRandom().nextDouble() * height;
			double vx = 0;
			double vy = 0;
			XYVxVyEventImpl e = new XYVxVyEventImpl(Id.create(i, Person.class), x, y, vx, vy, time);
			ret.add(e);
		}
		return ret;
	}

}
