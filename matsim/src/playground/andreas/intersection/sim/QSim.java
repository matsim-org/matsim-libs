/* *********************************************************************** *
 * project: org.matsim.*
 * ItsumoSim.java
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

package playground.andreas.intersection.sim;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.Simulation;
import org.matsim.plans.Plans;

public class QSim extends Simulation {

	protected static final int INFO_PERIOD = 3600;
	
	private Events events = null; 
	private Plans plans;
	private QNetworkLayer network;
	private Config config;
			
//	final private static Logger log = Logger.getLogger(QueueSimulation.class);
	
	

	public QSim(Events events, Plans population, QNetworkLayer network) {
		super();
		this.events = events;
		this.plans = population;
		this.network = network;
		this.config = Gbl.getConfig();
	}

	protected void prepareSim() {
		System.out.println("Preparing Sim");
		
		
	}

	protected void cleanupSim() {
		System.out.println("cleanup");
		
		
	}

	public void beforeSimStep(final double time) {
		System.out.println("before sim step");
	}

	public boolean doSimStep(final double time) {
		System.out.println("do a sim step");
				
		return false;
	}

	public void afterSimStep(final double time) {
		System.out.println("after sim step");
		
	}

}