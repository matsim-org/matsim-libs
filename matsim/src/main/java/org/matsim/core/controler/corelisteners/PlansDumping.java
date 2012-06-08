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

package org.matsim.core.controler.corelisteners;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.knowledges.Knowledges;

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
	private Scenario sc ;
	private int writePlansInterval, firstIteration ; 
	private IterationStopWatch stopwatch ;
	private ControlerIO controlerIO;
	public PlansDumping(Scenario sc, int firstIteration, int writePlansInterval, IterationStopWatch stopwatch, 
			ControlerIO controlerIO ) {
		this.sc = sc ;
		this.firstIteration = firstIteration ;
		this.writePlansInterval = writePlansInterval ;
		this.stopwatch = stopwatch ;
		this.controlerIO = controlerIO ;
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
//		Controler controler = event.getControler();
		if ((writePlansInterval > 0) && ((event.getIteration() % writePlansInterval== 0)
				|| (event.getIteration() == (firstIteration + 1)))) {
			stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			Knowledges k = null ;
			if ( sc.getConfig().scenario().isUseKnowledges() ) {
				k = ((ScenarioImpl) sc).getKnowledges();
			} else {
				k = ((ScenarioImpl) sc).retrieveNotEnabledKnowledges();
				// seems that this call is there for some backwards compatibility ... reading knowledges into the 
				// population even when knowledges is not enabled.  kai, mar'12
			}
			new PopulationWriter(sc.getPopulation(), sc.getNetwork(), k)
				.write(controlerIO.getIterationFilename(event.getIteration(), "plans.xml.gz"));
			log.info("finished plans dump.");
			stopwatch.endOperation("dump all plans");
		}
	}

}
