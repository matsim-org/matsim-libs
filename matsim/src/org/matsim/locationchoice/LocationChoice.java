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
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSetSimultan;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.MultithreadedModuleA;



public class LocationChoice extends MultithreadedModuleA {

	private NetworkLayer network=null;
	private Controler controler = null;
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	
	private static final Logger log = Logger.getLogger(LocationChoice.class);
	
	private boolean constrained = false;
	
	
	public LocationChoice() {
	}

	public LocationChoice(
			final NetworkLayer network,
			Controler controler) {

		this.initLocal(network, controler);
	}


	private void initLocal(
			final NetworkLayer network,
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
		this.network.connect();
	}
	
	@Override
	public void finish() {
		super.finish();
		if (this.constrained) {
			
			int unsuccessfull = 0;
			
			Iterator<PlanAlgorithm> planAlgo_it = this.planAlgoInstances.iterator();
			while (planAlgo_it.hasNext()) {
				PlanAlgorithm plan_algo = planAlgo_it.next();
				unsuccessfull += ((LocationMutatorwChoiceSetSimultan)plan_algo).getNumberOfUnsuccessfull();
				((LocationMutatorwChoiceSetSimultan)plan_algo).resetUnsuccsessfull();
			}		
			log.info("Number of unsuccessfull LC in this iteration: "+ unsuccessfull);	
				
		}
		this.planAlgoInstances.clear();
	}


	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		if (!this.constrained) {
			this.planAlgoInstances.add(new RandomLocationMutator(this.network, this.controler));
		}
		else {
			this.planAlgoInstances.add(new LocationMutatorwChoiceSetSimultan(this.network, this.controler));
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
	
}
