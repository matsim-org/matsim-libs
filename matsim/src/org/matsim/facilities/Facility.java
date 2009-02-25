/* *********************************************************************** *
 * project: org.matsim.*
 * Facility.java
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

package org.matsim.facilities;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.LocationType;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;
import org.matsim.world.AbstractLocation;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class Facility extends AbstractLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Facility.class);
	
	private final TreeMap<String, Activity> activities = new TreeMap<String, Activity>();
	private String desc = null;
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	protected Facility(final Facilities layer, final Id id, final Coord center) {
		super(layer,id,center);
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public double calcDistance(Coord coord) {
		return this.center.calcDistance(coord);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Activity createActivity(final String type) {
		if (this.activities.containsKey(type)) {
			Gbl.errorMsg(this + "[type=" + type + " already exists]");
		}
		String type2 = type.intern();
		Activity a = new Activity(type2, this);
		this.activities.put(type2, a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// move methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Moves a facility to a new {@link Coord coordinate}. It also takes care that
	 * the up- and down-mapping to the neighbor layers (up-layer: {@link ZoneLayer}
	 * and down-layer: {@link NetworkLayer}) will be updated, too (if the neighbors exist).
	 * 
	 * <p><b>Note:</b> Other data structures than the {@link World} and the {@link NetworkLayer} of MATSim
	 * will not be updated (i.e. the references to links and facilities in a {@link Plan}
	 * of an agent of the {@link Population}).</p>
	 * 
	 * <p><b>Mapping rule (zone-facility):</b> The facility gets one zones assigned, in which 
	 * the facility is located in, or---if no such zone exists---the facility does not get a zone assigned.</p>
	 * 
	 * <p><b>Mapping rule (facility-link):</b> The facility gets the nearest right entry link assigned
	 * (see also {@link NetworkLayer#getNearestRightEntryLink(Coord)}).</p>
	 * 
	 * @param newCoord the now coordinate of the facility
	 */
	public final void moveTo(Coord newCoord) {
		log.info("moving facility id="+id+" from "+center+" to "+newCoord+"...");
		center.setXY(newCoord.getX(),newCoord.getY());
		if (layer.getUpRule() != null) {
			log.info("  removed "+up_mapping.size()+" up-mappings (zone).");
			removeAllUpMappings();
			ZoneLayer zones = (ZoneLayer)layer.getUpRule().getUpLayer();
			ArrayList<Location> nearestZones = zones.getNearestLocations(center);
			if (nearestZones.isEmpty()) { /* facility does not belong to a zone */ }
			else {
				// choose the first of the list (The list is generated via a defined order of the zones,
				// therefore the chosen zone is deterministic). 
				Zone z = (Zone)nearestZones.get(0);
				if (!z.contains(center)) { /* f is not located IN any of the nearest zones */ }
				else {
					addUpMapping(z);
					z.addDownMapping(this);
					log.info("  added "+up_mapping.size()+" new up-mappings (zone):");
					log.info("  - zone id="+z.getId());
				}
			}
		}
		if (layer.getDownRule() != null) {
			log.info("  removed "+down_mapping.size()+" down-mappings (link).");
			removeAllDownMappings();
			NetworkLayer network = (NetworkLayer)layer.getDownRule().getDownLayer();
			Link l = network.getNearestRightEntryLink(center);
			addDownMapping(l);
			l.addUpMapping(this);
			log.info("  added "+down_mapping.size()+" down-mapping (link):");
			log.info("  - link id="+l.getId());
		}
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setDesc(String desc) {
		if (desc == null) { this.desc = null; }
		else { this.desc = desc.intern(); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getDesc() {
		return this.desc;
	}
	
	public final TreeMap<String,Activity> getActivities() {
		return this.activities;
	}

	public final Activity getActivity(final String type) {
		return this.activities.get(type);
	}

	public final Link getLink() {
		if (this.down_mapping.isEmpty()) { return null; }
		if (this.down_mapping.size() > 1) { Gbl.errorMsg("Something is wrong!!! A facility contains at most one Link (as specified for the moment)!"); }
		return (Link)this.getDownMapping().get(this.down_mapping.firstKey());
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
		       "[nof_activities=" + this.activities.size() + "]";
	}

	public LocationType getLocationType() {
		return LocationType.FACILITY;
	}
}
