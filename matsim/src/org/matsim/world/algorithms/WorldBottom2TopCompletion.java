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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.MappingRule;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class WorldBottom2TopCompletion extends WorldAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(WorldBottom2TopCompletion.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldBottom2TopCompletion() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean completeNetFacMapping(final MappingRule m) {
		Facilities up_facilities = (Facilities)m.getUpLayer();
		NetworkLayer down_network = (NetworkLayer)m.getDownLayer();
		Iterator<? extends Location> f_it = up_facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility up_f = (Facility)f_it.next();
			Link down_link = down_network.getNearestRightEntryLink(up_f.getCenter());
			up_f.addDownMapping(down_link);
			down_link.addUpMapping(up_f);
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
				Zone zone = zones.get(Gbl.random.nextInt(zones.size()));
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

	@Override
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
