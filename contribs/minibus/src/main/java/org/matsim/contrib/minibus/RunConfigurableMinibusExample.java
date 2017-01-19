/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.TicketMachineDefaultImpl;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Entry point, registers all necessary hooks
 * 
 * @author aneumann
 */
public final class RunConfigurableMinibusExample {

	private final static Logger log = Logger.getLogger(RunConfigurableMinibusExample.class);

	public static void main(final String[] args) {

		if(args.length == 0){
			log.info("Arg 1: config.xml is missing.");
			log.info("Check http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/ for an example.");
			System.exit(1);
		}

		Config config = ConfigUtils.loadConfig( args[0], new PConfigGroup() ) ;
		final PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.class) ; 

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Set<Id<TransitStopFacility>> subsidizedStops = new TreeSet<>() ;
		// subsidizedStops.add(...) ; // add subsidized stops
		
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setCreateGraphs(false);
		
		if ( true ) {
			throw new RuntimeException("the following is not possibly any more; just copy PModule and modify it to your needs.  kai, jan'17") ;
		}
//		builder.setTicketMachine(new TicketMachineI() {
//			TicketMachineI delegate = new TicketMachine(pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger() / 1000.0 ) ;
//			@Override public double getFare(StageContainer stageContainer) {
//				double fare = delegate.getFare(stageContainer) ;
//				if ( subsidizedStops.contains( stageContainer.getStopEntered() ) ) {
//					fare+=1. ; 
//					// this would increase the fare for the subsidized stops ...
//					// ... but I fear that this also means that the passengers have to pay this.  kai, jan'17
//				}
//				return fare ;
//			}
//		}) ;
		
		
		controler.addOverridingModule(new PModule()) ;


		controler.run();
	}		
}