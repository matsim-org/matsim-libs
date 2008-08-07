/* *********************************************************************** *
 * project: org.matsim.*
 * DumpPlans.java.java
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

package org.matsim.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.population.PopulationWriter;

/**
 * {@link org.matsim.controler.listener.ControlerListener} that dumps the
 * complete plans every 10th iteration as well as in the first iteration,
 * just in case someone might check that the replanning worked correctly in
 * the first iteration.
 *
 * @author mrieser
 */
public class PlansDumping implements BeforeMobsimListener {

	static final private Logger log = Logger.getLogger(PlansDumping.class);

	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		if (event.getIteration() % 10 == 0 || event.getIteration() == (controler.getFirstIteration() + 1)) {
			controler.stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			String outversion = controler.getConfig().plans().getOutputVersion();
			PopulationWriter plansWriter = new PopulationWriter(controler.getPopulation(), Controler.getIterationFilename("plans.xml.gz"), outversion);
			plansWriter.write();
			log.info("finished plans dump.");
			controler.stopwatch.endOperation("dump all plans");
		}
	}

}
