
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
<p>The StrategyManager controls the replanning of all agents in a population.</p>

<p>Various strategies can be created and added to the
{@link org.matsim.core.replanning.StrategyManager}. The
<code>StrategyManager</code> selects for each agent randomly one of the
strategies. When adding strategies to the strategy manager, a weight must be
provided which influences the probability the strategy gets chosen for a person.
The probability for a strategy to be chosen is defined as the strategy's weight
divided by the sum of all strategies' weights.</p>

<p>A {@link org.matsim.core.replanning.PlanStrategyImpl} always consists
of exactly one <code>PlanSelector</code> and zero or more <code>StrategyModule</code>s.
A {@link org.matsim.core.replanning.selectors.PlanSelector
PlanSelector} selects one of the agents' existing plans, while
a {@link org.matsim.api.core.v01.replanning.PlanStrategyModule
StrategyModule} modifies a plan. A <code>StrategyModule</code> can
change the route information, time information, change the sequence of
activities, change locations of activities and so on.</p>

<p>After a plan is selected, the strategy makes a copy of the plan before it is
modified by a strategy module. If a strategy contains no strategy module, the
plan will not be duplicated. If a strategy contains multiple strategy modules,
the plan will be modified by one strategy module after the other, in the order
the strategy modules were added to the strategy.</p>


<h2>Example</h2>
<p>The strategy manager contains the following 5 strategies:
<ol>
	<li>the time information of the currently selected plan is modified</li>
	<li>the route information of a randomly selected plan is modified</li>
	<li>first, the time information of a randomly selected plan is modified, and afterwards new routes are assigned according to the new times</li>
	<li>the plan with the best score is selected for the agent, but not modified</li>
	<li>a random plan is selected from the agent, but plans with higher score have a higher probability to be selected</li>
</ol>
</p>

<p>This could be visualized as follows, including the probabilities each strategy is chosen:
<pre>
StrategyManager
    |
    +--- 5% -- KeepSelected
    |          |_ TimeAllocationMutator
    |
    +--- 7% -- RandomPlanSelector
    |          |_ ReRoute
    |
    +--- 8% -- RandomPlanSelector
    |          |_ TimeAllocationMutator
    |          |_ ReRoute
    |
    +-- 75% -- BestScoreSelector
    |
    +--- 5% -- ExpBetaSelector
</pre>
</p>

<p>And here is the corresponding code to setup such a StrategyManager:
	<pre>
	StrategyManager manager = new StrategyManager();

	// strategy1
	PlanStrategy strategy1 = new PlanStrategy(new KeepSelected());
	strategy1.addStrategyModule(new TimeAllocationMutator());

	// strategy2
	PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
	strategy2.addStrategyModule(new ReRoute());

	// strategy3
	PlanStrategy strategy3 = new PlanStrategy(new RandomPlanSelector());
	strategy3.addStrategyModule(new TimeAllocationMutator());
	strategy3.addStrategyModule(new ReRoute());

	// strategy4
	PlanStrategy strategy4 = new PlanStrategy(new BestScoreSelector());

	// strategy5
	PlanStrategy strategy5 = new PlanStrategy(new ExpBetaSelector());

	// add the strategies to the manager
	manager.addStrategy(strategy1, 0.05);
	manager.addStrategy(strategy2, 0.07);
	manager.addStrategy(strategy3, 0.08);
	manager.addStrategy(strategy4, 0.75);
	manager.addStrategy(strategy5, 0.05);

	// run it
	// Plans plans = new Plans();
	manager.run(plans);
</pre>
</p>
*/
package org.matsim.core.replanning;
