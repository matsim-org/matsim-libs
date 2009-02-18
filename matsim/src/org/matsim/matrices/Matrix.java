/* *********************************************************************** *
 * project: org.matsim.*
 * Matrix.java
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

package org.matsim.matrices;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class Matrix {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String id;
	private final Layer worldLayer;
	private String desc = null;

	// double data structure. for fast access via from_location or via to_location
	// TreeMap<Integer f_loc_id,ArrayList<Entry entry>>
	private final TreeMap<Id,ArrayList<Entry>> fromLocs = new TreeMap<Id,ArrayList<Entry>>();
	// TreeMap<Integer t_loc_id,ArrayList<Entry entry>>
	private final TreeMap<Id,ArrayList<Entry>> toLocs = new TreeMap<Id,ArrayList<Entry>>();

	private static final Logger log = Logger.getLogger(Matrix.class);

	private long counter = 0;
	private long nextMsg = 1;

	//////////////////////////////////////////////////////////////////////
	// Constructors
	//////////////////////////////////////////////////////////////////////

	protected Matrix(final String id, final Layer worldLayer, final String desc) {
		if ((id == null) || (worldLayer == null)) {
			throw new IllegalArgumentException("id and layer must not be null: id="+id+", worldLayer="+worldLayer);
		}
		this.id = id;
		this.worldLayer = worldLayer;
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	protected final Entry createEntry(final String fromLocId, final String toLocId, final double value) {
		// get the from location
		Location f_loc = this.worldLayer.getLocation(fromLocId);
		if (f_loc == null) {
			log.warn("[from_loc_id="+fromLocId+" does not exist.]");
			return null;
		}
		// get the to location
		Location t_loc = this.worldLayer.getLocation(toLocId);
		if (t_loc == null) {
			log.warn("[to_loc_id="+toLocId+" does not exist.]");
			return null;
		}
		return createEntry(f_loc, t_loc, value);
	}

	public final Entry createEntry(final Location fromLoc, final Location toLoc, final double value) {

		Id f_id = fromLoc.getId();
		Id t_id = toLoc.getId();

		// create an entry
		Entry e = new Entry(fromLoc,toLoc,value);

		// add it to the from location data structure
		if (!this.fromLocs.containsKey(f_id)) {
			this.fromLocs.put(f_id,new ArrayList<Entry>());
		}
		ArrayList<Entry> fe = this.fromLocs.get(f_id);
		fe.add(e);

		// add it to the to location data structure
		if (!this.toLocs.containsKey(t_id)) {
			this.toLocs.put(t_id,new ArrayList<Entry>());
		}
		ArrayList<Entry> te = this.toLocs.get(t_id);
		te.add(e);

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			log.info("Matrix id=" + this.id + ": entry # " + this.counter);
		}

		// return the new entry
		return e;
	}

	//////////////////////////////////////////////////////////////////////
	// set/add methods
	//////////////////////////////////////////////////////////////////////

	public final Entry setEntry(final Location fromLocation, final Location toLocation, final double value) {
		Entry e = getEntry(fromLocation, toLocation);
		if (e == null) {
			return createEntry(fromLocation, toLocation, value);
		}
		e.setValue(value);
		return e;
	}

	public final void setDesc(final String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	public final void removeEntry(final Entry entry) {
		Location from_loc = entry.getFromLocation();
		Location to_loc = entry.getToLocation();

		ArrayList<Entry> from_loc_entries = this.fromLocs.get(from_loc.getId());
		ArrayList<Entry> to_loc_entries = this.toLocs.get(to_loc.getId());

		if ((from_loc_entries == null) || (to_loc_entries == null)) {
			Gbl.errorMsg("entry with from_loc_id=" + from_loc.getId() +
			             " and to_loc_id=" + to_loc.getId() + " does not exist!" +
			             " Inconsistent data strucutre!!!");
			return;
		}

		if (!from_loc_entries.remove(entry)) {
			Gbl.errorMsg("entry with from_loc_id=" + from_loc.getId() +
		             " and to_loc_id=" + to_loc.getId() + " does not exist!" +
		             " Inconsistent data strucutre!!!");
		}
		if (from_loc_entries.isEmpty()) {
			this.fromLocs.remove(from_loc.getId());
		}

		if (!to_loc_entries.remove(entry)) {
			Gbl.errorMsg("entry with from_loc_id=" + from_loc.getId() +
		             " and to_loc_id=" + to_loc.getId() + " does not exist!" +
		             " Inconsistent data strucutre!!!");
		}
		if (to_loc_entries.isEmpty()) {
			this.toLocs.remove(to_loc.getId());
		}
		System.out.println("entry " + entry.toString() + " removed.");
	}

	public final void removeToLocEntries(final Location toLocation) {
		ArrayList<Entry> to_loc_entries = this.toLocs.get(toLocation.getId());
		if (to_loc_entries != null) {
			ArrayList<Entry> tmp = new ArrayList<Entry>(to_loc_entries);
			Iterator<Entry> e_it = tmp.iterator();
			while (e_it.hasNext()) {
				Entry e = e_it.next();
				this.removeEntry(e);
			}
		}
	}

	public final void removeFromLocEntries(final Location fromLocation) {
		ArrayList<Entry> from_loc_entries = this.fromLocs.get(fromLocation.getId());
		if (from_loc_entries != null) {
			ArrayList<Entry> tmp = new ArrayList<Entry>(from_loc_entries);
			Iterator<Entry> e_it = tmp.iterator();
			while (e_it.hasNext()) {
				Entry e = e_it.next();
				this.removeEntry(e);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getId() {
		return this.id;
	}

	public final Layer getLayer() {
		return this.worldLayer;
	}

	public final String getDesc() {
		return this.desc;
	}

	public final TreeMap<Id, ArrayList<Entry>> getFromLocations() {
		return this.fromLocs;
	}

	public final TreeMap<Id, ArrayList<Entry>> getToLocations() {
		return this.toLocs;
	}

	public final ArrayList<Entry> getFromLocEntries(final Location fromlocation) {
		return this.fromLocs.get(fromlocation.getId());
	}

	public final ArrayList<Entry> getToLocEntries(final Location toLocation) {
		return this.toLocs.get(toLocation.getId());
	}

	public final Entry getEntry(final Location fromLocation, final Location toLocation) {
		return this.getEntry(fromLocation.getId(),toLocation.getId());
	}

	public final Entry getEntry(final Id from, final Id to) {
		ArrayList<Entry> fe = this.fromLocs.get(from);
		if (fe == null) return null;
		for (int i=0; i<fe.size(); i++) {
			Entry e = fe.get(i);
			if (e.getToLocation().getId().equals(to)) {
				return e;
			}
		}
		return null;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[id=" + this.id + "]" +
				"[world_layer_type=" + this.worldLayer.getType() + "]" +
				"[desc=" + this.desc + "]" +
				"[nof_from_locs=" + this.fromLocs.size() + "]" +
				"[nof_to_locs=" + this.toLocs.size() + "]";
	}
}
