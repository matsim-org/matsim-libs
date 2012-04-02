/* *********************************************************************** *
 * project: org.matsim.*
 * MyMobsimFactory.java
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
package tutorial.unsupported.example50VeryExperimentalWithinDayReplanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;

/**
 * @author nagel
 *
 */
public class MyMobsimFactory implements MobsimFactory {

	private PersonalizableTravelDisutility travCostCalc;
	private PersonalizableTravelTime travTimeCalc;

	private enum ReplanningType { general, carPlans }
	private ReplanningType replanningType = ReplanningType.general ;

	MyMobsimFactory( PersonalizableTravelDisutility travelCostCalculator, PersonalizableTravelTime travelTimeCalculator ) {
		this.travCostCalc = travelCostCalculator ;
		this.travTimeCalc = travelTimeCalculator ;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager events) {
		QSim qSim = new QSim(sc, events, new DefaultQSimEngineFactory());
		if ( replanningType.equals( ReplanningType.general ) ) {
			qSim.addQueueSimulationListeners(new MyWithinDayMobsimListener(this.travCostCalc,this.travTimeCalc)) ;
		} else if ( replanningType.equals( ReplanningType.carPlans ) ) {
			qSim.addQueueSimulationListeners(new MyWithinDayMobsimListener2(this.travCostCalc,this.travTimeCalc)) ;
		}
		ExperimentalBasicWithindayAgentFactory fac = new ExperimentalBasicWithindayAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), fac, qSim);
		qSim.addAgentSource(agentSource);		
		return qSim ;
	}

}
