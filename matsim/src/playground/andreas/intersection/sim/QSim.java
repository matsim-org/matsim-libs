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

import java.util.Date;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Simulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Time;

public class QSim extends Simulation {

	final private static Logger log = Logger.getLogger(QueueLink.class);

	protected static final int INFO_PERIOD = 3600;

	private static Events events = null;

	private Plans plans;

	private QNetworkLayer network;

	private Config config;

	// final private static Logger log =
	// Logger.getLogger(QueueSimulation.class);

	public QSim(Events events, Plans population, QNetworkLayer network) {
		super();
		setEvents(events);
		this.plans = population;
		this.network = network;
		this.config = Gbl.getConfig();
	}

	protected void prepareSim() {
		System.out.println("Preparing Sim");
		
		double startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if (this.stopTime == Time.UNDEFINED_TIME || this.stopTime == 0) this.stopTime = Double.MAX_VALUE;

		SimulationTimer.setSimStartTime(24*3600);
		SimulationTimer.setTime(startTime);
		
		if (this.plans == null) {
			throw new RuntimeException(
					"No valid Population found (plans == null)");
		}

		this.plans.addAlgorithm(new QCreateVehicle());
		this.plans.runAlgorithms();
		this.plans.clearAlgorithms();
		
		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		SimulationTimer.setSimStartTime(Math.max(startTime,SimulationTimer.getSimStartTime()));
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());

	}

	protected void cleanupSim() {
		System.out.println("cleanup");

	}

	public void beforeSimStep(final double time) {
//		System.out.println("before sim step");
	}

	/**
	 * Do one step of the simulation run.
	 *
	 * @return true if the simulation needs to continue
	 */
	@Override
	public boolean doSimStep(final double time) {
		
		this.network.moveLinks(time);
		this.network.moveNodes(time);

		if (time % INFO_PERIOD == 0) {
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
			double diffsim  = time - SimulationTimer.getSimStartTime();
			
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + getLiving() + " lost=" + getLost()
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}
//		System.out.println(isLiving());

		return isLiving() && (this.stopTime >= time);
	}

	public void afterSimStep(final double time) {
//		System.out.println("after sim step");

	}

	public static Events getEvents() {
		// TODO Auto-generated method stub
		return events;
	}

	private static final void setEvents(final Events events) {
		QSim.events = events;
	}

}