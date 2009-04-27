/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;


public abstract class LocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	protected NetworkLayer network = null;
	protected Controler controler = null;	
	protected TreeMap<String, QuadTree<Facility>> quad_trees;
	
	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, Facility []> facilities_of_type;
	protected final LocationChoiceConfigGroup config;
			
	private static final Logger log = Logger.getLogger(LocationMutator.class);
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler) {
		this.quad_trees = new TreeMap<String, QuadTree<Facility>>();
		this.facilities_of_type = new TreeMap<String, Facility []>();
		this.config = Gbl.getConfig().locationchoice();
		this.initLocal(network, controler);		
	}
	
	/*
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(Facilities facilities) {
		
		TreeMap<String, TreeMap<Id, Facility>> trees = new TreeMap<String, TreeMap<Id, Facility>>();
		
		// get all types of activities
		for (Facility f : facilities.getFacilities().values()) {
			Map<String, ActivityOption> activities = f.getActivityOptions();
			
			Iterator<ActivityOption> act_it = activities.values().iterator();
			while (act_it.hasNext()) {
				ActivityOption act = act_it.next();
				
				if (!trees.containsKey(act.getType())) {
					trees.put(act.getType(), new TreeMap<Id, Facility>());
				}
				trees.get(act.getType()).put(f.getId(), f);
			}	
		}
		
		// create the quadtrees and the arrays
		Iterator<TreeMap<Id, Facility>> tree_it = trees.values().iterator();
		Iterator<String> type_it = trees.keySet().iterator();
			
		while (tree_it.hasNext()) {
			TreeMap<Id, Facility> tree_of_type = tree_it.next();
			String type = type_it.next();
			
			// do not construct tree for home act
			if (type.startsWith("h")) continue;
			this.quad_trees.put(type, this.builFacQuadTree(type, tree_of_type));
			this.facilities_of_type.put(type, tree_of_type.values().toArray(new Facility[tree_of_type.size()]));
		}
		
		
	}

	private void initLocal(final NetworkLayer network, Controler controler) {		
		//create a quadtree for every activity type
		this.initTrees(controler.getFacilities());				
		this.network = network;
		this.controler = controler;
	}

	public abstract void handlePlan(final Plan plan);


	@Override
	public void run(final Person person) {
		final int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			final Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {	
		handlePlan(plan);
	}
	
	private QuadTree<Facility> builFacQuadTree(String type, TreeMap<Id,Facility> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final Facility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Facility> quadtree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (final Facility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("    done");
		Gbl.printRoundTime();
		return quadtree;
	}

	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
	
	
	protected List<Activity>  defineMovablePrimaryActivities(final Plan plan) {
	
		List<Activity> primaryActivities = new Vector<Activity>();
		
		final List actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);
			if (act.getType().startsWith("h")) continue;
			boolean isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
			
			if (isPrimary) {
				primaryActivities.add(act);
			}
		}
		Collections.shuffle(primaryActivities);
		
		List<Activity> movablePrimaryActivities = new Vector<Activity>();
		
		// key: activity.type + activity.facility
		HashMap<String, Boolean> fixPrimaries = new HashMap<String, Boolean>();
				
		Iterator<Activity> it = primaryActivities.iterator();
		while (it.hasNext()) {
			Activity a = it.next();		
			String key = a.getType()+a.getFacility().getId().toString();
			if (fixPrimaries.containsKey(key)) {
				// there is already one activity performed of the specific type at this location
				movablePrimaryActivities.add(a);
			}
			else {
				fixPrimaries.put(key, true);
			}
		}
		return movablePrimaryActivities;
	}
	
}
