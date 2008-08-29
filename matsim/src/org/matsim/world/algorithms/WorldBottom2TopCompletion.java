/* *********************************************************************** *
 * project: org.matsim.*
 * WorldBottom2TopCompletion.java
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

package org.matsim.world.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappingRule;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class WorldBottom2TopCompletion {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(WorldBottom2TopCompletion.class);
	
	private final Set<String> excludingLinkTypes;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldBottom2TopCompletion() {
		this(new HashSet<String>());
	}
	
	public WorldBottom2TopCompletion(Set<String> excludingLinkTypes) {
		this.excludingLinkTypes = excludingLinkTypes;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void mapNetFac(Facilities up_facilities, NetworkLayer down_network) {
		Iterator<? extends Location> f_it = up_facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility up_f = (Facility)f_it.next();
			Link down_link = down_network.getNearestRightEntryLink(up_f.getCenter());
			up_f.addDownMapping(down_link);
			down_link.addUpMapping(up_f);
		}
	}
	
	private final boolean completeNetFacMapping(final MappingRule m) {
		Facilities up_facilities = (Facilities)m.getUpLayer();
		NetworkLayer down_network = (NetworkLayer)m.getDownLayer();

		// get all links from the network with specified link types
		ArrayList<Link> linksRemoved = new ArrayList<Link>();
		for (Link l : down_network.getLinks().values()) { if (this.excludingLinkTypes.contains(l.getType())) { linksRemoved.add(l); } }

		if (linksRemoved.size() == down_network.getLinks().size()) {
			StringBuffer str = new StringBuffer();
			for (String s : this.excludingLinkTypes) { str.append(s); str.append(","); }
			log.warn("No link will be left for the given link types ("+str+"). Therefore, completing the network-facility mapping with the whole network.");

			// find and assign the nearest (right entry) link of all links to each facility
			this.mapNetFac(up_facilities,down_network);
		}
		else {
			// remove all links from the network with specified link types
			for (Link l : linksRemoved) { down_network.removeLink(l); }
			
			// also remove all nodes which have now no incident links
			ArrayList<Node> nodesRemoved = new ArrayList<Node>();
			for (Node n : down_network.getNodes().values()) { if (n.getIncidentLinks().isEmpty()) { nodesRemoved.add(n); } }
			for (Node n : nodesRemoved) { down_network.removeNode(n); }

			// find and assign the nearest (right entry) link of the remaining links to each facility
			this.mapNetFac(up_facilities,down_network);
			
			// restore the removed nodes
			for (Node n : nodesRemoved) {
				down_network.createNode(n.getId(),n.getCoord(),n.getType());
			}

			// restore the removed links
			for (Link l : linksRemoved) {
				down_network.createLink(l.getId(),l.getFromNode(),l.getToNode(),
				    l.getLength(),l.getFreespeed(Time.UNDEFINED_TIME),
				    l.getCapacity(Time.UNDEFINED_TIME),l.getLanes(Time.UNDEFINED_TIME),
				    l.getOrigId(),l.getType());
			}
		}
		return true;
	}

	private final boolean completeNetZoneMapping(final MappingRule m) {
		log.warn("[completeNetZoneMapping()] TODO: No mapping will be created for rule=" + m);
		return true;
	}

	private final boolean completeFacZoneMapping(final MappingRule m) {
		// Iterates through ALL zones and ALL facilities. JH
		Facilities down_facilities = (Facilities)m.getDownLayer();
		ZoneLayer up_zones = (ZoneLayer)m.getUpLayer();
		Iterator<? extends Location> f_it = down_facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility down_f = (Facility)f_it.next();
			ArrayList<Zone> zones = new ArrayList<Zone>();
			Iterator<? extends Location> z_it = up_zones.getLocations().values().iterator();
			while(z_it.hasNext()){
				Zone up_zone = (Zone)z_it.next();
				if(up_zone.contains(down_f.getCenter())){
					zones.add(up_zone);
				}
			}
			if(zones.isEmpty()){
				log.warn("[completeFacZoneMapping()] No Zone found for "+ down_f);
			}
			else {
				Zone zone = zones.get(MatsimRandom.random.nextInt(zones.size()));
				down_f.addUpMapping(zone);
				zone.addDownMapping(down_f);
			}
		}
		return true;
	}

	private final boolean completeZoneZoneMapping(final MappingRule m) {
		return true;
	}

	//////////////////////////////////////////////////////////////////////

	private final boolean completeMapping(final MappingRule m) {
		Layer up_layer = m.getUpLayer();
		Layer down_layer = m.getDownLayer();
		if (down_layer instanceof NetworkLayer) {
			if (up_layer instanceof Facilities) { return this.completeNetFacMapping(m); }
			else if (up_layer instanceof ZoneLayer) { return this.completeNetZoneMapping(m); }
			else { Gbl.errorMsg("This should never happen!"); }
		}
		else if (down_layer instanceof Facilities) {
			if (up_layer instanceof ZoneLayer) {
				return this.completeFacZoneMapping(m);
			}
			Gbl.errorMsg("This should never happen!");
		}
		else if (down_layer instanceof ZoneLayer) {
			if (up_layer instanceof ZoneLayer) {
				return this.completeZoneZoneMapping(m);
			}
			Gbl.errorMsg("This should never happen!");
		}
		else { Gbl.errorMsg("That's very weird!!!"); }
		return false;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("    running " + this.getClass().getName() + " module...");

		world.complete();

		int nof_layers = world.getLayers().size();
		if (nof_layers == 0) { log.info("      nof_layers=0: Nothing to do."); }
		else if (nof_layers == 1) { log.info("      nof_layers=1: Nothing to do."); }
		else {
			Layer l = world.getBottomLayer();
			while (l.getUpRule() != null) {
				MappingRule m = l.getUpRule();
				boolean ok = this.completeMapping(m);
				if (!ok) { Gbl.errorMsg("m=" + m + ": completion was not successful!"); }
				l = m.getUpLayer();
			}
		}

		log.info("    done.");
	}
}
