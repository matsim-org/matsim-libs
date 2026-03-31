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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Maintainer: mrieser / Senozon AG
 * @author balmermi
 */
public class ActivityFacilitiesImpl implements ActivityFacilities, SearchableActivityFacilities {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long nextMsg = 1;

	private static final Logger log = LogManager.getLogger(ActivityFacilitiesImpl.class);
	private final ActivityFacilitiesFactory factory ;
	private final Attributes attributes = new AttributesImpl();

	private final IdMap<ActivityFacility, ActivityFacility> facilities = new IdMap<>(ActivityFacility.class); // FIXME potential iteration order change

	private String name;

	private QuadTree<ActivityFacility> facilitiesQuadTree;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	ActivityFacilitiesImpl(final String name) {
		this.name = name;
		this.factory = new ActivityFacilitiesFactoryImpl();
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivityFacilityImpl createAndAddFacility(final Id<ActivityFacility> id, final Coord center) {
		return createAndAddFacility(id, center, null);
	}

	public final ActivityFacilityImpl createAndAddFacility(final Id<ActivityFacility> id, final Coord center, final Id<Link> linkId) {
		if (this.facilities.containsKey(id)) {
			throw new IllegalArgumentException("Facility with id=" + id + " already exists.");
		}
		ActivityFacilityImpl f = new ActivityFacilityImpl(id, center, linkId);
		this.facilities.put(f.getId(),f);

		// show counter
		if (this.facilities.size() % this.nextMsg == 0) {
			this.nextMsg *= 2;
			log.info("    facility # " + this.facilities.size() );
		}

		return f;
	}

	@Override
	public ActivityFacilitiesFactory getFactory() {
		return this.factory;
	}

	@Override
	public final Map<Id<ActivityFacility>, ? extends ActivityFacility> getFacilities() {
		return this.facilities;
	}

	@Override
	public final TreeMap<Id<ActivityFacility>, ActivityFacility> getFacilitiesForActivityType(final String act_type) {
		TreeMap<Id<ActivityFacility>, ActivityFacility> facs = new TreeMap<>();
		for (ActivityFacility f : this.facilities.values()) {
			Map<String, ? extends ActivityOption> a = f.getActivityOptions();
			if (a.containsKey(act_type)) {
				facs.put(f.getId(), f);
			}
		}
		return facs;
	}

	 @Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public final void addActivityFacility(ActivityFacility facility) {
		// validation
		if (this.facilities.containsKey(facility.getId())) {
			throw new IllegalArgumentException("Facility with id=" + facility.getId() + " already exists.");
		}

		this.facilities.put(facility.getId(),facility);
	}

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder(200);
		stb.append(super.toString());
		stb.append("\n");
		stb.append("[number of facilities=");
		stb.append(this.facilities.size());
		stb.append("]\n");
		for ( Entry<Id<ActivityFacility>,? extends ActivityFacility> entry : this.facilities.entrySet() ) {
			final ActivityFacility fac = entry.getValue();
			stb.append("[key=");
			stb.append(entry.getKey().toString());
			stb.append("; value=");
			stb.append(fac.toString());
			stb.append("]\n");
		}

		return stb.toString();
	}

	synchronized private void buildQuadTree() {
		/* the method must be synchronized to ensure we only build one quadTree
		 * in case that multiple threads call a method that requires the quadTree.
		 */
		if (this.facilitiesQuadTree != null) {
			return;
		}
		double startTime = System.currentTimeMillis();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for ( ActivityFacility n : this.facilities.values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15

		log.info("building QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
		for (ActivityFacility n : this.facilities.values()) {
			quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		/* assign the quadTree at the very end, when it is complete.
		 * otherwise, other threads may already start working on an incomplete quadtree
		 */
		this.facilitiesQuadTree = quadTree;
		log.info("Building QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
	}


	/**
	 * finds the node nearest to <code>coord</code>
	 *
	 * @param coord the coordinate to which the closest node should be found
	 * @return the closest node found, null if none
	 */
	@Override public ActivityFacility getNearestFacility(final Coord coord) {
		if (this.facilitiesQuadTree == null) { buildQuadTree(); }
		return this.facilitiesQuadTree.getClosest(coord.getX(), coord.getY());
	}

	/**
	 * finds the nodes within distance to <code>coord</code>
	 *
	 * @param coord the coordinate around which nodes should be located
	 * @param distance the maximum distance a node can have to <code>coord</code> to be found
	 * @return all nodes within distance to <code>coord</code>
	 */
	@Override public Collection<ActivityFacility> getNearestFacilities(final Coord coord, final double distance) {
		if (this.facilitiesQuadTree == null) { buildQuadTree(); }
		return this.facilitiesQuadTree.getDisk(coord.getX(), coord.getY(), distance);
	}


	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
