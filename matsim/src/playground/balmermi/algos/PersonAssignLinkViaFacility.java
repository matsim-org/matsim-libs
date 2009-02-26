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

import java.util.Iterator;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.collections.QuadTree;


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

	private QuadTree<Facility> hfacs = null;
	private QuadTree<Facility> wfacs = null;
	private QuadTree<Facility> efacs = null;
	private QuadTree<Facility> sfacs = null;
	private QuadTree<Facility> lfacs = null;

	private final NetworkLayer network;
	private final Facilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignLinkViaFacility(NetworkLayer network, Facilities facilities) {
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
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
			if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
			if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
			if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.hfacs = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.wfacs = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.efacs = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.sfacs = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.lfacs = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (Facility f : this.facilities.getFacilities().values()) {
			if (f.getActivity(HOME) != null) { this.hfacs.put(f.getCenter().getX(),f.getCenter().getY(),f); }
			if (f.getActivity(WORK) != null) { this.wfacs.put(f.getCenter().getX(),f.getCenter().getY(),f); }
			if (f.getActivity(EDUCATION) != null) { this.efacs.put(f.getCenter().getX(),f.getCenter().getY(),f); }
			if (f.getActivity(SHOP) != null) { this.sfacs.put(f.getCenter().getX(),f.getCenter().getY(),f); }
			if (f.getActivity(LEISURE) != null) { this.lfacs.put(f.getCenter().getX(),f.getCenter().getY(),f); }
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
	public void run(final Person person) {
		Iterator<Plan> p_it = person.getPlans().iterator();
		while (p_it.hasNext()) {
			Plan p = p_it.next();
			Iterator<?> l_it = p.getIteratorLeg();
			while (l_it.hasNext()) {
				Leg l = (Leg)l_it.next();
				l.setRoute(null);
			}

			Iterator<?> a_it = p.getIteratorAct();
			while (a_it.hasNext()) {
				Act a = (Act)a_it.next();
				Coord coord = a.getCoord();
				if (coord == null) { Gbl.errorMsg("Something is wrong!"); }
				char type = a.getType().charAt(0);
				Facility f = null;
				if (type == H) { f = this.hfacs.get(coord.getX(),coord.getY()); }
				else if (type == W) { f = this.wfacs.get(coord.getX(),coord.getY()); }
				else if (type == E) { f = this.efacs.get(coord.getX(),coord.getY()); }
				else if (type == S) { f = this.sfacs.get(coord.getX(),coord.getY()); }
				else if (type == L) { f = this.lfacs.get(coord.getX(),coord.getY()); }
				else { Gbl.errorMsg("Something is wrong!"); }
				if (f == null) { Gbl.errorMsg("Something is wrong!"); }

				Link link = f.getLink();
				if (link == null) { Gbl.errorMsg("Something is wrong!"); }

				a.setLink(link);
				a.setCoord(f.getCenter());
			}
		}
	}
}
