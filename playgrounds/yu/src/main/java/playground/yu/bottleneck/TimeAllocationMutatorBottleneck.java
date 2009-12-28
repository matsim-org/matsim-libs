/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
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

package playground.yu.bottleneck;

import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

public class TimeAllocationMutatorBottleneck extends AbstractMultithreadedModule {
	
	public TimeAllocationMutatorBottleneck(Config config) {
		super(config.global());
	}

	// -----------------------------MEMBER
	// VARIABLE------------------------------
	private static final int mutationRange = 1800;

	/*
	 * ////////////////////////// // public TimeAllocationMutatorBottleneck() { //
	 * String range = null; // try { // range =
	 * Gbl.getConfig().getParam("TimeAllocationMutatorBottleneck","mutationRange");
	 * //// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ // } // catch
	 * (IllegalArgumentException e) { //
	 * Gbl.noteMsg(this.getClass(),"TimeAllocationMutatorBottleneck()","No
	 * mutation range defined in the config file. Using 1800 sec."); // } // if
	 * (range != null) { // this.mutationRange = Integer.parseInt(range); //
	 * Gbl.noteMsg(this.getClass(),"TimeAllocationMutatorBottleneck()","mutation
	 * range = " + this.mutationRange + "."); // } // } // // public
	 * TimeAllocationMutatorBottleneck(final int muntation_range) { //
	 * this.mutationRange = muntation_range; // }
	 * /////////////////////////////////////////////////
	 */
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanMutateTimeAllocationBottleneck(mutationRange);
	}

}
