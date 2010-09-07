/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFromLsFile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.LineString;

public class NetworkFromLsFile {

	private ScenarioImpl sc;
	private HashMap<Id, LineString> lsmp;
	private Map<Id,List<Id>> linkSubLinkMapping = new HashMap<Id, List<Id>>();

	public NetworkFromLsFile(ScenarioImpl scenario, HashMap<Id, LineString> lsmp) {
		this.sc = scenario;
		this.lsmp = lsmp;
		
	}

	public void loadNetwork() {
		
		createNodes();
		createLinks();
		
	}

	public Map<Id,List<Id>> getLinkSubLinkMapping(){
		return this.linkSubLinkMapping;
	}
	
	private void createLinks() {
		NetworkLayer net = (NetworkLayer)this.sc.getNetwork();
		int count = 0;
		
		for (Entry<Id, LineString> e : this.lsmp.entrySet()) {
			LineString ls = e.getValue();
			List<Id> subLinks = new ArrayList<Id>();
			this.linkSubLinkMapping.put(e.getKey(), subLinks);
			for (int i = 1; i < ls.getNumPoints(); i ++) {
				double length = ls.getCoordinateN(i-1).distance(ls.getCoordinateN(i));
				Coord c1 = MGC.coordinate2Coord(ls.getCoordinateN(i-1));
				Coord c2 = MGC.coordinate2Coord(ls.getCoordinateN(i));
				Collection<Node> froms = net.getNearestNodes(c1, 1);
				Collection<Node> tos = net.getNearestNodes(c2, 1);
				if (froms.size() != 1 || tos.size() != 1) {
					throw new RuntimeException();
				}
				IdImpl id1 = new IdImpl(count++);
				IdImpl id2 = new IdImpl(count++);
				subLinks.add(id1);
				subLinks.add(id2);
				net.createAndAddLink(id1, froms.iterator().next(), tos.iterator().next(), length, 1.66, 1, 1);
				net.createAndAddLink(id2, tos.iterator().next(),froms.iterator().next(), length, 1.66, 1, 1);
			}
		}
		
	}

	private void createNodes() {
		NetworkLayer net = (NetworkLayer)this.sc.getNetwork();
		int count = 0;
		for (LineString ls : this.lsmp.values()) {
			for (int i = 0; i < ls.getNumPoints(); i ++) {
				Coord c = MGC.coordinate2Coord(ls.getCoordinateN(i));
				Collection<Node> cs = net.getNearestNodes(c, 1);
				if (cs.size() == 0) {
					net.createAndAddNode(new IdImpl(count++), c);
				}
			}
		}
		
	}

}
