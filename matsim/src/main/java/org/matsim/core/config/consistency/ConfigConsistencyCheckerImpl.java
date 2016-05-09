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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * Implementation of the ConfigCosistencyChecker interface.
 *
 * @author dgrether
 */
public final class ConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {

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
		this.checkPlanCalcScore(config);
		this.checkTransit(config);
	}

	/*package*/ void checkPlanCalcScore(final Config c) {
		if (c.planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingPt is > 0. This values specifies a utility. " +
					"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".traveling is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingBike is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
		if (c.planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() > 0) {
			log.warn(PlanCalcScoreConfigGroup.GROUP_NAME + ".travelingWalk is > 0. This values specifies a utility. " +
			"Typically, this should be a disutility, i.e. have a negative value.");
		}
		ActivityParams ptAct = c.planCalcScore().getActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE) ;
		if ( ptAct != null ) {
//			if ( ptAct.getClosingTime()!=0. && ptAct.getClosingTime()!=Time.UNDEFINED_TIME ) {
//				if ( !c.vspExperimental().isAbleToOverwritePtInteractionParams()==true ) {
//					throw new RuntimeException("setting the pt interaction activity closing time away from 0/undefined is not allowed because it breaks pt scoring." +
//					" If you need this anyway (for backwards compatibility reasons), you can allow this by a parameter in VspExperimentalConfigGroup.") ;
//				}
//			}
			if ( ptAct.isScoringThisActivityAtAll() ) {
				if ( !c.vspExperimental().isAbleToOverwritePtInteractionParams()==true ) {
					throw new RuntimeException("Scoring " + ptAct.getActivityType() + " is not allowed because it breaks pt scoring." +
					" If you need this anyway (for backwards compatibility reasons), you can allow this by a parameter in VspExperimentalConfigGroup.") ;
				}
			}
		}
	}

	private void checkEventsFormatLanesSignals(final Config c) {
		if (c.qsim().isUseLanes()) {
			if (!c.controler().getEventsFileFormats().contains(EventsFileFormat.xml)){
				log.error("Xml events are not enabled, but lanes and possibly signal systems" +
						"are enalbed. Events from this features will only be written to the xml format, consider" +
						"to add xml events in the controler config module");
			}
		}
	}

	private void checkScenarioFeaturesEnabled(final Config c) {
		ScenarioConfigGroup scg = c.scenario();
		if (! ("qsim".equals(c.controler().getMobsim()) ||  c.qsim() != null)){
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
				config.qsim().isRemoveStuckVehicles()){
			throw new IllegalStateException("Link to link travel time calculation is not" +
					"available if using the remove stuck vehicles option!");
		}


	}


	private void checkLaneDefinitionRoutingConfiguration(final Config config) {
		if ((config.qsim().isUseLanes()) &&
		    !config.controler().isLinkToLinkRoutingEnabled()){
		  	log.warn("Using lanes without enabling linktolinkrouting might not lead to expected simulation results");
		}
	}

	private void checkTransit(final Config config) {
		if ( config.transit().isUseTransit() && config.transit().getVehiclesFile()==null ) {
			log.warn("Your are using Transit but have not provided a transit vehicles file. This most likely won't work.");
		}
	}

}
