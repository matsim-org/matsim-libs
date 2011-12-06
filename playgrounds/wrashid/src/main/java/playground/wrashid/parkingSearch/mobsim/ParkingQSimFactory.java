/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingQSimFactory.java
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

package playground.wrashid.parkingSearch.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;

import playground.wrashid.parkingSearch.withinday.InsertParkingActivities;
import playground.wrashid.parkingSearch.withinday.ParkingInfrastructure;

/**
 * This class is basically a copy of QSimFactory but instead of a
 * PopulationAgentSource, a ParkingPopulationAgentSource object is
 * created and added to the QSim.
 * 
 * Can be removed when a DefaultAgentSource can be set in the QSimFactory.
 *
 * @author cdobler
 */
public class ParkingQSimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(ParkingQSimFactory.class);

    private final InsertParkingActivities insertParkingActivities;
    private final ParkingInfrastructure parkingInfrastructure;
    
    public ParkingQSimFactory(InsertParkingActivities insertParkingActivities, ParkingInfrastructure parkingInfrastructure) {
    	this.insertParkingActivities = insertParkingActivities;
    	this.parkingInfrastructure = parkingInfrastructure;
    }
    
    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        if (numOfThreads > 1) {
            eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
            netsimEngFactory = new ParallelQNetsimEngineFactory();
            log.info("Using parallel QSim with " + numOfThreads + " threads.");
        } else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }
        QSim qSim = new QSim(sc, eventsManager, netsimEngFactory);
        AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
        AgentSource agentSource = new ParkingPopulationAgentSource(sc.getPopulation(), agentFactory, qSim, 
        		insertParkingActivities, parkingInfrastructure);
        qSim.addAgentSource(agentSource);
        return qSim;

    }

}
