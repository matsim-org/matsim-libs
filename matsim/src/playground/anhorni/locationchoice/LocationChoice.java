/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
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

package playground.anhorni.locationchoice;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;

import playground.anhorni.locationchoice.choiceset.LocationMutatorwChoiceSetSimultan;


public class LocationChoice extends MultithreadedModuleA {

	private NetworkLayer network=null;
	private Controler controler = null;
	private List<PlanAlgorithmI>  planAlgoInstances = new Vector<PlanAlgorithmI>();
	
	private static final Logger log = Logger.getLogger(LocationChoice.class);
	
	// temporaray hack:
	// 0: random 1: with choice set
	int variant = 0;
	

	public LocationChoice() {
	}

	public LocationChoice(
			final NetworkLayer network,
			Controler controler) {

		this.init(network, controler);
	}


	private void init(
			final NetworkLayer network,
			final Controler controler) {

		this.controler = controler;
		this.network = network;
		this.network.connect();
	}
	
	public void finish() {
		super.finish();
		if (variant == 1) {
			
			int unsuccessfull = 0;
			
			Iterator<PlanAlgorithmI> planAlgo_it = this.planAlgoInstances.iterator();
			while (planAlgo_it.hasNext()) {
				PlanAlgorithmI plan_algo = planAlgo_it.next();
				unsuccessfull += ((LocationMutatorwChoiceSetSimultan)plan_algo).getNumberOfUnsuccessfull();
				((LocationMutatorwChoiceSetSimultan)plan_algo).resetUnsuccsessfull();
			}		
			log.info("Number of unsuccessfull LC in this iteration: "+ unsuccessfull);	
				
		}
		this.planAlgoInstances.clear();
	}


	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		if (variant == 0) {
			this.planAlgoInstances.add(new RandomLocationMutator(this.network));
		}
		else {
			this.planAlgoInstances.add(new LocationMutatorwChoiceSetSimultan(this.network, this.controler));
		}		
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}
}
