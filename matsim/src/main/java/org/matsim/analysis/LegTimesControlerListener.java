/* *********************************************************************** *
 * project: org.matsim.*
 * LegTimesListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;

import jakarta.inject.Inject;

class LegTimesControlerListener implements AfterMobsimListener {

	private static final  Logger log = LogManager.getLogger(LegTimesControlerListener.class);

	private final CalcLegTimes legTimes;

	private final OutputDirectoryHierarchy controlerIO;

	@Inject
    LegTimesControlerListener(CalcLegTimes legTimes, OutputDirectoryHierarchy controlerIO) {
		this.legTimes = legTimes;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		legTimes.writeStats(controlerIO.getIterationFilename(event.getIteration(), "legdurations.txt"));
		// - print averages in log
		log.info("[" + event.getIteration() + "] average trip (probably: leg) duration is: " + (int) legTimes.getAverageLegDuration()
				+ " seconds = " + Time.writeTime(legTimes.getAverageLegDuration(), Time.TIMEFORMAT_HHMMSS));
		// trips are from "true" activity to "true" activity.  legs may also go
		// from/to ptInteraction activity.  This, in my opinion "legs" is the correct (matsim) term
		// kai, jul'11

	}

}
