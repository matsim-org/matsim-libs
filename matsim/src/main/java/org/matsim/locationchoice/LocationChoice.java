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
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.constrained.LocationMutatorTGSimple;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;
import org.matsim.locationchoice.utils.QuadTreeRing;
import org.matsim.locationchoice.utils.TreesBuilder;
import org.matsim.population.algorithms.PlanAlgorithm;

public class LocationChoice extends AbstractMultithreadedModule {

	private Network network=null;
	private Controler controler = null;
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	private static final Logger log = Logger.getLogger(LocationChoice.class);
	private boolean constrained = false;
	
	protected TreeMap<String, QuadTreeRing<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacility>>();
	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private Knowledges knowledges;
	
	
	/**
	 * @deprecated seems like this constructor is used nowhere
	 */
	public LocationChoice(Config config) {
		super(config.global());
	}

	public LocationChoice(
			final Network network,
			Controler controler, Knowledges kn) {
		super(controler.getConfig().global());
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
		this.knowledges = kn;
		this.initLocal(network, controler);
	}


	private void initLocal(
			final Network network,
			final Controler controler) {
						
		if (Gbl.getConfig().locationchoice().getMode().equals("true")) {
			this.constrained = true;
			log.info("Doing constrained location choice");
		}
		else {
			log.info("Doing random location choice on univ. choice set");
		}
		this.controler = controler;
		this.network = network;
		((NetworkImpl) this.network).connect();
		this.initTrees(controler.getFacilities());
	}
			
	/*
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(ActivityFacilities facilities) {
		DefineFlexibleActivities defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges);
		log.info("Doing location choice for activities: " + defineFlexibleActivities.getFlexibleTypes().toString());
		TreesBuilder treesBuilder = new TreesBuilder(defineFlexibleActivities.getFlexibleTypes(), this.network);
		treesBuilder.createTrees(facilities);
		this.facilitiesOfType = treesBuilder.getFacilitiesOfType();
		this.quadTreesOfType = treesBuilder.getQuadTreesOfType();
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
				
				if (Gbl.getConfig().locationchoice().getSimpleTG().equals("true")) {				
					unsuccessfull += ((LocationMutatorTGSimple)plan_algo).getNumberOfUnsuccessfull();
					((LocationMutatorTGSimple)plan_algo).resetUnsuccsessfull();
				}
				else {
					unsuccessfull += ((LocationMutatorwChoiceSet)plan_algo).getNumberOfUnsuccessfull();
					((LocationMutatorwChoiceSet)plan_algo).resetUnsuccsessfull();
				}
			}		
			log.info("Number of unsuccessfull LC in this iteration: "+ unsuccessfull);					
		}
		this.planAlgoInstances.clear();
	}


	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		if (!this.constrained) {
			this.planAlgoInstances.add(new RandomLocationMutator(this.network, this.controler, this.knowledges,
					this.quadTreesOfType, this.facilitiesOfType));
		}
		else {
			// only moving one flexible activity
			if (Gbl.getConfig().locationchoice().getSimpleTG().equals("true")) {	
				this.planAlgoInstances.add(new LocationMutatorTGSimple(this.network, this.controler, this.knowledges, 
					this.quadTreesOfType, this.facilitiesOfType));
			}
			else {
				// computing new chain of flexible activity
				this.planAlgoInstances.add(new LocationMutatorwChoiceSet(this.network, this.controler,  this.knowledges,
				this.quadTreesOfType, this.facilitiesOfType));
			}
		}		
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}

	// for test cases:
	/*package*/ Network getNetwork() {
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
}
