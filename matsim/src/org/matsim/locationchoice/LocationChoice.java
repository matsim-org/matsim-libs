/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoice.java
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
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.LocationChoiceScoringFunctionFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;



public class LocationChoice extends AbstractMultithreadedModule {

	private NetworkLayer network=null;
	private Controler controler = null;
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	private static final Logger log = Logger.getLogger(LocationChoice.class);
	private boolean constrained = false;
	
	protected TreeMap<String, QuadTree<Facility>> quad_trees = new TreeMap<String, QuadTree<Facility>>();
	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, Facility []> facilities_of_type = new TreeMap<String, Facility []>();
	
	
	public LocationChoice() {
	}

	public LocationChoice(
			final NetworkLayer network,
			Controler controler) {
		// TODO: why does this module need the control(l)er as argument?  Gets a bit awkward
		// when you use it in demandmodelling where you don't really need a control(l)er.
		// kai, jan09
		
		/*
		 	Using the controler in the replanning module actually looks quite inconsiderately. 
		 	But controler creates a new instance of router in every iteration! 
		 	Thus, the controler must be given to the replanning module by now (see below).
		
			controler.getRoutingAlgorithm(f) {		
				return new PlansCalcRoute(this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory());
			}

			TODO: extend handlePlan() by the argument "router". Maybe some kind of startup method is needed, which is called 
			everytime before handlePlan is called. But that seems to
			require extended refactorings of replanning code. 
			
			anhorni: april09
		 */

		this.initLocal(network, controler);
	}


	private void initLocal(
			final NetworkLayer network,
			final Controler controler) {
		// TODO: see above (why do you need the control(l)er as argument?)
		
		/* 	This has to be done in the controler
			log.info("adding FacilitiesLoadCalculator");
			controler.addControlerListener(new FacilitiesLoadCalculator(controler.getFacilityPenalties()));
		*/
						
		if (Gbl.getConfig().locationchoice().getMode().equals("true")) {
			this.constrained = true;
			log.info("Doing constrained location choice");
		}
		else {
			log.info("Doing random location choice on univ. choice set");
		}

		this.controler = controler;
		this.network = network;
		this.network.connect();
		this.initTrees(controler.getFacilities());
	}
	
	@Override
	public void finishReplanning() {
		Gbl.printMemoryUsage();
		
		super.finishReplanning();
		if (this.constrained) {
			
			int unsuccessfull = 0;
			
			Iterator<PlanAlgorithm> planAlgo_it = this.planAlgoInstances.iterator();
			while (planAlgo_it.hasNext()) {
				PlanAlgorithm plan_algo = planAlgo_it.next();
				unsuccessfull += ((LocationMutatorwChoiceSet)plan_algo).getNumberOfUnsuccessfull();
				((LocationMutatorwChoiceSet)plan_algo).resetUnsuccsessfull();
			}		
			log.info("Number of unsuccessfull LC in this iteration: "+ unsuccessfull);	
				
		}
		this.planAlgoInstances.clear();
	}


	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		if (!this.constrained) {
			this.planAlgoInstances.add(new RandomLocationMutator(this.network, this.controler, 
					this.quad_trees, this.facilities_of_type));
		}
		else {
			this.planAlgoInstances.add(new LocationMutatorwChoiceSet(this.network, this.controler,  
					this.quad_trees, this.facilities_of_type));
		}		
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}

	// for test cases:
	public NetworkLayer getNetwork() {
		return network;
	}

	public Controler getControler() {
		return controler;
	}

	public List<PlanAlgorithm> getPlanAlgoInstances() {
		return planAlgoInstances;
	}
	
	// getters and setters needed exclusively for test cases
	public boolean isConstrained() {
		return constrained;
	}

	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}
	
	public void setNetwork(NetworkLayer network) {
		this.network = network;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
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
}
