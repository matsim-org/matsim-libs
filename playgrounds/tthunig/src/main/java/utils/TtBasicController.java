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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import signals.CombinedSignalsModule;


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
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
        
		// add the signals module if signal systems are used
		if (signalsConfigGroup.isUseSignalSystems()) {
			// the combined signals module works for a lot of different signal controller, e.g. planbased, sylvia, downstream, laemmer...
			controler.addOverridingModule(new CombinedSignalsModule());
		}
				
		// add analysis tools
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
			}
		});
		
		controler.run();
	}

}
