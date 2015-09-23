/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
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
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.router.InvertedNetworkTripRouterFactoryModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author tthunig
 *
 */
public class TtBasicController {

	/**
	 * @param args the config file
	 */
	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig( args[0]) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
        
		// add the signals module if signal systems are used
		if (signalsConfigGroup.isUseSignalSystems()) {
			controler.addOverridingModule(new SignalsModule());
		}
				
		// add the module for link to link routing if enabled
		if (config.controler().isLinkToLinkRoutingEnabled()){
			controler.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		}
		
		controler.run();
	}

}
