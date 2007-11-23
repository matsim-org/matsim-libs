/* *********************************************************************** *
 * project: org.matsim.*
 * Simulation.java
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

package org.matsim.mobsim;

import java.util.Date;

import org.matsim.gbl.Gbl;

// TODO [DS] this is completely wrong: move more functionality into QueueSimulator
// leaving only most rudimentary functionality in Simulator itself
public class Simulation {

	/* I started implementing the new config-classes. Instead of
	 * <code>Gbl.getConfig().getParam(SIMULATION, ENDTIME);</code>
	 * one can now access the elements directly with
	 * <code>Gbl.getConfig().simulation().getEndTime();</code>.
	 * This prevents the repeated conversation of the strings to
	 * int/double/boolean etc. I commented out those constants that
	 * are currently no longer used in the code, the others will
	 * follow hopefully soon.   // mrieser, 03sept07
	 */
	public static final String SIMULATION = "simulation";
	public static final String SNAPSHOTFORMAT = "snapshotFormat";
	public static final String STARTTIME = "startTime";
	public static final String ENDTIME = "endTime";
//	public static final String SNAPSHOTPERIOD = "snapshotperiod";
//	public static final String OUTFILE = "outputLanduseFile";
	public static final String SHELLTYPE = "shellType";
	public static final String JAVACLASSPATH = "classPath";
	public static final String JVMOPTIONS = "JVMOptions";
	public static final String CLIENTLIST = "clientList";
	public static final String LOCALCONFIG = "localConfig";
	public static final String LOCALCONFIGDTD = "localConfigDTD";
//	public static final String FLOWCAPACITYFACTOR = "flowCapacityFactor";
//	public static final String STORAGECAPACITYFACTOR = "storageCapacityFactor";
//	public static final String STORAGECAPACITYCOMPENSATIONFACTOR = "storageCapacityCompensationFactor";
//	public static final String STUCKTIME = "stuckTime";

	protected Date starttime = new Date();

	private static int living = 0;
	private static int lost = 0;
	private static int stuckTime = Integer.MAX_VALUE;
	protected double stopTime = 100*3600;

	public Simulation() {
		setLiving(0);
		resetLost();
		setStuckTime((int)Gbl.getConfig().simulation().getStuckTime());//TODO [DS] change time to double
	}

	protected void prepareSim()
	{
	}
	protected void cleanupSim()
	{
	}

	public void beforeSimStep(final double time) {
	}

	public boolean doSimStep(final double time)
	{
		return false;
	}

	public void afterSimStep(final double time) {
	}

	//////////////////////////////////////////////////////////////////////
	// only the very basic simulation scheme here
	// overload prepare/cleanup and doSim step
	//////////////////////////////////////////////////////////////////////
	public final void run()
	{
		prepareSim();
		//do iterations
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			beforeSimStep(time);
			cont = doSimStep(time);
			afterSimStep(time);
			if (cont) {
				SimulationTimer.incTime();
			}
		}
		cleanupSim();
	}

	//////////////////////////////////////////////////////////////////////
	// some getter / setter functions
	//////////////////////////////////////////////////////////////////////
	public static final int getStuckTime() {return stuckTime;	}
	private static final void setStuckTime(final int stuckTime) { Simulation.stuckTime = stuckTime; }

	public static final int getLiving() {return living;	}
	public static final void setLiving(final int count) {living = count;}
	public static final boolean isLiving() {return living > 0;	}
	public static final int getLost() {return lost;	}
	public static final void incLost() {lost++;}
	public static final void incLost(final int count) {lost += count;}
	private static final void resetLost() { lost = 0; }

	// Why is incLiving() synchronized, but not decLiving()?? / mrieser, 07sep2007
	synchronized public static final void incLiving() {living++;}
	synchronized public static final void incLiving(final int count) {living += count;}
	public static final void decLiving() {living--;}
	public static final void decLiving(final int count) {living -= count;}
}
