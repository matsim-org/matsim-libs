/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util;

import org.apache.logging.log4j.LogManager;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

public class TaxiSimulationConsistencyChecker implements MobsimBeforeCleanupListener {
	private final TaxiEventSequenceCollector taxiEventSequenceCollector;
	private final TaxiConfigGroup taxiCfg;

	@Inject
	public TaxiSimulationConsistencyChecker(TaxiEventSequenceCollector taxiEventSequenceCollector,
			TaxiConfigGroup taxiCfg) {
		this.taxiEventSequenceCollector = taxiEventSequenceCollector;
		this.taxiCfg = taxiCfg;
	}

	public void addCheckAllRequestsPerformed() {
		for (var seq : taxiEventSequenceCollector.getRequestSequences().values()) {
			if (!seq.isCompleted()) {
				if (taxiCfg.breakSimulationIfNotAllRequestsServed) {
					throw new IllegalStateException(
							"Not all taxi requests served at simulation end time. This exception can be disabled in the taxi config group.");
				} else {
					LogManager.getLogger(getClass())
							.warn("Taxi request not performed. Request time:\t" + Time.writeTime(
									seq.getSubmitted().getTime()) + "\tPassenger:\t" + seq.getSubmitted()
									.getPersonIds());
				}
			}
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(final MobsimBeforeCleanupEvent e) {
		addCheckAllRequestsPerformed();
	}
}
