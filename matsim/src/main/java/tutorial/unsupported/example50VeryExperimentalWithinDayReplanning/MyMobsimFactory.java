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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * @author nagel
 *
 */
public class MyMobsimFactory implements MobsimFactory {
	private static final Logger log = Logger.getLogger(MyMobsimFactory.class);

	private PersonalizableTravelCost travCostCalc;
	private PersonalizableTravelTime travTimeCalc;

	private enum ReplanningType { general, carPlans }
	private ReplanningType replanningType = ReplanningType.general ;

	MyMobsimFactory( PersonalizableTravelCost travelCostCalculator, PersonalizableTravelTime travelTimeCalculator ) {
		this.travCostCalc = travelCostCalculator ;
		this.travTimeCalc = travelTimeCalculator ;
	}

	@Override
	public Simulation createMobsim(Scenario sc, EventsManager events) {

		Mobsim mobsim = new QSim( sc, events ) ;

		if ( replanningType.equals( ReplanningType.general ) ) {
			mobsim.addQueueSimulationListeners(new MyWithinDayMobsimListener(this.travCostCalc,this.travTimeCalc)) ;

//		} else if ( replanningType.equals( ReplanningType.carPlans ) ) {
//			mobsim.addQueueSimulationListeners(new MyWithinDayMobsimListener2(this.travCostCalc,this.travTimeCalc)) ;
			// the above should work (at the same level as the remainder of the package if not better), but it uses
			// EditRoutes, which at the time of moving this material into "matsim" was still in "playground" and thus
			// cannot be referenced from here.  MyWithinDayMobsimListener2 is unter playground.kai.usecases.withinday .  kai, dec'10

		}

		mobsim.setAgentFactory( new ExperimentalBasicWithindayAgentFactory(mobsim) ) ;

		return mobsim ;
	}

}
