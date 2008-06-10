/* *********************************************************************** *
 * project: org.matsim.*
 * Facilities.java
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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.facilities.algorithms.FacilityAlgorithmI;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Layer;

public class Facilities extends Layer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	public static final Id LAYER_TYPE = new IdImpl("facility");

	private final ArrayList<FacilitiesAlgorithm> algorithms = new ArrayList<FacilitiesAlgorithm>();

	private long counter = 0;
	private long nextMsg = 1;

	public static final boolean FACILITIES_USE_STREAMING = true;
	public static final boolean FACILITIES_NO_STREAMING = false;

	private boolean isStreaming = Facilities.FACILITIES_NO_STREAMING;

	private static final Logger log = Logger.getLogger(Facilities.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Facilities(final String name, final boolean isStreaming) {
		super(LAYER_TYPE,name);
		this.isStreaming = isStreaming;
	}

	public Facilities() {
		this(null, false);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Facility createFacility(final Id id, final CoordI center) {
		if (this.locations.containsKey(id)) {
			Gbl.errorMsg("Facility id=" + id + " already exists.");
		}
		Facility f = new Facility(this,id,center);
		this.locations.put(f.getId(),f);

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			printFacilitiesCount();
		}

		return f;
	}

	public final Facility createFacility(final String id, final String x, final String y) {
		return this.createFacility(new IdImpl(id),new Coord(x,y));
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final void addAlgorithm(final FacilitiesAlgorithm algo) {
		this.algorithms.add(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		if (!this.isStreaming) {
			for (int i = 0; i < this.algorithms.size(); i++) {
				FacilitiesAlgorithm algo = this.algorithms.get(i);
				algo.run(this);
			}
		} else {
			log.info("Facilities streaming is on. Algos were run during parsing");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// finish methods
	//////////////////////////////////////////////////////////////////////

	public final void finishFacility(Facility f) {
		if (this.isStreaming) {
			// run algorithms
			for (FacilitiesAlgorithm facilitiesAlgo : this.algorithms) {
				if (facilitiesAlgo instanceof FacilityAlgorithmI) {
					((FacilityAlgorithmI) facilitiesAlgo).run(f);
				}
			}

			// remove facility because we are streaming
			this.locations.remove(f.getId());
		} else {
			// do nothing
		}
	}

	//////////////////////////////////////////////////////////////////////
	// clear methods
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.algorithms.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public final Map<Id, ? extends Facility> getFacilities() {
		return (Map<Id, ? extends Facility>) getLocations();
	}

	//Added 27.03.08 JH for random secondary location changes
	public final TreeMap<Id,Facility> getFacilities(final String act_type) {
		TreeMap<Id,Facility> facs = new TreeMap<Id, Facility>();
		Iterator<? extends Facility> lit = this.getFacilities().values().iterator();
		while(lit.hasNext()){
			Facility f = (Facility) lit.next();
			TreeMap<String, Activity> a = f.getActivities();
			if(a.containsKey(act_type)){
				facs.put(f.getId(),f);
			}
		}
		return facs;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
		"[nof_algorithms=" + this.algorithms.size() + "]";
	}
	
	public final void printFacilitiesCount() {
		log.info("    facility # " + this.counter);
	}
}
