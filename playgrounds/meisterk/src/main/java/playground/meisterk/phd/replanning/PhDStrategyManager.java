/* *********************************************************************** *
 * project: org.matsim.*
 * PhDStrategyManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.phd.replanning;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManagerImpl;

public class PhDStrategyManager extends StrategyManagerImpl {

	/**
	 * Records which person is selected for what plan strategy.
	 */
	private TreeMap<String, Set<Person>> personTreatment = new TreeMap<String, Set<Person>>();

//	@Override
//	public void run(Population population, int iteration) {
//		// keep selection of persons to plan strategies until right before new selection 
//		this.personTreatment.clear();
//		super.run(population, iteration);
//	}
	//
	// replaced by "beforeRunHook".  This really exchanges the sequence of "handleChangeRequests" and "personTreatment.clear()",
	// but it looks to me like this should not matter.  kai, sep'10
	
	@Override
	protected void beforePopulationRunHook( @SuppressWarnings("unused") Population population ) {
		this.personTreatment.clear() ;
	}

//	@Override
//	public void run(Population population) {
//		
//		// initialize all strategies
//		for (PlanStrategy strategy : this.getStrategies() ) {
//			strategy.init();
//		}
//		
//		// then go through the population and assign each person to a strategy
//		for (Person person : population.getPersons().values()) {
//			
//			if ((this.getMaxPlansPerAgent() > 0) && (person.getPlans().size() > this.getMaxPlansPerAgent())) {
//				removePlans((PersonImpl) person, this.getMaxPlansPerAgent());
//			}
//			PlanStrategy strategy = this.chooseStrategy();
//			
//			beforeStrategyRunHook(person, strategy);
//			
//			if (strategy != null) {
//				strategy.run(person);
//			} else {
//				Gbl.errorMsg("No strategy found!");
//			}
//		}
//		// finally make sure all strategies have finished there work
//		for (PlanStrategy strategy : this.getStrategies() ) {
//			strategy.finish();
//		}
//	}
	// the modification from the parent class is replaced by "beforeStrategyRunHook" below.   kai, sep'10

	@Override
	protected void beforeStrategyRunHook(Person person, PlanStrategy strategy) {
		Set<Person> personIdSet = null;
		if (this.personTreatment.containsKey(strategy.toString())) {
			personIdSet = this.personTreatment.get(strategy.toString());
		} else {
			personIdSet = new HashSet<Person>();
			this.personTreatment.put(strategy.toString(), personIdSet);
		}
		personIdSet.add(person);
	}

	public Map<String, Set<Person>> getPersonTreatment() {
		return Collections.unmodifiableMap(this.personTreatment);
	}

}
