/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingQsimFactory.java
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
package playground.thibautd.hitchiking.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class HitchHikingQsimFactory implements MobsimFactory {

    private final Controler controler;

	public HitchHikingQsimFactory(final Controler controler) {
		this.controler = controler;
	}

    @Override
    public Netsim createMobsim(
			final Scenario sc,
			EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		// make sure we simulate car pooling!
		Collection<String> mainModes = conf.getMainModes();
		if (!mainModes.contains( HitchHikingConstants.DRIVER_MODE )) {
			List<String> ms = new ArrayList<String>(mainModes);
			ms.add( HitchHikingConstants.DRIVER_MODE );
			conf.setMainModes( ms );
		}


        //QSim qSim = QSim.createQSimWithDefaultEngines(sc, eventsManager, netsimEngFactory);

		// default initialisation
		QSim qSim = new QSim(sc,  eventsManager );
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		// set specific engine
		PassengerQueuesManager queuesManager = new PassengerQueuesManager( eventsManager );
		qSim.addMobsimEngine( queuesManager );
		qSim.addDepartureHandler( queuesManager );
        AgentFactory agentFactory =
			new HitchHikerAgentFactory(
					new TransitAgentFactory(qSim),
                    controler.getScenario().getNetwork(),
					controler.getTripRouterProvider().get(),
					queuesManager,
					eventsManager,
					controler.getConfig().planCalcScore().getMonetaryDistanceCostRateCar());

        if (sc.getConfig().scenario().isUseTransit()) {
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }
        
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
    }

}
