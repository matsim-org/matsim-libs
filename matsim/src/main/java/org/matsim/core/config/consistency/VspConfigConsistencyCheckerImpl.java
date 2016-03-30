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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.pt.PtConstants;

/**
 * @author nagel
 *
 */
public final class VspConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {
	private static Logger log = Logger.getLogger(VspConfigConsistencyCheckerImpl.class) ;
	
	public VspConfigConsistencyCheckerImpl() {
		// empty.  only here to find out where it is called.
	}

	@Override
	public void checkConsistency(Config config) {
		Level lvl ;
		
		switch ( config.vspExperimental().getVspDefaultsCheckingLevel() ) {
		case ignore:
			log.info( "NOT running vsp config consistency check because vsp defaults checking level is set to IGNORE"); 
			return ;
		case info:
			lvl = Level.INFO ;
			break ;
		case warn:
			lvl = Level.WARN ;
			break;
		case abort:
			lvl = Level.WARN ;
			break;
		default:
			throw new RuntimeException("not implemented");
		}
		log.info("running checkConsistency ...");
		
		boolean problem = false ; // ini
		
		// added feb'16
		if ( !config.qsim().isUsingTravelTimeCheckInTeleportation() ) {
			log.log( lvl, "found `qsim.usingTravelTimeCheckInTeleporation==false'; vsp should try out `true' and report." ) ;
		}
		
		// added feb'16
		if ( !config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			log.log( lvl, "found `plansCalcRoute.insertingAccessEgressWalk==false'; vsp should try out `true' and report. " ) ;
		}
		
		// added apr'15
		if ( !config.qsim().isUsingFastCapacityUpdate() ) {
			log.log( lvl,  " found 'qsim.usingFastCapacityUpdate==false'; vsp should try out `true' and report. ") ;
		}
		switch( config.qsim().getTrafficDynamics() ) {
		case withHoles:
			break;
		default:
			log.log( lvl,  " found 'qsim.trafficDynamics==" + config.qsim().getTrafficDynamics() + "'; vsp standard is`" 
					+ TrafficDynamics.withHoles + "'." ) ;
			break;
		}
		
		// added apr'15:
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			if ( PtConstants.TRANSIT_ACTIVITY_TYPE.equals( params.getActivityType() ) ) {
				// they have typicalDurationScoreComputation==relative, but are not scored anyways. benjamin/kai, nov'15
				continue ;
			}
			switch( params.getTypicalDurationScoreComputation() ) {
			case relative:
				break;
			case uniform:
//				problem = true ;
				log.log( lvl,  "found `typicalDurationScoreComputation == uniform' for activity type " + params.getActivityType() + "; vsp should try out `relative' and report. ") ;
				break;
			default:
				throw new RuntimeException("unexpected setting; aborting ... ") ;
			}
		}

		if ( config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() > 0 ) {
			problem = true ;
			System.out.flush() ;
			log.error("found monetary distance cost rate car > 0.  You probably want a value < 0 here.  " +
					"This is a bug and may be changed eventually.  kai, jun'11") ;
		}
		if ( config.planCalcScore().getModes().get(TransportMode.pt).getMonetaryDistanceRate() > 0 ) {
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
			log.log( lvl, "found that the qsim is removing stuck vehicles.  vsp default is setting this to false.");
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
			log.log( lvl, "You have no strategy configured that uses ChangeExpBeta. vsp default is to use ChangeExpBeta at least in one strategy.");
		}
		
		Set<EventsFileFormat> formats = config.controler().getEventsFileFormats();
		if ( !formats.contains(EventsFileFormat.xml) ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "did not find xml as one of the events file formats. vsp default is using xml events.");
		}
		
		// added nov'15
		boolean usingTimeMutator = false ;
		for ( StrategySettings it : config.strategy().getStrategySettings() ) {
			if ( DefaultStrategy.TimeAllocationMutator.name().equals( it.getName() ) ) {
				usingTimeMutator = true ;
				break ;
			}
		}
		if ( usingTimeMutator ) {
			// added before nov'12
			if ( config.timeAllocationMutator().getMutationRange() < 7200 ) {
				problem = true ;
				System.out.flush() ;
				log.log( lvl, "timeAllocationMutator mutationRange < 7200; vsp default is 7200.  This means you have to add the following lines to your config file: ") ;
				log.log( lvl, "<module name=\"TimeAllocationMutator\">");
				log.log( lvl, "	<param name=\"mutationRange\" value=\"7200.0\" />");
				log.log( lvl, "</module>");
			}
			// added jan'14
			if ( config.timeAllocationMutator().isAffectingDuration() ) {
				//			problem = true ;
				System.out.flush() ;
				log.log( lvl, "timeAllocationMutator is affecting duration; vsp default is to not do that.  This will be more strictly" +
						" enforced in the future. This means you have to add the following lines to your config file: ") ;
				log.log( lvl, "<module name=\"TimeAllocationMutator\">");
				log.log( lvl, "	<param name=\"affectingDuration\" value=\"false\" />");
				log.log( lvl, "</module>");
			}
		}
		
		// added before nov'12
		if ( !config.plans().isRemovingUnneccessaryPlanAttributes() ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are not removing unnecessary plan attributes; vsp default is to do that.") ;
		}
		
		PlansConfigGroup.ActivityDurationInterpretation actDurInterpr =  config.plans().getActivityDurationInterpretation()  ;
		if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly ) {
			// added jan'13
			log.log( lvl, PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly + " is deprecated. Use " + PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration + " instead.") ;
			problem = true;
			// added before nov'12
			if( config.transit().isUseTransit()) {
				problem = true;
				System.out.flush() ;
				log.error("You are using " + config.plans().getActivityDurationInterpretation() + " as activityDurationInterpretation in " +
						"conjunction with the matsim transit module. This is not working at all as pt interaction activities never have an end time and " +
				"thus will never end!");
			}
		}
		
		// added jan'13
		if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are using ActivityDurationInterpretation " + config.plans().getActivityDurationInterpretation() + " ; vsp default is to use " +
					PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration + 
							"This means you have to add the following lines into the vspExperimental section of your config file: ") ;
			log.log( lvl,  "   <param name=\"activityDurationInterpretation\" value=\"" + PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration + "\" />" ) ;
			log.log( lvl, "Please report if this causes odd results (this will simplify many code maintenance issues, but is unfortunately not well tested).") ;
		}
		
		// pseudo-pt über Distanz, nicht ptSpeedFactor
		// todo
		
		// use beta_brain=1 // added as of nov'12
		if ( config.planCalcScore().getBrainExpBeta() != 1. ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are using a brainExpBeta != 1; vsp default is 1.  (Different values may cause conceptual " +
					"problems during paper writing.) This means you have to add the following lines to your config file: ") ;
			log.log( lvl, "<module name=\"planCalcScore\">");
			log.log( lvl, "	<param name=\"BrainExpBeta\" value=\"1.0\" />");
			log.log( lvl, "</module>");
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
		
		if ( problem && config.vspExperimental().getVspDefaultsCheckingLevel() == VspDefaultsCheckingLevel.abort ) {
			String str = "found a situation that leads to vsp-abort.  aborting ..." ; 
			System.out.flush() ;
			log.fatal( str ) ; 
			throw new RuntimeException( str ) ;
		}
		
	}

}
