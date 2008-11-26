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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.collections.QuadTree;


public abstract class LocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	protected NetworkLayer network = null;
	protected Controler controler = null;	
	protected TreeMap<String, QuadTree<Facility>> quad_trees;
	
	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, Facility []> facilities_of_type;
			
	private static final Logger log = Logger.getLogger(LocationMutator.class);
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler) {
		this.quad_trees = new TreeMap<String, QuadTree<Facility>>();
		this.facilities_of_type = new TreeMap<String, Facility []>();
		this.initiLocal(network, controler);		
	}
	
	/*
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(Facilities facilities) {
		
		TreeMap<String, TreeMap<Id, Facility>> trees = new TreeMap<String, TreeMap<Id, Facility>>();
		
		// get all types of activities
		Iterator<Facility> fac_it = facilities.iterator();
		while (fac_it.hasNext()) {
			Facility f = fac_it.next();
			TreeMap<String, Activity> activities = f.getActivities();
			
			Iterator<Activity> act_it = activities.values().iterator();
			while (act_it.hasNext()) {
				Activity act = act_it.next();
				
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
			this.quad_trees.put(type, this.builFacQuadTree(type, tree_of_type));
			Facility [] facility_array = (Facility [])(tree_of_type.values().toArray());
			this.facilities_of_type.put(type, facility_array);
		}
		
		
	}

	private void initiLocal(final NetworkLayer network, Controler controler) {		
		//create a quadtree for every activity type
		this.initTrees(controler.getFacilities());				
		this.network = network;
		this.controler = controler;
	}

	public void handlePlan(final Plan plan){
	}


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
			if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
			if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
			if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
			if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Facility> quadtree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (final Facility f : facilities_of_type.values()) {
			quadtree.put(f.getCenter().getX(),f.getCenter().getY(),f);
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
	
	
	protected List<Act>  defineMovablePrimaryActivities(final Plan plan) {

		/*
		 * TODO: maybe better use a hash-map for both structures (speed-up?) 
		 */
		List<Act> movablePrimaryActivities = new Vector<Act>();		
		TreeMap<String, List<Act>> primaryActivities = new TreeMap<String, List<Act>>();
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			boolean isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
			
			if (isPrimary && !primaryActivities.containsKey(act.getType())) {
				primaryActivities.put(act.getType(), new Vector<Act>());
			}
			
			if (isPrimary && !act.getType().startsWith("h")) {					
				primaryActivities.get(act.getType()).add(act);
			}
		}
		
		Iterator<List<Act>> it = primaryActivities.values().iterator();
		while (it.hasNext()) {
			List<Act> list = it.next();
			if (list.size() > 1) {
				int index = MatsimRandom.random.nextInt(list.size()-1);
				list.remove(index);
				movablePrimaryActivities.addAll(list);
			}
		}
		return movablePrimaryActivities;
	}
	
}
