/* *********************************************************************** *
 * project: org.matsim.*
 * DgScenarioUtils
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
package playground.dgrether.signalsystems.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author dgrether
 *
 */
public class DgScenarioUtils {

	private static final boolean loadPopulation = true;
	
	public static Scenario loadScenario(String net, String pop, String lanesFilename, String signalsFilename,
			String signalGroupsFilename, String signalControlFilename){
		Config c2 = ConfigUtils.createConfig();
		c2.qsim().setUseLanes(true);
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(c2,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);
		
		c2.network().setInputFile(net);
		if (loadPopulation){
			c2.plans().setInputFile(pop);
		}
		c2.network().setLaneDefinitionsFile(lanesFilename);
		signalsConfigGroup.setSignalSystemFile(signalsFilename);
		signalsConfigGroup.setSignalGroupsFile(signalGroupsFilename);
		signalsConfigGroup.setSignalControlFile(signalControlFilename);
		
		Scenario scenario = ScenarioUtils.loadScenario(c2);
		
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(c2).loadSignalsData());
		
		return scenario;
	}

}
