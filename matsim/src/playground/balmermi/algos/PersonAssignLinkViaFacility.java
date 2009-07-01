/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAssignLinkViaFacility.java
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

package playground.balmermi.algos;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


public class PersonAssignLinkViaFacility extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final char H = 'h';
	private static final char W = 'w';
	private static final char E = 'e';
	private static final char S = 's';
	private static final char L = 'l';
	private static final String HOME = "home";
	private static final String WORK = "work";
	private static final String EDUCATION = "education";
	private static final String SHOP = "shop";
	private static final String LEISURE = "leisure";

	private QuadTree<ActivityFacility> hfacs = null;
	private QuadTree<ActivityFacility> wfacs = null;
	private QuadTree<ActivityFacility> efacs = null;
	private QuadTree<ActivityFacility> sfacs = null;
	private QuadTree<ActivityFacility> lfacs = null;

	private final NetworkLayer network;
	private final ActivityFacilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignLinkViaFacility(NetworkLayer network, ActivityFacilities facilities) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.network = network;
		this.facilities = facilities;
		this.buildFacQuadTrees();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void buildFacQuadTrees() {
		Gbl.startMeasurement();
		System.out.println("      building facility quad trees...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.hfacs = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.wfacs = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.efacs = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.sfacs = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.lfacs = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOption(HOME) != null) { this.hfacs.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			if (f.getActivityOption(WORK) != null) { this.wfacs.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			if (f.getActivityOption(EDUCATION) != null) { this.efacs.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			if (f.getActivityOption(SHOP) != null) { this.sfacs.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			if (f.getActivityOption(LEISURE) != null) { this.lfacs.put(f.getCoord().getX(),f.getCoord().getY(),f); }
		}
		System.out.println("        # homes = " + this.hfacs.size());
		System.out.println("        # works = " + this.wfacs.size());
		System.out.println("        # educs = " + this.efacs.size());
		System.out.println("        # shops = " + this.sfacs.size());
		System.out.println("        # leis' = " + this.lfacs.size());
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final PersonImpl person) {
		for (PlanImpl p : person.getPlans()) {
			for (PlanElement pe : p.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl l = (LegImpl) pe;
					l.setRoute(null);
				} else if (pe instanceof ActivityImpl) {
					ActivityImpl a = (ActivityImpl) pe;
					Coord coord = a.getCoord();
					if (coord == null) { throw new RuntimeException("Something is wrong!"); }
					char type = a.getType().charAt(0);
					ActivityFacility f = null;
					if (type == H) { f = this.hfacs.get(coord.getX(),coord.getY()); }
					else if (type == W) { f = this.wfacs.get(coord.getX(),coord.getY()); }
					else if (type == E) { f = this.efacs.get(coord.getX(),coord.getY()); }
					else if (type == S) { f = this.sfacs.get(coord.getX(),coord.getY()); }
					else if (type == L) { f = this.lfacs.get(coord.getX(),coord.getY()); }
					else { throw new RuntimeException("Something is wrong!"); }
					if (f == null) { throw new RuntimeException("Something is wrong!"); }
					
					Link link = f.getLink();
					if (link == null) { throw new RuntimeException("Something is wrong!"); }
					
					a.setLink(link);
					a.setCoord(f.getCoord());
				}
			}
		}
	}
}
