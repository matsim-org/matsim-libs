/* *********************************************************************** *
 * project: org.matsim.*
 * GuidedAgentFactory.java
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

/**
 * 
 */
package playground.johannes.teach.telematics.ha3;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.WithindayAgentLogicFactory;
import org.matsim.withinday.contentment.AgentContentment;
import org.matsim.withinday.routeprovider.RouteProvider;

import playground.johannes.eut.ForceReplan;
import playground.johannes.eut.PreventReplan;
import playground.johannes.itsc08.ReactRouteGuidance;

/**
 * @author illenberger
 *
 */
public class GuidedAgentFactory extends WithindayAgentLogicFactory {// implements IterationStartsListener{

	private final double equipmentFraction;
	
	private static final ForceReplan forceReplan = new ForceReplan();
	
	private static final PreventReplan preventReplan = new PreventReplan();
	
	private final ReactRouteGuidance router;
	
	private Random random;
	
	private Set<Person> guidedPersons;
	
	private Set<Person> unguidedPersons;
	
	private long randomSeed;
//	private Map<Person, Plan> selectedPlans;
	
	/**#
	 * @param network
	 * @param scoringConfig
	 */
	public GuidedAgentFactory(Network network,
			CharyparNagelScoringConfigGroup scoringConfig, TravelTime reactTTs, double fraction, long randomSeed) {
		super(network, scoringConfig);
		router = new ReactRouteGuidance(network, reactTTs);
		equipmentFraction = fraction;
		this.randomSeed = randomSeed;
	}

	@Override
	public AgentContentment createAgentContentment(WithindayAgent agent) {
//		selectedPlans = new HashMap<Person, Plan>();
		random.nextDouble();
		if(random.nextDouble() < equipmentFraction) {
			guidedPersons.add(agent.getPerson());
//			selectedPlans.put(agent.getPerson(), agent.getPerson().getSelectedPlan());
			return forceReplan;
		} else {
			unguidedPersons.add(agent.getPerson());
			return preventReplan;
		}
	}

	@Override
	public RouteProvider createRouteProvider() {
		return router;
	}
	
	public Set<Person> getGuidedPersons() {
		return guidedPersons;
	}
	
	public Set<Person> getUnguidedPersons() {
		return unguidedPersons;
	}

	public void reset() {
		random = new Random(randomSeed);
		guidedPersons = new HashSet<Person>();
		unguidedPersons = new HashSet<Person>();
	}

//	public void notifyIterationStarts(IterationStartsEvent event) {
//		if (selectedPlans != null) {
//			for (Person person : selectedPlans.keySet()) {
//				person.exchangeSelectedPlan(selectedPlans.get(person), false);
//			}
//		}
//	}

}
