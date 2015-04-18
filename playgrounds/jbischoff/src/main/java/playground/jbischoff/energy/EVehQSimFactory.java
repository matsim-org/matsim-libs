/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingWithSimplisticRelocationQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.jbischoff.energy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

import playground.jbischoff.energy.charging.ChargingHandler;

/**
 *
 * @author jbischoff
 */
public class EVehQSimFactory implements MobsimFactory {

    private ChargingHandler ch;
    private EnergyConsumptionTracker ect;

    public EVehQSimFactory(ChargingHandler ch, EnergyConsumptionTracker ect)
    {
        this.ch  = ch;
        this.ect = ect;
    }
    
    @Override
    public RunnableMobsim createMobsim(
            final Scenario sc,
            final EventsManager eventsManager) {
        final QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        eventsManager.addHandler(ch);
        eventsManager.addHandler(ect);
        final QSim qSim = new QSim( sc, eventsManager );

        final ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);

        QNetsimEngineModule.configure(qSim);
		qSim.addMobsimEngine(ch);

	       final TeleportationEngine teleportationEngine = new TeleportationEngine();
	        qSim.addMobsimEngine(teleportationEngine);

	        final AgentFactory agentFactory = createAgentFactory( qSim , sc );

	        if (sc.getConfig().network().isTimeVariantNetwork()) {
	            qSim.addMobsimEngine(new NetworkChangeEventsEngine());      
	        }

	        final PopulationAgentSource agentSource =
	            new PopulationAgentSource(
	                    sc.getPopulation(),
	                    agentFactory,
	                    qSim);
	        qSim.addAgentSource(agentSource);
	        return qSim;
	    }

	private static AgentFactory createAgentFactory(
			final QSim qSim,
			final Scenario sc) {
		if (sc.getConfig().scenario().isUseTransit()) {
			final AgentFactory agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
			return agentFactory;
		}

		return new DefaultAgentFactory(qSim);
	}

}

