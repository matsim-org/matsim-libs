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
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.scoring;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
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
public class PlansScoring4PC_mnl extends PlansScoring4PC implements
		StartupListener, ScoringListener, IterationStartsListener {
	private final static Logger log = Logger.getLogger(PlansScoring4PC_mnl.class);
	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler ctl = event.getControler();

		planScorer = new Events2Score4PC_mnl(ctl.getConfig(), ctl
				.getScoringFunctionFactory(), ctl.getPopulation());

		log.debug(
				"PlansScoring4PC_mnl loaded ScoringFunctionFactory");

		ctl.getEvents().addHandler(planScorer);
	}

}
