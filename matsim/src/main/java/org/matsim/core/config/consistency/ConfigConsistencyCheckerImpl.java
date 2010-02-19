/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigConsistencyCheckerImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.config.consistency;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;


/**
 * Implementation of the ConfigCosistencyChecker interface.
 * @author dgrether
 *
 */
public class ConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {
	
	private static final Logger log = Logger
			.getLogger(ConfigConsistencyCheckerImpl.class);
	
	public void checkConsistency(Config config) {
		this.checkScenarioFeaturesEnabled(config);
		this.checkEventsFormatLanesSignals(config);
		this.checkTravelTimeCalculationRoutingConfiguration(config);
		this.checkLaneDefinitionRoutingConfiguration(config);
		this.checkSignalSystemConfiguration(config);
	}
	
	private void checkEventsFormatLanesSignals(Config c) {
		ScenarioConfigGroup scg = c.scenario();
		if (scg.isUseLanes() || scg.isUseSignalSystems()) {
			if (!c.controler().getEventsFileFormats().contains(EventsFileFormat.xml)){
				log.error("Xml events are not enabled, but lanes and eventually signal systems" +
						"are enalbed. Events from this features will only be written to the xml format, consider" +
						"to add xml events in the controler config module");
			}
		}
	}

	private void checkScenarioFeaturesEnabled(Config c) {
		ScenarioConfigGroup scg = c.scenario();
		if (!scg.isUseLanes() && scg.isUseSignalSystems()) {
			throw new IllegalStateException("Cannot use the signal systems framework without" +
			"using lanes. Please enable lanes in scenario config group");
		}
		if (scg.isUseSignalSystems() && c.getQSimConfigGroup() == null){
		  log.warn("The signal system implementation is only supported by the org.matsim.ptproject.qsim mobility simulation that is not activated. Please make sure you are using the correct" +
		  		"mobility simulation. This warning can be ingored if a customized mobility simulation developed outside of org.matsim is used and set correctly.");
		}
	}

	private void checkSignalSystemConfiguration(Config config) {
			if ((config.signalSystems().getSignalSystemFile() != null) &&
					(config.signalSystems().getSignalSystemConfigFile() == null)){
				log.error("Signal systems are defined in config however there is no" +
						"configuration file for the systems. This may not be fatal if " +
				"incode custom configuration is implemented. ");
			}
			
			if ((config.signalSystems().getSignalSystemFile() == null) &&
					(config.signalSystems().getSignalSystemConfigFile() != null)){
				throw new IllegalStateException("SignalSystemConfigurations are set " +
				"in config but no input file for the SignalSystems is specified.!");
			}
			
			if ((config.network().getLaneDefinitionsFile() == null) &&
					(config.signalSystems().getSignalSystemFile() != null) && 
					(config.signalSystems().getSignalSystemConfigFile() != null)) {
				throw new IllegalStateException("Cannot use the signal systems framework without" +
				"a definition of lanes.");
			}
	}


	private void checkTravelTimeCalculationRoutingConfiguration(Config config){
		if (config.controler().isLinkToLinkRoutingEnabled() && 
				!config.travelTimeCalculator().isCalculateLinkToLinkTravelTimes()){
			throw new IllegalStateException("LinkToLinkRouting is activated in config and" +
					" link to link traveltime calculation is not enabled but required!");
		}
		
		if (config.travelTimeCalculator().isCalculateLinkTravelTimes() &&
				config.travelTimeCalculator().isCalculateLinkToLinkTravelTimes() &&
				!config.controler().isLinkToLinkRoutingEnabled()) {
			log.warn("Config enables link travel time calculation and link to link " +
					"travel time calculation. This requires at least twice as much memory as " +
					"if only one method is used, however it might be necessary to enable " +
					"a certain module configuration.");
		}
		
		if (!config.travelTimeCalculator().isCalculateLinkTravelTimes()){
			log.warn("Link travel time calculation is switched off, be aware that this optimization" +
					"might not work with all modules. ");
		}
		
		if (config.travelTimeCalculator().isCalculateLinkToLinkTravelTimes() && 
				config.getQSimConfigGroup().isRemoveStuckVehicles()){
			throw new IllegalStateException("Link to link travel time calculation is not" +
					"available if using the remove stuck vehicles option!");
		}
		
		
	}
	
	
	private void checkLaneDefinitionRoutingConfiguration(Config config) {
		if ((config.scenario().isUseLanes()) && 
		    !config.controler().isLinkToLinkRoutingEnabled()){
		  	log.warn("Using lanes without enabling linktolinkrouting might not lead to expected simulation results"); 
		   }
	}

}
