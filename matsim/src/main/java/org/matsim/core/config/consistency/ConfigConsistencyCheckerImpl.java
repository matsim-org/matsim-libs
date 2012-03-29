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
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

/**
 * Implementation of the ConfigCosistencyChecker interface.
 *
 * @author dgrether
 */
public class ConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {

	private static final Logger log = Logger
			.getLogger(ConfigConsistencyCheckerImpl.class);

	public ConfigConsistencyCheckerImpl() { // explicit constructor so that I can eclipse-search for instantiation.  kai, may'11
		// nothing to do
	}

	@Override
	public void checkConsistency(final Config config) {
		this.checkScenarioFeaturesEnabled(config);
		this.checkEventsFormatLanesSignals(config);
		this.checkTravelTimeCalculationRoutingConfiguration(config);
		this.checkLaneDefinitionRoutingConfiguration(config);
		this.checkTransitReplanningConfiguration(config);
		this.checkPlanCalcScore(config);
		this.checkMobsimSelection(config) ;
		this.checkMultimodalMobsim(config);
	}

	/**
	 * Design comments:<ul>
	 * <li> This is so complicated since currently it is possible to define some mobsim in the controler config module
	 * but still run the jdeqsim.  The logical behavior would be to run the defined mobsim and to ignore the jdeqsim.
	 * But this would silently change the behavior for people who have used it in that way.  If anybody finds this 
	 * after, say, a year from now, it could/should be simplified (and made more restrictive).  kai, mar'12
	 * </ul>
	 */
	private void checkMobsimSelection(final Config config) {
		if ( config.getModule("JDEQSim")!=null && config.controler().getMobsim() != null ) {
			if ( !config.controler().getMobsim().equalsIgnoreCase(MobsimType.JDEQSim.toString()) ) {
				throw new RuntimeException( "config module for JDEQSim defined but other mobsim selected in controler config" +
						" module; aborting since there is no way to fix this AND remain backwards compatible.\n" +
						" Either select jdeqsim in the controler config OR remove the jdeqsim config module.") ;
			}
		}
			
		if (config.controler().getMobsim() == null) {
			log.warn("You should specify which mobsim is to used in the configuration (controler.mobsim).");
		} else if ( config.controler().getMobsim().equalsIgnoreCase( MobsimType.queueSimulation.toString() ) ) {
			if ( config.simulation() == null ) {
				config.addSimulationConfigGroup(new SimulationConfigGroup()) ;
				for ( MobsimType mType : MobsimType.values() ) {
					if ( mType != MobsimType.queueSimulation ) {
						checkForConfigModulesOfUnselectedMobsims(config, mType);
					}
				}
			}
			for ( MobsimType mType : MobsimType.values() ) {
				if ( mType != MobsimType.queueSimulation ) {
					config.removeModule(mType.toString());
				}
			}
		} else if ( config.controler().getMobsim().equalsIgnoreCase( MobsimType.qsim.toString() ) ) {
			if ( config.getQSimConfigGroup() == null ) {
				config.addQSimConfigGroup(new QSimConfigGroup()) ;
				for ( MobsimType mType : MobsimType.values() ) {
					if ( mType != MobsimType.qsim ) {
						checkForConfigModulesOfUnselectedMobsims(config, mType);
					}
				}
			}
			for ( MobsimType mType : MobsimType.values() ) {
				if ( mType != MobsimType.qsim ) {
					config.removeModule(mType.toString());
				}
			}
		} else if ( config.controler().getMobsim().equalsIgnoreCase( MobsimType.JDEQSim.toString() ) ) {
			if ( config.getModule(MobsimType.JDEQSim.toString()) == null ) {
				log.warn("JDEQSim does not seem to have a typed (= preconfigured) config group; " +
				"thus cannot load it; thus cannot print configuration options into logfile.  kai, mar'12") ;
				for ( MobsimType mType : MobsimType.values() ) {
					if ( mType != MobsimType.JDEQSim ) {
						checkForConfigModulesOfUnselectedMobsims(config, mType);
					}
				}
			}
			for ( MobsimType mType : MobsimType.values() ) {
				if ( mType != MobsimType.JDEQSim ) {
					config.removeModule(mType.toString());
				}
			}
		} else if ( config.controler().getMobsim().equalsIgnoreCase( MobsimType.multimodalQSim.toString() ) ) {
			if ( config.getModule(MobsimType.multimodalQSim.toString()) == null ) {
				config.addModule(MultiModalConfigGroup.GROUP_NAME, new MultiModalConfigGroup() ) ;
				for ( MobsimType mType : MobsimType.values() ) {
					if ( mType != MobsimType.multimodalQSim ) {
						checkForConfigModulesOfUnselectedMobsims(config, mType);
					}
				}
			}
			for ( MobsimType mType : MobsimType.values() ) {
				if ( mType != MobsimType.multimodalQSim ) {
					config.removeModule(mType.toString());
				}
			}
		} else {
			log.warn("mobsim type not known to this config consistency checker.  Assuming you are using your own mobsim, " +
					"and you know what you are doing.") ;
		}

		// older checks, valid for the implicit mobsim selection by putting in the corresponding config group.
		if ( config.simulation()!=null ) {
			if ( config.getQSimConfigGroup()!=null ) {
				log.warn("have both `simulation' and `qsim' config groups; presumably both are defined in" +
						" the config file; removing the `simulation' config group; in future versions, this" +
						" may become a fatal error") ;
				config.removeModule( SimulationConfigGroup.GROUP_NAME ) ;
			}
			if ( config.getModule("JDEQSim")!=null ) {
				log.warn("have both `simulation' and `JDEQSim' config groups; presumably both are defined in" +
						" the config file; removing the `simulation' config group; in future versions, this" +
						" may become a fatal error") ;
				config.removeModule( SimulationConfigGroup.GROUP_NAME ) ;
			}
		}
		if ( config.getQSimConfigGroup()!=null && config.getModule("JDEQSim")!=null ) {
			log.warn("have both `qsim' and `JDEQSim' config groups; presumably both are defined in" +
					" the config file; removing the `qsim' config group; in future versions, this" +
					" may become a fatal error") ;
			config.removeModule( QSimConfigGroup.GROUP_NAME ) ;
		}
	}

