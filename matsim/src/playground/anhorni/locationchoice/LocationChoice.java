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

import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;

import playground.anhorni.locationchoice.choiceset.LocationMutatorwChoiceSetSimultan;


public class LocationChoice extends MultithreadedModuleA {

	private NetworkLayer network=null;
	private Controler controler = null;
	

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


	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		//return new RandomLocationMutator(this.network);
		return new LocationMutatorwChoiceSetSimultan(this.network, this.controler);
	}
}
