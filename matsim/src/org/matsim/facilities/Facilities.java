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
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Layer;

public class Facilities extends Layer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	public static final IdI LAYER_TYPE = new Id("facility");

	private final ArrayList<FacilitiesAlgorithm> algorithms = new ArrayList<FacilitiesAlgorithm>();

	private long counter = 0;
	private long nextMsg = 1;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Facilities(final String name) {
		super(LAYER_TYPE,name);
	}

	public Facilities() {
		this(null);
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Facility createFacility(final IdI id, final CoordI center) {
		if (this.locations.containsKey(id)) {
			Gbl.errorMsg("Facility id=" + id + " aready exists.");
		}
		Facility f = new Facility(this,id,center);
		this.locations.put(f.getId(),f);

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			System.out.println("    facility # " + this.counter);
		}
		return f;
	}

	public final Facility createFacility(final String id, final String x, final String y) {
		return this.createFacility(new Id(id),new Coord(x,y));
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
		for (int i = 0; i < this.algorithms.size(); i++) {
			FacilitiesAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
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
	public final Map<IdI, ? extends Facility> getFacilities() {
		return (Map<IdI, ? extends Facility>) this.locations;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
		       "[nof_algorithms=" + this.algorithms.size() + "]";
	}
}
