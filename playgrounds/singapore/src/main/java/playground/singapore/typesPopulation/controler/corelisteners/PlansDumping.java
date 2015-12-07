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

package playground.singapore.typesPopulation.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import playground.singapore.typesPopulation.population.PopulationWriter;

/**
 * {@link org.matsim.core.controler.listener.ControlerListener} that dumps the
 * complete plans regularly at the start of an iteration
 * ({@link ControlerConfigGroup#getWritePlansInterval()} as well as in the first
 * iteration, just in case someone might check that the replanning worked
 * correctly in the first iteration.
 *
 * @author mrieser
 */
public class PlansDumping implements BeforeMobsimListener {

	static final private Logger log = Logger.getLogger(PlansDumping.class);

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Scenario sc = event.getControler().getScenario() ;
		int firstIteration = event.getControler().getConfig().controler().getFirstIteration() ;
		int writePlansInterval = sc.getConfig().controler().getWritePlansInterval() ;
		IterationStopWatch stopwatch = event.getControler().getStopwatch();
		OutputDirectoryHierarchy controlerIO = event.getControler().getControlerIO() ;
		if ((writePlansInterval > 0) && ((event.getIteration() % writePlansInterval== 0)
				|| (event.getIteration() == (firstIteration + 1)))) {
			stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			new PopulationWriter(sc.getPopulation()).write(controlerIO.getIterationFilename(event.getIteration(), "plans.xml.gz"));
			log.info("finished plans dump.");
			stopwatch.endOperation("dump all plans");
		}
	}

}
