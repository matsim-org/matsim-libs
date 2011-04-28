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

package org.matsim.core.controler.corelisteners;

import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author mrieser
 */
public class LinkStatsControlerListener implements IterationEndsListener, IterationStartsListener {

	private final LinkStatsConfigGroup config;
	private int iterationsUsed = 0;
	private boolean doReset = false;
	
	public LinkStatsControlerListener(final LinkStatsConfigGroup config) {
		this.config = config;
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
		int iteration = event.getIteration();
		
		if (useVolumesOfIteration(iteration, controler.getFirstIteration())) {
			this.iterationsUsed++;
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
		}

		if (createLinkStatsInIteration(iteration)) {
			controler.getLinkStats().writeFile(event.getControler().getControlerIO().getIterationFilename(iteration, Controler.FILENAME_LINKSTATS));
			this.doReset = true;
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (this.doReset) {
			// resetting at the beginning of an iteration, to allow others to use the data until the very end of the previous iteration
			event.getControler().getLinkStats().reset();
			this.doReset = false;
		}
	}
	
	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		if (this.config.getWriteLinkStatsInterval() < 1) {
			return false;
		}
		int iterationMod = iteration % this.config.getWriteLinkStatsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.config.getAverageLinkStatsOverIterations(), this.config.getWriteLinkStatsInterval());
		if (iterationMod == 0) {
			return ((this.config.getAverageLinkStatsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.config.getWriteLinkStatsInterval() - this.config.getAverageLinkStatsOverIterations())
				&& (effectiveIteration + (this.config.getWriteLinkStatsInterval() - iterationMod) >= averaging));
	}
	
	/*package*/ boolean createLinkStatsInIteration(final int iteration) {
		return ((iteration % this.config.getWriteLinkStatsInterval() == 0) && (this.iterationsUsed >= this.config.getAverageLinkStatsOverIterations()));		
	}

}
