/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreePathWalker.java
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD.Quad;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.events.debug.RectEvent;
import playground.gregor.sim2d_v4.experimental.Dijkstra;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

import com.vividsolutions.jts.geom.Envelope;

public class QuadTreePathWalker implements DesiredDirectionCalculator {

	private final DesiredDirectionCalculator dd;
	private final Sim2DAgent agent;
	private final Network net;
	private final EventsManager em;

	public QuadTreePathWalker(Sim2DAgent agent, DesiredDirectionCalculator dd, Network net, EventsManager em) {
		this.dd = dd;
		this.agent = agent;
		this.net = net;
		this.em = em;
	}

	@Override
	public double[] computeDesiredDirection() {
		
		Id linkId = this.agent.getCurrentLinkId();
		Link link = this.net.getLinks().get(linkId);
		Node toNode = link.getToNode();
		double toX = toNode.getCoord().getX();
		double toY = toNode.getCoord().getY();
		double toDx = toX-this.agent.getPos()[0];
		double toDy = toY-this.agent.getPos()[1];
		double sqrDist = toDx*toDx+toDy*toDy;
		if (sqrDist < 4) {
			return this.dd.computeDesiredDirection();
		}
		LinearQuadTreeLD quad = this.agent.getPSec().getQuadTree();
		if (quad == null || quad.getQuads().size() < 4) {
			return this.dd.computeDesiredDirection();
		}

		double fX = this.agent.getPos()[0];
		double fY = this.agent.getPos()[1];
		List<Quad> from = quad.query(new Envelope(fX-0.1,fX+0.1,fY-0.1,fY+0.1));
		List<Quad> to = quad.query(new Envelope(toX-0.1,toX+0.1,toY-0.1,toY+0.1));
		Dijkstra d = new Dijkstra(this.agent.getPSec().getPhysicalEnvironment().getSim2DEnvironment().getEnvelope());
		LinkedList<Quad> path = d.computeShortestPath(quad, from.get(0), to.get(0));
		if (path.size() < 2) {
			return this.dd.computeDesiredDirection();
		}
		
		Quad next = path.get(1);
		Envelope e = next.getEnvelope();
		double nX = e.getMinX()+e.getWidth()/2;
		double nY = e.getMinY()+e.getHeight()/2;
		double dx = nX-fX;
		double dy = nY-fY;
		double length = Math.sqrt(dx*dx+dy*dy);
		dx /= length;
		dy /= length;
//		draw(path);
		return new double[]{dx,dy};
	}

	//DEBUG
	
	private void draw(LinkedList<Quad> path) {
		if (path.size() == 0) {
			return;
		}
		for (Quad q : path) {
			draw(q);
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
			this.em.processEvent(new LineEvent(0, ls, false, 0, 255,255, 255, 0));
		}
		
	}
	private void draw(Quad quad) {
		RectEvent re = new RectEvent(0, quad.getEnvelope().getMinX(), quad.getEnvelope().getMaxY(), quad.getEnvelope().getWidth(), quad.getEnvelope().getHeight(), false);
		this.em.processEvent(re);
		
	}
	
}
