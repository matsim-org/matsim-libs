/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.mobsimcheckpointing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class MobsimCheckpointing {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler controler = new Controler( scenario ) ;
		
		final MobsimBeforeSimStepListener ee = new MobsimBeforeSimStepListener() {
			@Override
			public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent ff) {
				double now = ff.getSimulationTime() ;
				QSim qsim = (QSim) ff.getQueueSimulation() ;

				// we can either go through all persons and find their positions, or through the simulation and find all items.  Or both
				
				// yyyy the following design is really very ugly; probably better do something where objects checkpoint themselves.  ????
				
				for ( NetsimLink ll :  qsim.getNetsimNetwork().getNetsimLinks().values() ) {
					ll.getAllNonParkedVehicles() ;
					ll.getAllVehicles() ;
				}

				for ( MobsimAgent ag : qsim.getAgents() ) {
					switch( ag.getState() ) {
					case ABORT:
						// can this happen at all?
						break;
					case ACTIVITY:
						// Need: activity location, activity end time.
						// ...

						if ( ag instanceof PlanAgent ) {
							PlanAgent pda = (PlanAgent) ag ;
							Activity act = (Activity) pda.getCurrentPlanElement() ; // would that be enough, or do we need the "index"?
						}
						break;
					case LEG:
						if ( ag instanceof PlanAgent ) {
							PlanAgent pa = (PlanAgent) ag ;
							Leg leg = (Leg) pa.getCurrentPlanElement() ; // would that be enough, or do we need the "index"?
							Route route = leg.getRoute() ;
							if ( route instanceof NetworkRoute ) {
								NetworkRoute nr = (NetworkRoute) route ;
							}
							// ===
							PersonDriverAgentImpl pda = (PersonDriverAgentImpl) ag ;
							
						}
						break;
					default:
						break;} 
				}
				
				
			}
		} ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().toInstance(ee);
			}
		});
		;
		
		controler.run();
	}

}
