/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetNearestFacCoord.java
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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000v2.data.CAtts;

public class PersonXY2Facility extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonXY2Facility.class);

	private static final String H = "h";
	private static final String W = "w";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String T = "t";

	private static final String[] HOMES = { CAtts.ACT_HOME };
	private static final String[] WORKS = { CAtts.ACT_W2, CAtts.ACT_W3 };
	private static final String[] EDUCATIONS = { CAtts.ACT_EHIGH, CAtts.ACT_EKIGA, CAtts.ACT_EOTHR, CAtts.ACT_EPRIM, CAtts.ACT_ESECO };
	private static final String[] LEISURES = { CAtts.ACT_LC, CAtts.ACT_LG, CAtts.ACT_LS };
	private static final String[] SHOPS = { CAtts.ACT_S1, CAtts.ACT_S2, CAtts.ACT_S3, CAtts.ACT_S4, CAtts.ACT_S5, CAtts.ACT_SOTHR };
	private static final String[] TTAS = { "tta" };

	private final ActivityFacilitiesImpl facilities;
	private final Map<String,QuadTree<ActivityFacilityImpl>> fqts = new HashMap<String, QuadTree<ActivityFacilityImpl>>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonXY2Facility(final ActivityFacilitiesImpl facilities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.buildQuadTrees();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildQuadTrees() {
		log.info("      building a quadtree for the 5 base activity type (h,w,e,s,l)...");
		String[] types = { H, W, E, S, L, T };
		for (int i=0; i<types.length; i++) {
			log.info("        building a quadtree for type '"+types[i]+"'...");
			double minx = Double.POSITIVE_INFINITY;
			double miny = Double.POSITIVE_INFINITY;
			double maxx = Double.NEGATIVE_INFINITY;
			double maxy = Double.NEGATIVE_INFINITY;
			for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
				String[] type_set = this.getFacilityActTypes(types[i]);
				boolean ok = false;
				for (int j=0; j<type_set.length; j++) {
					if (f.getActivityOptions().keySet().contains(type_set[j])) { ok = true; }
				}
				if (ok) {
					if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
					if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
					if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
					if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
				}
			}
			minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
			log.info("        type="+types[i]+": xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
			QuadTree<ActivityFacilityImpl> qt = new QuadTree<ActivityFacilityImpl>(minx,miny,maxx,maxy);
			for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
				String[] type_set = this.getFacilityActTypes(types[i]);
				boolean ok = false;
				for (int j=0; j<type_set.length; j++) {
					if (f.getActivityOptions().keySet().contains(type_set[j])) { ok = true; }
				}
				if (ok) { qt.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			}
			log.info("        "+qt.size()+" facilities of type="+types[i]+" added.");
			this.fqts.put(types[i],qt);
			log.info("        done.");
		}
		log.info("      done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final String[] getFacilityActTypes(String act_type) {
		if (act_type.startsWith(E)) { return EDUCATIONS; }
		else if (act_type.startsWith(S)) { return SHOPS; }
		else if (act_type.startsWith(L)) { return LEISURES; }
		else if (act_type.startsWith(H)) { return HOMES; }
		else if (act_type.startsWith(W)) { return WORKS; }
		else if (act_type.startsWith(T)) { return TTAS; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (person.getPlans().isEmpty()) { Gbl.errorMsg("Each person must have at least one plan!"); }
		for (Plan plan : person.getPlans()) { this.run(plan); }
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				Coord coord = act.getCoord();
				QuadTree<ActivityFacilityImpl> qt = null;
				if (act.getType().startsWith(H)) { qt = this.fqts.get(H); }
				else if (act.getType().startsWith(W)) { qt = this.fqts.get(W); }
				else if (act.getType().startsWith(E)) { qt = this.fqts.get(E); }
				else if (act.getType().startsWith(S)) { qt = this.fqts.get(S); }
				else if (act.getType().startsWith(L)) { qt = this.fqts.get(L); }
				else if (act.getType().startsWith(T)) { qt = this.fqts.get(T); }
				else { throw new RuntimeException("act type ="+act.getType()+"not known!"); }
				
				ActivityFacilityImpl f = qt.get(coord.getX(),coord.getY());
				if (f == null) { throw new RuntimeException("Coordinates == null; something is wrong!"); }
				
				Link l = f.getLink();
				if (l == null) { throw new RuntimeException("Link == null; something is wrong!"); }
				
				act.setFacility(f);
				act.setLink(l);
				Coord coord_f = f.getCoord();
				act.setCoord(coord_f);
			}
		}
	}
}

