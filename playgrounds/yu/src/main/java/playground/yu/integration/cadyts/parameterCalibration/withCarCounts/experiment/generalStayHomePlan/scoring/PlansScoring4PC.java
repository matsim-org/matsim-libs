/* *********************************************************************** *
 * project: org.matsim.*
 * DummyPlansScoring4PC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalStayHomePlan.scoring;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * a changed copy of {@code PlansScoring} for the parameter calibration,
 * especially in order to put new parameters to CharyparNagelScoringConfigGroup
 *
 * @author yu
 *
 */
public abstract class PlansScoring4PC implements StartupListener,
		ScoringListener, IterationStartsListener {

	protected Events2Score4PC planScorer;

	@Override
	public abstract void notifyStartup(final StartupEvent event);

	public Events2Score4PC getPlanScorer() {
		return planScorer;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		planScorer.reset(event.getIteration());
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		planScorer.finish();
		// TODO during the first iteration, calculate ASC 4 stay home Plan, ASC
		// should be saved as custom-attr.
		if (event.getIteration() == event.getControler().getFirstIteration()) {

		}
	}

}
