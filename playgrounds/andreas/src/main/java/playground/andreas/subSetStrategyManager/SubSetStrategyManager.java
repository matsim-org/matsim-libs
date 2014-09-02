/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.subSetStrategyManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;

/**
 * @author mrieser
 */
public class SubSetStrategyManager extends StrategyManager {

	private Map<Set<Id>, StrategyManager> managers = new LinkedHashMap<Set<Id>, StrategyManager>();
	private boolean allowDefaultFallback = true;

	public void addSubset(final Set<Id> ids, final StrategyManager manager) {
		this.managers.put(ids, manager);
	}

	/**
	 * Specifies if the default strategies (strategies registered in this instance, e.g. using
	 * {@link SubSetStrategyManager#addStrategyForDefaultSubpopulation(PlanStrategy, double)}) should be used
	 * for all persons who are not listed in one of the special Id-sets
	 * ({@link #addSubset(Set, StrategyManager)}. Defaults to <code>true</code>.
	 *
	 * @param allowDefault
	 */
	public void setAllowDefaultFallback(boolean allowDefault) {
		this.allowDefaultFallback = allowDefault;
	}

	@Override
	protected void beforePopulationRunHook(Population population, ReplanningContext replanningContext) {
		super.beforePopulationRunHook(population, replanningContext);
		for (StrategyManager mgr : this.managers.values()) {
			for (GenericPlanStrategy<Plan, Person> strategy : mgr.getStrategiesOfDefaultSubpopulation()) {
				strategy.init(replanningContext);
			}
		}
	}

	@Override
	protected void afterRunHook(Population population) {
		super.afterRunHook(population);
		for (StrategyManager mgr : this.managers.values()) {
			for (GenericPlanStrategy<Plan, Person> strategy : mgr.getStrategiesOfDefaultSubpopulation()) {
				strategy.finish();
			}
		}
	}

	@Override
	public GenericPlanStrategy<Plan, Person> chooseStrategy(Person person, String subpopulation) {
		for (Map.Entry<Set<Id>, StrategyManager> e : this.managers.entrySet()) {
			Set<Id> ids = e.getKey();
			if (ids.contains(person.getId())) {
				StrategyManager m = e.getValue();
				return m.chooseStrategy(person, subpopulation);
			}
		}
		if (this.allowDefaultFallback) {
			return super.chooseStrategy(person, subpopulation);
		}
		return null;
	}
} 