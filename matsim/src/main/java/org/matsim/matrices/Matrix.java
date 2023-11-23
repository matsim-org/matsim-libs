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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Matrix {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String id;
	private String desc = null;

	// double data structure. for fast access via from_location or via to_location
	private final TreeMap<String,ArrayList<Entry>> fromLocs = new TreeMap<>();
	private final TreeMap<String,ArrayList<Entry>> toLocs = new TreeMap<>();

	private static final Logger log = LogManager.getLogger(Matrix.class);

	private long counter = 0;
	private long nextMsg = 1;

	//////////////////////////////////////////////////////////////////////
	// Constructors
	//////////////////////////////////////////////////////////////////////

	public Matrix(final String id, final String desc) {
		if (id == null) {
			throw new NullPointerException("id must not be null");
		}
		this.id = id;
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Entry createAndAddEntry(final String fromLocId, final String toLocId, final double value) {

		// create an entry
		Entry e = new Entry(fromLocId, toLocId, value);

		// add it to the from location data structure
		if (!this.fromLocs.containsKey(fromLocId)) {
			this.fromLocs.put(fromLocId, new ArrayList<Entry>());
		}
		ArrayList<Entry> fe = this.fromLocs.get(fromLocId);
		fe.add(e);

		// add it to the to location data structure
		if (!this.toLocs.containsKey(toLocId)) {
			this.toLocs.put(toLocId, new ArrayList<Entry>());
		}
		ArrayList<Entry> te = this.toLocs.get(toLocId);
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

	public final Entry setEntry(final String fromLocId, final String toLocId, final double value) {
		Entry e = getEntry(fromLocId, toLocId);
		if (e == null) {
			return createAndAddEntry(fromLocId, toLocId, value);
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

	public final void removeEntry(final String from_loc, final String to_loc) {
		Entry entry = getEntry(from_loc, to_loc);
		ArrayList<Entry> from_loc_entries = this.fromLocs.get(from_loc);
		ArrayList<Entry> to_loc_entries = this.toLocs.get(to_loc);

		if ((from_loc_entries == null) || (to_loc_entries == null)) {
			throw new RuntimeException("entry with from_loc_id=" + from_loc +
			             " and to_loc_id=" + to_loc + " does not exist!" +
			             " Inconsistent data strucutre!!!");
		}

		if (!from_loc_entries.remove(entry)) {
			throw new RuntimeException("entry with from_loc_id=" + from_loc +
		             " and to_loc_id=" + to_loc + " does not exist!" +
		             " Inconsistent data strucutre!!!");
		}
		if (from_loc_entries.isEmpty()) {
			this.fromLocs.remove(from_loc);
		}

		if (!to_loc_entries.remove(entry)) {
			throw new RuntimeException("entry with from_loc_id=" + from_loc +
		             " and to_loc_id=" + to_loc + " does not exist!" +
		             " Inconsistent data strucutre!!!");
		}
		if (to_loc_entries.isEmpty()) {
			this.toLocs.remove(to_loc);
		}
		log.info("entry " + entry.toString() + " removed.");
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getId() {
		return this.id;
	}

	public final String getDesc() {
		return this.desc;
	}

	public final Map<String, ArrayList<Entry>> getFromLocations() {
		return this.fromLocs;
	}

	public final Map<String, ArrayList<Entry>> getToLocations() {
		return this.toLocs;
	}

	public final List<Entry> getFromLocEntries(final String fromLocationId) {
		return this.fromLocs.get(fromLocationId);
	}

	public final List<Entry> getToLocEntries(final String toLocationId) {
		return this.toLocs.get(toLocationId);
	}

	public final Entry getEntry(final String from, final String to) {
		ArrayList<Entry> fe = this.fromLocs.get(from);
		if (fe == null) return null;
		for (int i=0; i<fe.size(); i++) {
			Entry e = fe.get(i);
			if (e.getToLocation().equals(to)) {
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
				"[desc=" + this.desc + "]" +
				"[nof_from_locs=" + this.fromLocs.size() + "]" +
				"[nof_to_locs=" + this.toLocs.size() + "]";
	}
}
