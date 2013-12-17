/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimFactory.java
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

package playground.christoph.evacuation.mobsim;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.simengine.MultiModalDepartureHandler;
import org.matsim.contrib.multimodal.simengine.MultiModalSimEngine;
import org.matsim.contrib.multimodal.simengine.MultiModalSimEngineFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelPassengerQNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.withinday.mobsim.WithinDayEngine;

import playground.christoph.evacuation.config.EvacuationConfig;

/**
 * @author cdobler
 */
public class EvacuationQSimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(EvacuationQSimFactory.class);
    
    private final WithinDayEngine withinDayEngine;
    private final ObjectAttributes householdObjectAttributes;
    private final JointDepartureOrganizer jointDepartureOrganizer;
    private final Map<String, TravelTime> multiModalTravelTimes;
    
    public EvacuationQSimFactory(WithinDayEngine withinDayEngine, ObjectAttributes householdObjectAttributes,
    		JointDepartureOrganizer jointDepartureOrganizer, Map<String, TravelTime> multiModalTravelTimes) {
    	this.withinDayEngine = withinDayEngine;
    	this.householdObjectAttributes = householdObjectAttributes;
    	this.jointDepartureOrganizer = jointDepartureOrganizer;
    	this.multiModalTravelTimes = multiModalTravelTimes;
    }
    
    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        
		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		/*
		 * Create a PassengerQNetsimEngine and add its PassengerDepartureHandler
		 * as well as its (super)VehicularDepartureHandler to the QSim.
		 * The later one handles non-joint departures.
		 */
		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		if (numOfThreads > 1) {
			if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);        		
			}
			ParallelPassengerQNetsimEngine netsimEngine = new ParallelPassengerQNetsimEngine(qSim, MatsimRandom.getLocalInstance(), jointDepartureOrganizer);
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
			qSim.addDepartureHandler(netsimEngine.getVehicularDepartureHandler());

			log.info("Using parallel PassengerQSim with " + numOfThreads + " threads.");
		} else {
			PassengerQNetsimEngine netsimEngine = new PassengerQNetsimEngine(qSim, MatsimRandom.getLocalInstance(), jointDepartureOrganizer);
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
			qSim.addDepartureHandler(netsimEngine.getVehicularDepartureHandler());
		}
			
		MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, this.multiModalTravelTimes);
		qSim.addMobsimEngine(multiModalEngine);
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) sc.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup));
		
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		
		qSim.addMobsimEngine(new HouseholdsInformer(((ScenarioImpl) sc).getHouseholds(), EvacuationConfig.informAgentsRayleighSigma, 
				EvacuationConfig.householdsInformerRandomSeed + EvacuationConfig.deterministicRNGOffset));
		// increase initial value in case additional iterations will be performed
//		this.householdsInformerRNG++;
//		EvacuationConfig.householdsInformerRandomSeed++;
		
        AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
        AgentSource agentSource = new EvacuationPopulationAgentSource(sc, agentFactory, qSim, this.householdObjectAttributes);
        qSim.addAgentSource(agentSource);
        
        qSim.addMobsimEngine(withinDayEngine);
        
        return qSim;
    }

}
