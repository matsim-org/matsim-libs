/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import org.matsim.contrib.accidents.handlers.AnalysisEventHandler;
import org.matsim.contrib.accidents.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.accidents.moneyTravelDisutility.MoneyTravelDisutilityModule;


/**
* @author ikaddoura
*/

public class AccidentsModule extends AbstractModule {

	private final AccidentsConfigGroup accidentsConfigGroup;
	private final Scenario scenario;
	
	public AccidentsModule(Scenario scenario) {
		
		this.scenario = scenario;
		
		ConfigUtils.addOrGetModule(scenario.getConfig(), AccidentsConfigGroup.class);
		this.accidentsConfigGroup = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
	}

	@Override
	public void install() {
		
		if (accidentsConfigGroup.isEnableAccidentsModule()) {
			
			AccidentsContext accidentsContext = new AccidentsContext();
			this.bind(AccidentsContext.class).toInstance(accidentsContext);
			
			this.bind(AnalysisEventHandler.class).asEagerSingleton();						
			this.addEventHandlerBinding().to(AnalysisEventHandler.class);
			
			if (accidentsConfigGroup.isInternalizeAccidentCosts()) {
				
				// adjust the travel disutility (toll-sensitive routing)
				
				final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scenario.getConfig().planCalcScore()));
//				factory.setSigma(3.0); // with randomization
				factory.setSigma(0.0); // without randomization

				install(new MoneyTravelDisutilityModule(TransportMode.car, factory, null));
			}
			
			this.addControlerListenerBinding().to(AccidentControlerListener.class);
		}
				
	}

}

