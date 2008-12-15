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
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * Connects locations of neighbor layers.
 * 
 * <p>It connects different neighbor layers according to the layer connection produced by {@link World#complete()}.
 * mappings between two zones will not be touched but the mapping between zone<==>facility, zone<==>link, facility<==>link resp.
 * will be generated.</p>
 * 
 * <p><b>Note:</b> by defining a set of link types that should be excluded from the facility<==>link mapping,
 * the module is able to set the facilities to links, that actually can be accessed, and e.g. highways can be excluded.</p>
 * 
 * @author balmermi
 */
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

	/**
	 * Extracts those links and nodes from the given {@link NetworkLayer network} such
	 * that all links with types given in {@link #excludingLinkTypes} are excluded.
	 * It returns a newly created network. The given one will not be changed.
	 * 
	 * <p><b>Note:</b> The new network that will be returned is not necessarily a 
	 * "cleaned one" (strongly connected digraph), but "empty {@link Node nodes}" are removed.</p>
	 * 
	 * @param network the network that will be the base for created a sub-network
	 * @return copy of the given network without links with type given in {@link #excludingLinkTypes}
	 * and without empty nodes
	 */
	private final NetworkLayer extractSubNetwork(final NetworkLayer network) {
		log.info("  extracting sub network...");
		// get all links from the network with specified link types
		ArrayList<Link> remainingLinks = new ArrayList<Link>();
		for (Link l : network.getLinks().values()) {
			if (!excludingLinkTypes.contains(l.getType())) { remainingLinks.add(l); }
		}
		if (remainingLinks.isEmpty()) {
			StringBuffer str = new StringBuffer();
			for (String s : this.excludingLinkTypes) { str.append(s); str.append(","); }
			log.warn("No link will be left for the given link types ("+str+"). Therefore, connecting the facility<-->link mapping with the whole network.");
			return network;
		}
		NetworkLayer subNetwork = new NetworkLayer();
		// add nodes and links to the subNetwork
		for (Link l : remainingLinks) {
			Node fn = l.getFromNode();
			Node nfn = subNetwork.getNode(fn.getId());
			if (nfn == null) { nfn = subNetwork.createNode(fn.getId(),fn.getCoord()); }

			Node tn = l.getToNode();
			Node ntn = subNetwork.getNode(tn.getId());
			if (ntn == null) { ntn = subNetwork.createNode(tn.getId(),tn.getCoord()); }

			subNetwork.createLink(l.getId(),nfn,ntn,l.getLength(),l.getFreespeed(Time.UNDEFINED_TIME),l.getCapacity(Time.UNDEFINED_TIME),l.getLanes(Time.UNDEFINED_TIME));
		}
		log.info("  done.");
		return subNetwork;
	}
	
	//////////////////////////////////////////////////////////////////////

	/**
	 * Sets the mapping between each {@link Facility facility} of the given {@link Facilities facility layer} and
	 * the {@link Link links} of the {@link NetworkLayer network layer}. The facilities layer should be part of the
	 * given {@link World world}, but the {@link NetworkLayer network} does not have to be necessarily part of the
	 * world. It could also be a sub-network of the network of the {@link World world} as created by the method {@link #extractSubNetwork(NetworkLayer)}.
	 * In both cases the facility layer will be connected with the network in the same world.
	 * 
	 * <p><b>Mapping rule:</b> each {@link Facility facility} of the {@link Facilities facilities layer} will be
	 * connected with <em>exactly</em> one link of the {@link NetworkLayer network layer}. the links of the network will get zero, one or many mappings to
	 * facilities of the facilities layer. (facilities[*]-[1]links)</p>
	 * 
	 * @param facilities a layer of the world
	 * @param network either a layer of the world or a network created with {@link #extractSubNetwork(NetworkLayer)} from a network layer of the world.
	 * @param world the world in which the facility and the network layer will be mapped
	 */
	private final void connect(Facilities facilities, NetworkLayer network, World world) {
		log.info("  connecting facilities with links...");
		for (Facility f : facilities.getFacilities().values()) {
			// remove previous mappings for facility f
			if (!f.removeAllDownMappings()) { throw new RuntimeException("could not remove old factivity<-->link mappings"); }
			// add the nearest right entry link mapping to the facility f
			// note: network could be a temporal copy of the one in the world. Therefore, get the original one.
			Location l = network.getNearestRightEntryLink(f.getCenter());
			l = world.getLayer(NetworkLayer.LAYER_TYPE).getLocation(l.getId());
			if (!world.addMapping(f,l)) { throw new RuntimeException("could not add nearest right entry factivity<-->link mappings"); }
		}
		log.info("  done.");
	}
	
	/**
	 * Sets the mapping between each {@link Zone zone} of the given {@link ZoneLayer zone layer} and
	 * the {@link Facility facilities} of the {@link Facilities facility layer} as part of a {@link World world}.
	 * 
	 * <p><b>Mapping rule:</b> each {@link Facility facility} of the {@link Facilities facilities layer} will be
	 * connected with zero or one zone. A zone will only be assigned if the facility is located within the zone.
	 * If more than one zone exists for which the facility is located in, the first one (smallest zone {@link Id id})
	 * will be chosen. (zones[?]-[*]facilities)</p>
	 * 
	 * @param zones a layer of the world
	 * @param facilities a layer of the world
	 * @param world the world in which the zone and the facility layer will be mapped
	 */
	private final void connect(ZoneLayer zones, Facilities facilities, World world) {
		log.info("  connecting zones with facilities...");
		for (Facility f : facilities.getFacilities().values()) {
			// remove previous mappings for facility f
			if (!f.removeAllUpMappings()) { throw new RuntimeException("could not remove old zone<-->facility mappings"); }
			// add the zone mapping to facility f
			ArrayList<Location> nearestZones = zones.getNearestLocations(f.getCenter());
			if (nearestZones.isEmpty()) { /* facility does not belong to a zone */ }
			else {
				// choose the first of the list (The list is generated via a defined order of the zones,
				// therefore the chosen zone is deterministic). 
				Zone z = (Zone)nearestZones.get(0);
				if (!z.contains(f.getCenter())) { /* f is not located IN any of the nearest zones */ }
				else {
					if (!world.addMapping(z,f)) { throw new RuntimeException("could not add zone<-->facility mapping"); }
				}
			}
		}
		log.info("  done.");
	}

	/**
	 * Sets the mapping between each {@link Zone zone} of the given {@link ZoneLayer zone layer} and
	 * the {@link Link links} of the {@link NetworkLayer network layer} as part of a {@link World world}.
	 * 
	 * <p><b>Mapping rule:</b> each {@link Link link} of the {@link NetworkLayer network layer} will be
	 * connected with zero or one zone. A zone will only be assigned if the center of the link is located within the zone.
	 * If more than one zone exists for which the center of the link is located in, the first one (smallest zone {@link Id id})
	 * will be chosen. (zones[?]-[*]links)</p>
	 * 
	 * @param zones a layer of the world
	 * @param network a layer of the world
	 * @param world the world in which the zone and the network layer will be mapped
	 */
	private final void connect(ZoneLayer zones, NetworkLayer network, World world) {
		log.info("  connecting zones with links...");
		for (Link l : network.getLinks().values()) {
			// remove previous mappings for link l
			if (!l.removeAllUpMappings()) { throw new RuntimeException("could not remove old zone<-->link mappings");  }
			// add the zone mapping to link l
			ArrayList<Location> nearestZones = zones.getNearestLocations(l.getCenter());
			if (nearestZones.isEmpty()) { /* link does not belong to a zone */ }
			else {
				// choose the first of the list (The list is generated via a defined order of the zone,
				// therefore the chosen zone is deterministic). 
				Zone z = (Zone)nearestZones.get(0);
				if (!z.contains(l.getCenter())) { /* link center is not located IN any of the nearest zones */ }
				else {
					if (!world.addMapping(z,l)) { throw new RuntimeException("could not add zone<-->link mapping"); }
				}
			}
		}
		log.info("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("running " + this.getClass().getName() + " module (MATSim-FUSION)...");

		if (world.getLayers().size() > 1) {
			Layer downLayer = world.getBottomLayer();
			while (downLayer.getUpRule() != null) {
				Layer upLayer = downLayer.getUpRule().getUpLayer();
				if (downLayer.getLocations().isEmpty() || upLayer.getLocations().isEmpty()) {
					log.warn("downLayer="+downLayer.getType()+", upLayer="+upLayer.getType()+
					         ": mapping cannot be set since at least one of the layers does not contain any locations. (this may not be fatal)");
				}
				else {
					if ((downLayer instanceof NetworkLayer) && (upLayer instanceof Facilities)) {
						if (excludingLinkTypes.isEmpty()) {
							connect((Facilities)upLayer,(NetworkLayer)downLayer,world);
						}
						else {
							NetworkLayer subNetwork = extractSubNetwork((NetworkLayer)downLayer);
							connect((Facilities)upLayer,subNetwork,world);
						}
					}
					else if ((downLayer instanceof NetworkLayer) && (upLayer instanceof ZoneLayer)) {
						connect((ZoneLayer)upLayer,(NetworkLayer)downLayer,world);
					}
					else if ((downLayer instanceof Facilities) && (upLayer instanceof ZoneLayer)) {
						connect((ZoneLayer)upLayer,(Facilities)downLayer,world);
					}
					else { /* zone<-->zone: nothing to do (keep it as it is) */ }
				}
				downLayer = upLayer;
			}
		}

		log.info("done.");
	}
}
