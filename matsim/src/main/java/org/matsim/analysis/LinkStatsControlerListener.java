/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author mrieser
 */
final class LinkStatsControlerListener implements IterationEndsListener, IterationStartsListener {

	@Inject private LinkStatsConfigGroup linkStatsConfigGroup;
	@Inject private ControlerConfigGroup controlerConfigGroup;
	@Inject private CalcLinkStats linkStats;
	@Inject private VolumesAnalyzer volumes;
	@Inject private OutputDirectoryHierarchy controlerIO;
	@Inject private Map<String, TravelTime> travelTime;
    private int iterationsUsed = 0;
	private boolean doReset = false;

    @Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();
		
		if (useVolumesOfIteration(iteration, controlerConfigGroup.getFirstIteration())) {
			this.iterationsUsed++;
            linkStats.addData(volumes, travelTime.get(TransportMode.car));
		}

		if (createLinkStatsInIteration(iteration)) {
			linkStats.writeFile(this.controlerIO.getIterationFilename(iteration, Controler.DefaultFiles.linkstats));
			this.doReset = true;
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.doReset) {
			// resetting at the beginning of an iteration, to allow others to use the data until the very end of the previous iteration
			this.linkStats.reset();
			this.doReset = false;
		}
	}
	
	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		if (this.linkStatsConfigGroup.getWriteLinkStatsInterval() < 1) {
			return false;
		}
		int iterationMod = iteration % this.linkStatsConfigGroup.getWriteLinkStatsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.linkStatsConfigGroup.getAverageLinkStatsOverIterations(), this.linkStatsConfigGroup.getWriteLinkStatsInterval());
		if (iterationMod == 0) {
			return ((this.linkStatsConfigGroup.getAverageLinkStatsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.linkStatsConfigGroup.getWriteLinkStatsInterval() - this.linkStatsConfigGroup.getAverageLinkStatsOverIterations())
				&& (effectiveIteration + (this.linkStatsConfigGroup.getWriteLinkStatsInterval() - iterationMod) >= averaging));
	}
	
	/*package*/ boolean createLinkStatsInIteration(final int iteration) {
		return ((iteration % this.linkStatsConfigGroup.getWriteLinkStatsInterval() == 0) && (this.iterationsUsed >= this.linkStatsConfigGroup.getAverageLinkStatsOverIterations()));
	}

}
