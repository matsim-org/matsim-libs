/* *********************************************************************** *
 * project: org.matsim.*
 * DgWithindayMobsimFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.satellic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;


/**
 * @author dgrether
 *
 */
public class DgWithindayMobsimFactory implements MobsimFactory {

	/**
	 * @see org.matsim.core.mobsim.framework.MobsimFactory#createMobsim(org.matsim.api.core.v01.Scenario, org.matsim.core.api.experimental.events.EventsManager)
	 */
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim qSim = new QSim(sc, eventsManager, new DefaultQSimEngineFactory());
		AgentFactory agentFactory = new DgWithindayAgentFactory(qSim, MatsimRandom.getLocalInstance());
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
