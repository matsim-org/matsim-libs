/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MainWithMultithreadedModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package tutorial.programming.multiThreadedPlanStrategy;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class RunWithMultithreadedModule {

    public static void main(String[] args) {
        final Controler controler = new Controler(ConfigUtils.createConfig());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("myStrategy").toProvider(new javax.inject.Provider<PlanStrategy>() {
                    @Override
                    public PlanStrategy get() {

// This method is called exactly once by the framework. The PlanStrategy
// which we return here will live as long as the Controler.

// Adding an EventHandler which observes the simulation. In this example,
// it simply collects the Events.
                        final List<Event> collectedInformationAboutSimulation = new ArrayList<Event>();
                        controler.getEvents().addHandler(new BasicEventHandler() {
                            @Override
                            public void handleEvent(final Event event) {
// Events don't arrive in parallel, so my observer can use
// an unsynchronized data structure (in this case, the ArrayList).
                                collectedInformationAboutSimulation.add(event);
                            }

                            @Override
                            public void reset(int iteration) {
// This EventHandler lives as long as the Controler lives, so
// it needs to clear its memory if the idea is that it
// knows only about one iteration.
// (No conceptual problem though with collecting information
// from more iterations).
                                collectedInformationAboutSimulation.clear();
                            }
                        });

                        final PlanStrategyImpl myStrategy = new PlanStrategyImpl(new RandomPlanSelector<Plan, Person>());
                        myStrategy.addStrategyModule(new AbstractMultithreadedModule(controler.getConfig().global()) {

							@Inject private Provider<TripRouter> tripRouterProvider;
                            @Override
                            public PlanAlgorithm getPlanAlgoInstance() {
                                return new PlanAlgorithm() {
// This method is called n times if the framework wants to run n threads.

                                    TripRouter tripRouter = tripRouterProvider.get();

                                    @Override
                                    public void run(Plan plan) {
// Modify the plan. If I want a completely new plan, I need to clear the
// plan elements and insert new ones.

// doSomethingWith(plan);

// Now I use my observed data from the simulation.
// THIS is the bit where thread-safety comes into play.
// I can iterate over this List for the reason that
// ArrayList is thread-safe for reading.
                                        for (Event event : collectedInformationAboutSimulation) {
// doSomethingWithPlanAndEvent(plan, event);
                                        }

// Everything I use here must be prepared to be
// called by me in parallel.

// doSomethingWith(plan, somethingThreadSafe);


// In particular, tripRouters are for single-thread use only.
// doSomethingWith(plan, tripRouter)

// I could even be defensive and call getTripRouter() in this method,
// every time I need one.

                                    }
                                };
                            }
                        });
                        return myStrategy;
                    }
                });
            }
        });
    }

}
