/* *********************************************************************** *
 * project: matsim
 * VspConfigConsistencyCheckerImpl.java
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

package org.matsim.core.config.consistency;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;

/**
 * @author nagel
 *
 */
public class VspConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {
	private static Logger log = Logger.getLogger(VspConfigConsistencyCheckerImpl.class) ;
	
	public VspConfigConsistencyCheckerImpl() {
		// empty.  only here to find out where it is called.
	}

	@Override
	public void checkConsistency(Config config) {
		if ( config.vspExperimental().getVspDefaultsCheckingLevel().equals( VspExperimentalConfigGroup.IGNORE ) ) {
			log.info( "NOT running vsp config consistency check because vsp defaults checking level is set to IGNORE"); 
			return ;
		}
		log.info("running checkConsistency ...");
		
		boolean problem = false ; // ini
		
		// added apr'15:
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			switch( params.getTypicalDurationScoreComputation() ) {
			case relative:
				break;
			case uniform:
//				problem = true ;
				log.warn( "found `typicalDurationScoreComputation == uniform'; vsp should try out `relative'. ") ;
				break;
			default:
				throw new RuntimeException("unexpected setting; aborting ... ") ;
			}
		}

		if ( config.planCalcScore().getMonetaryDistanceCostRateCar() > 0 ) {
			problem = true ;
			System.out.flush() ;
			log.error("found monetary distance cost rate car > 0.  You probably want a value < 0 here.  " +
					"This is a bug and may be changed eventually.  kai, jun'11") ;
		}
		if ( config.planCalcScore().getMonetaryDistanceCostRatePt() > 0 ) {
			problem = true ;
			System.out.flush() ;
			log.error("found monetary distance cost rate pt > 0.  You probably want a value < 0 here.  " +
					"This is a bug and may be changed eventually.  kai, jun'11") ;
		}
		if ( config.planCalcScore().getMarginalUtilityOfMoney() < 0. ) {
			problem = true ;
			System.out.flush() ;
			log.error("found marginal utility of money < 0.  You almost certainly want a value > 0 here. " ) ;
		}
		// added aug'13:
		if ( config.planCalcScore().getMarginalUtlOfWaiting_utils_hr() != 0. ) {
			problem = true ;
			System.out.flush() ;
			log.error("found marginal utility of waiting != 0.  vsp default is setting this to 0. " ) ;
		}
		
		if ( config.qsim()!=null && config.qsim().isRemoveStuckVehicles() ) {
			problem = true ;
			System.out.flush() ;
			log.warn("found that the qsim is removing stuck vehicles.  vsp default is setting this to false.");
		}
		
		boolean found = false ;
		Collection<StrategySettings> settingsColl = config.strategy().getStrategySettings();
		for ( StrategySettings settings : settingsColl ) {
			if ( settings.getStrategyName().equalsIgnoreCase("ChangeExpBeta") ) {
				found = true ;
			}
		}
		if ( !found ) {
			problem = true ;
			System.out.flush() ;
			log.warn("You have no strategy configured that uses ChangeExpBeta. vsp default is to use ChangeExpBeta at least in one strategy.");
		}
		
		Set<EventsFileFormat> formats = config.controler().getEventsFileFormats();
		if ( !formats.contains(EventsFileFormat.xml) ) {
			problem = true ;
			System.out.flush() ;
			log.warn("did not find xml as one of the events file formats. vsp default is using xml events.");
		}
		
		// added before nov'12
		if ( config.timeAllocationMutator().getMutationRange() < 7200 ) {
			problem = true ;
			System.out.flush() ;
			log.warn("timeAllocationMutator mutationRange < 7200; vsp default is 7200.  This means you have to add the following lines to your config file: ") ;
			log.warn("<module name=\"TimeAllocationMutator\">");
			log.warn("	<param name=\"mutationRange\" value=\"7200.0\" />");
			log.warn("</module>");
		}
		// added jan'14
		if ( config.timeAllocationMutator().isAffectingDuration() ) {
//			problem = true ;
			System.out.flush() ;
			log.warn("timeAllocationMutator is affecting duration; vsp default is to not do that.  This will be more strictly" +
					" enforced in the future. This means you have to add the following lines to your config file: ") ;
			log.warn("<module name=\"TimeAllocationMutator\">");
			log.warn("	<param name=\"affectingDuration\" value=\"false\" />");
			log.warn("</module>");
		}
		
