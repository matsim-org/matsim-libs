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
package playground.thibautd.pseudoqsim;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class PseudoSimConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "pseudoSim";

	public static enum PSimType { detailled , teleported; }
	private PSimType psimType = PSimType.teleported;
	private int nPSimIters = 5;
	private int period = 3;
	private int nThreads = 1;

	private int writeEventsAndPlansIntervalInMobsim = 10;
	private int writeEventsAndPlansIntervalInPSim = 10;

	public PseudoSimConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "pSimType" )
	public PSimType getPsimType() {
		return psimType;
	}

	@StringSetter( "pSimType" )
	public void setPsimType(final PSimType psimType) {
		this.psimType = psimType;
	}

	@StringGetter( "nPSimIters" )
	public int getNPSimIters() {
		return this.nPSimIters;
	}

	@StringSetter( "nPSimIters" )
	public void setNPSimIters(final int nPSimIters) {
		this.nPSimIters = nPSimIters;
	}

	@StringGetter( "period" )
	public int getPeriod() {
		return this.period;
	}

	@StringSetter( "period" )
	public void setPeriod(final int period) {
		this.period = period;
	}

	@StringGetter( "nThreads" )
	public int getNThreads() {
		return this.nThreads;
	}

	@StringSetter( "nThreads" )
	public void setNThreads(final int nThreads) {
		this.nThreads = nThreads;
	}

	@StringGetter( "writeEventsAndPlansIntervalInMobsim" )
	public int getWriteEventsAndPlansIntervalInMobsim() {
		return writeEventsAndPlansIntervalInMobsim;
	}

	@StringSetter( "writeEventsAndPlansIntervalInMobsim" )
	public void setWriteEventsAndPlansIntervalInMobsim(
			final int writeEventsAndPlansIntervalInMobsim) {
		this.writeEventsAndPlansIntervalInMobsim = writeEventsAndPlansIntervalInMobsim;
	}

	@StringGetter( "writeEventsAndPlansIntervalInPSim" )
	public int getWriteEventsAndPlansIntervalInPSim() {
		return writeEventsAndPlansIntervalInPSim;
	}

	@StringSetter( "writeEventsAndPlansIntervalInPSim" )
	public void setWriteEventsAndPlansIntervalInPSim(
			final int writeEventsAndPlansIntervalInPSim) {
		this.writeEventsAndPlansIntervalInPSim = writeEventsAndPlansIntervalInPSim;
	}

	public boolean isPSimIter(
			final int iteration ) {
		return getNPSimIters() > 0 && iteration % (getPeriod() + getNPSimIters()) < getNPSimIters();
	}

	public boolean isDumpingIter(
			final int iteration ) {
		if ( getNPSimIters() <= 0 ) return iteration % getWriteEventsAndPlansIntervalInMobsim() == 0;

		final int cycleNr = iteration - (iteration % (getPeriod() + getNPSimIters()));
		final int cycleLength = isPSimIter( iteration ) ? getNPSimIters() : getPeriod();

		final int offset = isPSimIter( iteration ) ? 0 : getPeriod();
		final int positionInCycle = iteration - (cycleNr * (getPeriod() + getNPSimIters())) - offset;

		final int writingPeriod = isPSimIter( iteration ) ?
			getWriteEventsAndPlansIntervalInPSim() :
			getWriteEventsAndPlansIntervalInMobsim();

		return (cycleNr * cycleLength + positionInCycle) %  writingPeriod == 0;
	}
}

