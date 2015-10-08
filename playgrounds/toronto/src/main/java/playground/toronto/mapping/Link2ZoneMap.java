/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.toronto.mapping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;

public class Link2ZoneMap {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(Link2ZoneMap.class);
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	

	private HashMap<Id, Integer> linkZoneMap; //LinkId, ZoneId
	private HashSet<String> setOfZones; //Used to create OD matrices

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Link2ZoneMap(){
		this.linkZoneMap = new HashMap<Id, Integer>();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private QuadTree<Node> buildCentroidNodeQuadTree(final Map<Id<Node>,? extends Node> nodes, boolean guessZoneNumbers) {
		this.setOfZones = new HashSet<String>();
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<Node> ns = new ArrayList<Node>();
		for (Node n : nodes.values()) {
			try {
				int nid = Integer.parseInt(n.getId().toString());
				if (guessZoneNumbers){
					if (nid < 10000) {
						ns.add(n);
						if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
						if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
						if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
						if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
						this.setOfZones.add(n.getId().toString());
					}
				}else{
					ns.add(n);
					if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
					if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
					if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
					if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
					this.setOfZones.add(n.getId().toString());
				}

			} catch (NumberFormatException e) {
			}
//			if (Integer.parseInt(n.getId().toString()) < 10000) {
//				ns.add(n);
//				if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
//				if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
//				if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
//				if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
//			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		QuadTree<Node> qt = new QuadTree<Node>(minx,miny,maxx,maxy);
		for (Node n : ns) {
			qt.put(n.getCoord().getX(),n.getCoord().getY(),n);
		}
		return qt;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	
	public void run(Network network){
		log.info("Mapping links to zones...");
		log.warn("Zones not explicitly defined! Assuming that all nodes with Ids < 10,000 are zones!");
		
		HashMap<Id, Node> zones = new HashMap<Id, Node>();
		
		QuadTree<Node> qt = buildCentroidNodeQuadTree(network.getNodes(), true);
		
		for (Link l : network.getLinks().values()) {
			Node n = qt.getClosest(l.getCoord().getX(), l.getCoord().getY());
			this.linkZoneMap.put(l.getId(), Integer.parseInt(n.getId().toString()));
		}
		
		log.info("..done.");
		
	}
	
	public void run(final Network network, final Map<Id,? extends Node> zones){
		log.info("Mapping links to zones...");
		
		Network tempNetwork = network;
		for (Node z : zones.values()){
			tempNetwork.addNode(z);
			this.setOfZones.add(z.getId().toString());
		}
		
		QuadTree<Node> qt = buildCentroidNodeQuadTree(tempNetwork.getNodes(), false);
		
		for (Link l : network.getLinks().values()) {
			Node n = qt.getClosest(l.getCoord().getX(), l.getCoord().getY());
			this.linkZoneMap.put(l.getId(), Integer.parseInt(n.getId().toString()));
		}
		
		log.info("..done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////
	
	public void write(String outfile) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
		bw.write("link,zone");
		for (Entry<Id, Integer> e : this.linkZoneMap.entrySet()){
			bw.newLine();
			bw.write(e.getKey() + "," + e.getValue());
		}
		bw.close();
		log.info("Link to zone mapping written to " + outfile);
	}
	
	public int getNumberOfZones(){
		return this.setOfZones.size();
	}
	
	public Integer getZoneOfLink(Id linkId){
		return this.linkZoneMap.get(linkId);
	}

	public Set<String> getSetOfZone() {
		return this.setOfZones;
	}
}
