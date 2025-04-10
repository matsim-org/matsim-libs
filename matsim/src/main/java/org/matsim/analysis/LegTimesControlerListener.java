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
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.misc.Time;

import jakarta.inject.Inject;

class LegTimesControlerListener implements AfterMobsimListener, IterationStartsListener, IterationEndsListener {

	private static final  Logger log = LogManager.getLogger(LegTimesControlerListener.class);

	private final CalcLegTimes legTimes;
	private final OutputDirectoryHierarchy controlerIO;
	private final ControllerConfigGroup controllerConfigGroup;

	@Inject
    LegTimesControlerListener(CalcLegTimes legTimes, OutputDirectoryHierarchy controlerIO, ControllerConfigGroup controllerConfigGroup) {
		this.legTimes = legTimes;
		this.controlerIO = controlerIO;
		this.controllerConfigGroup = controllerConfigGroup;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (ControllerUtils.isIterationActive(event, controllerConfigGroup.getLegDurationsInterval())) {
			event.getServices().getEvents().addHandler(this.legTimes);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (ControllerUtils.isIterationActive(event, controllerConfigGroup.getLegDurationsInterval())) {
			event.getServices().getEvents().removeHandler(this.legTimes);
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		if (ControllerUtils.isIterationActive(event, controllerConfigGroup.getLegDurationsInterval())) {
			legTimes.writeStats(controlerIO.getIterationFilename(event.getIteration(), "legdurations.txt"));
			// - print averages in log
			// it is a leg duration, not a trip duration
			log.info("[{}] average leg duration is: {} seconds = {}", event.getIteration(), (int) legTimes.getAverageLegDuration(), Time.writeTime(legTimes.getAverageLegDuration(), Time.TIMEFORMAT_HHMMSS));
		}
	}

}
