/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package playground.mzilske.city2000w;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.*;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.ParallelQSimulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.DefaultAgentFactory;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import playground.mzilske.freight.QSimAgentSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 10/31/11
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class City2000WQSimFactory implements MobsimFactory {

    private CarrierAgentTracker carrierAgentTracker;

    public City2000WQSimFactory(CarrierAgentTracker carrierAgentTracker) {
        this.carrierAgentTracker = carrierAgentTracker;
    }

    @Override
    public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();

		if (numOfThreads > 1) {
			SynchronizedEventsManagerImpl em = new SynchronizedEventsManagerImpl(eventsManager);
			ParallelQSimulation sim = new ParallelQSimulation(sc, em);
			return sim;
		} else {
			final QSim sim = new QSim(sc, eventsManager);
			if (sc.getConfig().scenario().isUseTransit()) {
				sim.getTransitEngine().setUseUmlaeufe(true);
				sim.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			}
            sim.addAgentSource(new QSimAgentSource(carrierAgentTracker.createPlans(), new DefaultAgentFactory(sim)));
			return sim;
		}
    }

}
