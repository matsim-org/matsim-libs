/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSimConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.mobsim;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class PseudoSimConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "pseudoSim";

	public static enum PSimType { none , detailled , teleported; }
	private PSimType psimType = PSimType.none;
	private int nPSimIters = 5;
	private int period = 3;
	private int nThreads = 1;
	private boolean dumpEvents = false;

	public PseudoSimConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "pSimType" )
	public PSimType getPsimType() {
		return psimType;
	}

	@StringSetter( "pSimType" )
	public void setPsimType(PSimType psimType) {
		this.psimType = psimType;
	}

	@StringGetter( "nPSimIters" )
	public int getNPSimIters() {
		return this.nPSimIters;
	}

	@StringSetter( "nPSimIters" )
	public void setNPSimIters(int nPSimIters) {
		this.nPSimIters = nPSimIters;
	}

	@StringGetter( "period" )
	public int getPeriod() {
		return this.period;
	}

	@StringSetter( "period" )
	public void setPeriod(int period) {
		this.period = period;
	}

	@StringGetter( "nThreads" )
	public int getNThreads() {
		return this.nThreads;
	}

	@StringSetter( "nThreads" )
	public void setNThreads(int nThreads) {
		this.nThreads = nThreads;
	}

	@StringGetter( "dumpEvents" )
	public boolean isDumpEvents() {
		return this.dumpEvents;
	}

	@StringSetter( "dumpEvents" )
	public void setDumpEvents(boolean dumpEvents) {
		this.dumpEvents = dumpEvents;
	}

	public boolean isPSimIter(
			final int iteration ) {
		return iteration % (getPeriod() + getNPSimIters()) < getNPSimIters();
	}
}

