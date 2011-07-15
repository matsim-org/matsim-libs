/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansReplanning.java
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
package playground.thibautd.jointtripsoptimizer.replanning;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;

/**
 * {@link ReplanningListener} allowing to pass the cliques to the {@link JointStrategyManager}
 * @author thibautd
 */
public class JointPlansReplanning implements ReplanningListener {
	private final static Logger log = Logger.getLogger(JointPlansReplanning.class);

	// definition of log messages
	private final static String SUCCESS = "Cliques succesfully passed to the strategy manager";
	private final static String FAIL = "Failed to extract cliques from the population, individuals passed to the strategy manager instead";

	/**
	 * Same as in the "PlansReplanning" class, but passes the population of the
	 * cliques to the StrategyManager, if it exists.
	 * {@inheritDoc}
	 * @see ReplanningListener#notifyReplanning(ReplanningEvent)
	 */
	public void notifyReplanning(ReplanningEvent event) {
		Controler controler = event.getControler();
		Population population = controler.getPopulation();
		Population populationToTreat = null;
		PopulationOfCliques cliques = null;
		try {
			// if the population is a PopulationWithCliques, run the
			// StrategyManager on them
			cliques = ((PopulationWithCliques) population).getCliques();
			log.info(SUCCESS);
			populationToTreat = (Population) cliques;
		} catch(ClassCastException e) {
			// else, run the strategies on individuals
			log.warn(FAIL);
			populationToTreat = population;
		} finally {
			controler.getStrategyManager().run(populationToTreat, event.getIteration());
		}
	}
}
