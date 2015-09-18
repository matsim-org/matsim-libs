/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEnvironmentNetworkBuilder.java
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

package playground.gregor.sim2d_v4.scenario;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;

import org.matsim.core.utils.geometry.CoordUtils;
import playground.gregor.sim2d_v4.cgal.LineSegment;

public abstract class Sim2DEnvironmentNetworkBuilder {
	
	private static double FLOPW_CAP_PER_METER_WIDTH = 1.2;
	
	
	public static void buildAndSetEnvironmentNetwork(Sim2DEnvironment env) {
		NetworkImpl net = NetworkImpl.createNetwork();
		NetworkFactoryImpl fac = net.getFactory();
		env.setNetwork(net);
		net.setCapacityPeriod(1);
		net.setEffectiveCellSize(.26);
		net.setEffectiveLaneWidth(.71);
		
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		modes.add("walk");
		modes.add("walk2d");
		
		for (Section s : env.getSections().values()) {
			for (int i = 0; i < s.getOpeningSegments().size(); i++) {
				Id<Node> id = s.getOpeningsIds()[i];
				Node node = net.getNodes().get(id);
				if (node == null) {
					LineSegment seg = s.getOpeningSegments().get(i);
					double x = (seg.x0+seg.x1)/2;
					double y = (seg.y0+seg.y1)/2;
					node = fac.createNode(id, new Coord(x, y));
					net.addNode(node);;
				}
			}
		}
		
		for (Section s : env.getSections().values()) {
			for (int i = 0; i < s.getOpeningSegments().size(); i++) {
				Id<Node> fromId = s.getOpeningsIds()[i];
				for (int j = 0; j < s.getOpeningSegments().size(); j++) {
					if (j == i) {
						continue;
					}
					Id<Node> toId = s.getOpeningsIds()[j];
					Node from = net.getNodes().get(fromId);
					Node to = net.getNodes().get(toId);
					Id<Link> lId = Id.create(fromId.toString() + "-->"+toId.toString(), Link.class);
					Link l = fac.createLink(lId, from, to);
					double dist = CoordUtils.calcDistance(from.getCoord(), to.getCoord());
					l.setLength(dist);
					l.setFreespeed(1.34);
					LineSegment seg = s.getOpeningSegments().get(j);
					double dx = seg.x0-seg.x1;
					double dy = seg.y0-seg.y1;
					double width = Math.sqrt(dx*dx+dy*dy);
					double lanes = width/net.getEffectiveLaneWidth();
					double cap = width*FLOPW_CAP_PER_METER_WIDTH;
					l.setCapacity(cap);
					l.setNumberOfLanes(lanes);
					l.setAllowedModes(modes);
					net.addLink(l);
					s.addRelatedLinkId(lId);
					
				}
			}
		}
		
	}

}
