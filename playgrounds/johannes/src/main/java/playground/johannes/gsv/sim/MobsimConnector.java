/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.sim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.TravelTime;
import playground.johannes.gsv.sim.cadyts.CadytsContext;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class MobsimConnector implements Mobsim {
	
	private static final Logger logger = Logger.getLogger(MobsimConnector.class);

	private final ParallelPseudoSim sim;
	
	private final Scenario scenario;
	
	private final EventsManager eventsManager;
	
	private final TravelTime roadTravelTimes;
	
	private CadytsContext cadytsContext;
	
	private boolean replanCandidates;
	
	public MobsimConnector(Scenario scenario, EventsManager eventsManager, CadytsContext cadytsContext, boolean replanCanditates) {
		sim = new ParallelPseudoSim(scenario.getConfig().global().getNumberOfThreads());
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		roadTravelTimes = MobsimConnectorFactory.getTravelTimeCalculator(1.5);
		
		this.cadytsContext = cadytsContext;
		this.replanCandidates = replanCanditates;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.Mobsim#run()
	 */
	@Override
	public void run() {
		logger.info("Running pseudo mobility simulation...");
		Population pop = scenario.getPopulation();
		
		Set<Plan> plans = new LinkedHashSet<Plan>(pop.getPersons().size());
		
		if (cadytsContext != null && cadytsContext.getODCalibrator() != null && replanCandidates) {
			logger.info(String.format("ODCalibrator active, simulating %s persons.", cadytsContext.getODCalibrator().getCandidates().size()));
			for (Person person : cadytsContext.getODCalibrator().getCandidates()) {
				plans.add(person.getSelectedPlan());
			}
		} else {
			for (Person person : pop.getPersons().values()) {
				plans.add(person.getSelectedPlan());
			}
		}
		
		sim.run(plans, scenario.getNetwork(), roadTravelTimes, eventsManager);
	}
}
