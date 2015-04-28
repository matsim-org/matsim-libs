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

package playground.wrashid.parkingSearch.withindayFW.core.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.withinday.mobsim.WithinDayEngine;
import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;

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
    private final WithinDayEngine replanningManager;

    public ParkingQSimFactory(InsertParkingActivities insertParkingActivities, ParkingInfrastructure parkingInfrastructure,
    		WithinDayEngine replanningManager) {
    	this.insertParkingActivities = insertParkingActivities;
    	this.parkingInfrastructure = parkingInfrastructure;
    	this.replanningManager = replanningManager;
    }

    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }


		QSim qSim1 = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim1.addMobsimEngine(teleportationEngine);
		qSim1.addMobsimEngine(replanningManager);
        QSim qSim = qSim1;
        AgentFactory agentFactory = new DefaultAgentFactory(qSim);
        AgentSource agentSource = new ParkingPopulationAgentSource(sc.getPopulation(), agentFactory, qSim, 
        		insertParkingActivities, parkingInfrastructure, conf.getNumberOfThreads());
        qSim.addAgentSource(agentSource);
        return qSim;

    }

}
