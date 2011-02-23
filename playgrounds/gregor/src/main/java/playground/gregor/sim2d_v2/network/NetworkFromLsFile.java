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
package playground.gregor.sim2d_v2.network;

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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkFromLsFile {

	private final ScenarioImpl sc;
	private final HashMap<Id, LineString> lsmp;

	public NetworkFromLsFile(ScenarioImpl scenario, HashMap<Id, LineString> lsmp) {
		this.sc = scenario;
		this.lsmp = lsmp;

	}

	public void loadNetwork() {

		createNodes();
		createLinks();

	}

	public Map<Id, List<Id>> getLinkSubLinkMapping() {
		return null;
	}

	private void createLinks() {
		NetworkImpl net = this.sc.getNetwork();
		int count = 0;

		for (Entry<Id, LineString> e : this.lsmp.entrySet()) {
			LineString ls = e.getValue();
			Coord c1 = MGC.coordinate2Coord(ls.getStartPoint().getCoordinate());
			Coord c2 = MGC.coordinate2Coord(ls.getEndPoint().getCoordinate());
			Collection<Node> froms = net.getNearestNodes(c1, 0.5);
			Collection<Node> tos = net.getNearestNodes(c2, 0.5);
			if (froms.size() != 1 || tos.size() != 1) {
				throw new RuntimeException();
			}
			IdImpl id1 = new IdImpl(count++);
			IdImpl id2 = new IdImpl(count++);
			net.createAndAddLink(id1, froms.iterator().next(), tos.iterator().next(), ls.getLength(), 1.34, 1, 1);
			net.createAndAddLink(id2, tos.iterator().next(), froms.iterator().next(), ls.getLength(), 1.34, 1, 1);
		}

	}

	private void createNodes() {
		NetworkImpl net = this.sc.getNetwork();
		int count = 0;
		for (LineString ls : this.lsmp.values()) {
			Point ps = ls.getStartPoint();
			Coord c1 = MGC.point2Coord(ps);
			Collection<Node> cs = net.getNearestNodes(c1, 0.5);
			if (cs.size() == 0) {
				net.createAndAddNode(new IdImpl(count++), c1);
			}
			Point pe = ls.getEndPoint();
			Coord c2 = MGC.point2Coord(pe);
			cs = net.getNearestNodes(c2, 0.5);
			if (cs.size() == 0) {
				net.createAndAddNode(new IdImpl(count++), c2);
			}

		}

	}

}