		// added before nov'12
		if ( !config.vspExperimental().isRemovingUnneccessaryPlanAttributes() ) {
			problem = true ;
			System.out.flush() ;
			log.warn("You are not removing unnecessary plan attributes; vsp default is to do that.") ;
		}
		
		ActivityDurationInterpretation actDurInterpr =  config.plans().getActivityDurationInterpretation()  ;
		if ( actDurInterpr == ActivityDurationInterpretation.endTimeOnly ) {
			// added jan'13
			log.warn(ActivityDurationInterpretation.endTimeOnly + " is deprecated. Use " + ActivityDurationInterpretation.tryEndTimeThenDuration + " instead.") ;
			problem = true;
			// added before nov'12
			if( config.scenario().isUseTransit()) {
				problem = true;
				System.out.flush() ;
				log.error("You are using " + config.plans().getActivityDurationInterpretation() + " as activityDurationInterpretation in " +
						"conjunction with the matsim transit module. This is not working at all as pt interaction activities never have an end time and " +
				"thus will never end!");
			}
		}
		
		// added jan'13
		if ( actDurInterpr == ActivityDurationInterpretation.minOfDurationAndEndTime ) {
			problem = true ;
			System.out.flush() ;
			log.warn("You are using ActivityDurationInterpretation " + config.plans().getActivityDurationInterpretation() + " ; vsp default is to use " +
					ActivityDurationInterpretation.tryEndTimeThenDuration + 
							"This means you have to add the following lines into the vspExperimental section of your config file: ") ;
			log.warn( "   <param name=\"activityDurationInterpretation\" value=\"" + ActivityDurationInterpretation.tryEndTimeThenDuration + "\" />" ) ;
			log.warn("Please report if this causes odd results (this will simplify many code maintenance issues, but is unfortunately not well tested).") ;
		}
		
		// pseudo-pt über Distanz, nicht ptSpeedFactor
		// todo
		
		// use beta_brain=1 // added as of nov'12
		if ( config.planCalcScore().getBrainExpBeta() != 1. ) {
			problem = true ;
			System.out.flush() ;
			log.warn("You are using a brainExpBeta != 1; vsp default is 1.  (Different values may cause conceptual " +
					"problems during paper writing.) This means you have to add the following lines to your config file: ") ;
			log.warn("<module name=\"planCalcScore\">");
			log.warn("	<param name=\"BrainExpBeta\" value=\"1.0\" />");
			log.warn("</module>");
		}
		
		boolean usingLocationChoice = false ;
		if ( config.getModule("locationchoice")!=null ) {
			usingLocationChoice = true ;
		}
		
		if ( usingLocationChoice ) {
			final String samplePercent = config.findParam("locationchoice", "destinationSamplePercent");
			if ( samplePercent!=null && !samplePercent.equals("100.") ) {
				problem = true ;
				System.out.flush() ;
				log.error("vsp will not accept location choice destination sample percent other than 100 until the corresponding warning in " +
				"DestinationSampler is resolved.  kai, jan'13") ;
			}
//			if ( !config.locationchoice().getProbChoiceExponent().equals("1.") ) {
//				//			problem = true ;
//				log.error("vsp will not accept location choice prob choice exponents other than 1 until the corresponding warning in " +
//				"ChoiceSet is resolved.  kai, jan'13") ;
//			}
			if ( !config.vspExperimental().isUsingOpportunityCostOfTimeForLocationChoice() ) {
				problem = true ;
				System.out.flush() ;
				log.error("vsp will not accept location choice without including opportunity cost of time into the approximation. kai,jan'13") ;
			}
		}
		
		if ( problem && config.vspExperimental().getVspDefaultsCheckingLevel().equals( VspExperimentalConfigGroup.ABORT ) ) {
			String str = "found a situation that leads to vsp-abort.  aborting ..." ; 
			System.out.flush() ;
			log.fatal( str ) ; 
			throw new RuntimeException( str ) ;
		}
		
	}

}