	private void checkForConfigModulesOfUnselectedMobsims(final Config config,
			MobsimType mType) {
		if ( config.getModule( mType.toString()) != null ) {
			throw new RuntimeException("you have NO config module defined for the mobsim that you have selected " +
					"BUT you have a config module defined for a mobsim that you have NOT selected; aborting ...") ;
//			log.warn("you have a config module defined for a mobsim that you have not selected;" +
//		" removing the config module; this may eventually become a fatal error.") ;
//		config.removeModule( mType.toString() ) ;
		}
	}

	/*package*/ void checkPlanCalcScore(final Config c) {
		if (c.planCalcScore().getTravelingPt_utils_hr() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingPt is > 0. This values specifies a utility. " +
					"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getTraveling_utils_hr() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".traveling is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getTravelingBike_utils_hr() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingBike is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getTravelingWalk_utils_hr() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingWalk is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
	}

	private void checkMultimodalMobsim(final Config c) {
		if ("multimodalQSim".equals(c.controler().getMobsim()) && (!c.multiModal().isMultiModalSimulationEnabled())) {
			log.error("A multimodal mobsim should be used according to controler.mobsim, but the multimodal-simulation feature is not enabled in multimodal.multiModalSimulationEnabled.");
		}
		if (c.multiModal().isMultiModalSimulationEnabled() && (c.controler().getMobsim() != null)) {
			if (!"multimodalQSim".equals(c.controler().getMobsim())) {
				log.error("multimodal-simulation is activated in the multimodal configuration, but no multimodal-supporting mobsim is definied in the controler configuration.");
			}
		}
	}

	private void checkEventsFormatLanesSignals(final Config c) {
		ScenarioConfigGroup scg = c.scenario();
		if (scg.isUseLanes() || scg.isUseSignalSystems()) {
			if (!c.controler().getEventsFileFormats().contains(EventsFileFormat.xml)){
				log.error("Xml events are not enabled, but lanes and eventually signal systems" +
						"are enalbed. Events from this features will only be written to the xml format, consider" +
						"to add xml events in the controler config module");
			}
		}
	}

	private void checkScenarioFeaturesEnabled(final Config c) {
		ScenarioConfigGroup scg = c.scenario();
		if (scg.isUseSignalSystems() && ! ("qsim".equals(c.controler().getMobsim()) ||  c.getQSimConfigGroup() != null)){
		  log.warn("The signal system implementation is only supported by the org.matsim.ptproject.qsim mobility simulation that is not activated. Please make sure you are using the correct" +
		  		"mobility simulation. This warning can be ingored if a customized mobility simulation developed outside of org.matsim is used and set correctly.");
		}
	}


	private void checkTravelTimeCalculationRoutingConfiguration(final Config config){
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


	private void checkLaneDefinitionRoutingConfiguration(final Config config) {
		if ((config.scenario().isUseLanes()) &&
		    !config.controler().isLinkToLinkRoutingEnabled()){
		  	log.warn("Using lanes without enabling linktolinkrouting might not lead to expected simulation results");
		   }
	}


	private void checkTransitReplanningConfiguration(final Config config) {
		if (config.scenario().isUseTransit()) {
			for (StrategySettings settings : config.strategy().getStrategySettings()) {
				if ("TimeAllocationMutator".equals(settings.getModuleName())) {
					log.error("The strategy 'TimeAllocationMutator' should be replaced with 'TransitTimeAllocationMutator' when transit is enabled!");
				} else if ("ChangeLegMode".equals(settings.getModuleName())) {
					log.error("The strategy 'ChangeLegMode' should be replaced with 'TransitChangeLegMode' when transit is enabled!");
				}
			}
		}
	}

}
