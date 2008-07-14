/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayControler.java
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

package playground.gregor.withinday_evac;



import org.matsim.controler.Controler;
import org.matsim.evacuation.EvacuationQSimControler;

import playground.gregor.withinday_evac.mobsim.WithindayQueueSimulation;

public class WithindayControler extends EvacuationQSimControler {

	public WithindayControler(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void runMobSim() {
		log.info("Starting withinday replanning iteration...");

		//build the queuesim
		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.network, this.population, this.events, this);
		//run the simulation
		sim.run();
	}

	public static void main(final String[] args) {
		final Controler controler = new WithindayControler(args);
		controler.run();
		System.exit(0);
	}
	
}
