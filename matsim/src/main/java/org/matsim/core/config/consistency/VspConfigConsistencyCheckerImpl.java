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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.CheckingOfMarginalUtilityOfTravellng;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.pt.PtConstants;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author nagel
 *
 */
public final class VspConfigConsistencyCheckerImpl implements ConfigConsistencyChecker {

	private static final  Logger log = LogManager.getLogger(VspConfigConsistencyCheckerImpl.class);

	public VspConfigConsistencyCheckerImpl() {
		// empty.  only here to find out where it is called.
	}

	@Override
	public void checkConsistency(Config config) {
		Level lvl ;

		switch( config.vspExperimental().getVspDefaultsCheckingLevel() ){
			case ignore -> {
				log.info( "NOT running vsp config consistency check because vsp defaults checking level is set to IGNORE" );
				return;
			}
			case info -> lvl = Level.INFO;
			case warn -> lvl = Level.WARN;
			case abort -> lvl = Level.WARN;
			default -> throw new RuntimeException( "not implemented" );
		}
		log.info("running checkConsistency ...");

		boolean problem = false ; // ini

		// yy: sort the config groups alphabetically

		// === controler:

		problem = checkControlerConfigGroup( config, lvl, problem );

		// === facilities:

		//noinspection ReassignedVariable
		problem = checkFacilitiesConfigGroup( config, lvl, problem );

		// === global:

		problem = checkGlobalConfigGroup( config, lvl, problem );

		// === location choice:

		problem = checkLocationChoiceConfigGroup( config, problem );

		// === planCalcScore:

		problem = checkPlanCalcScoreConfigGroup( config, lvl, problem );

		// === plans:

		problem = checkPlansConfigGroup( config, lvl, problem );

		// === plansCalcRoute:

		checkPlansCalcScoreConfigGroup( config, lvl );

		// === qsim:

		problem = checkQsimConfigGroup( config, lvl, problem );

		// === strategy:

		problem = checkStrategyConfigGroup( config, lvl, problem );

		// === travelTimeCalculator:

		checkTravelTimeCalculatorConfigGroup( config, lvl );

		// === interaction between config groups:
		boolean containsModeChoice = false ;
		for ( StrategySettings settings : config.replanning().getStrategySettings() ) {
			if ( settings.getStrategyName().contains("Mode") ) {
				containsModeChoice = true ;
			}
		}

		// added jun'16
		if ( config.qsim().getVehiclesSource()==VehiclesSource.fromVehiclesData
				&& config.qsim().getUsePersonIdForMissingVehicleId()
				&& containsModeChoice
				&& config.qsim().getMainModes().size() > 1 )
		{
			problem = true ;
			log.log( lvl, "You can't use more than one main (=vehicular) mode while using the agent ID as missing vehicle ID ... "
					+ "because in this case the person can only have one vehicle and thus cannot switch to a different vehicle type." ) ;
		}

		// === zzz:

		if ( problem && config.vspExperimental().getVspDefaultsCheckingLevel() == VspDefaultsCheckingLevel.abort ) {
			String str = "found a situation that leads to vsp-abort.  aborting ..." ;
			System.out.flush() ;
			log.fatal( str ) ;
			throw new RuntimeException( str ) ;
		}

	}
//	private boolean checkSubtourModeChoiceConfigGroup( Config config, Level lvl, boolean problem ){
//		if ( config.subtourModeChoice().considerCarAvailability() ) {
////			problem = true;
//			log.log( lvl, "you are considering car abailability; vsp config is not doing that.   Instead, we are using a daily monetary constant for car.");
//		}
//		return problem;
//	}
//	private boolean checkModeChoiceConfigGroup( Config config, Level lvl, boolean problem ){
//		if ( !config.changeMode().getIgnoreCarAvailability() ) {
////			problem = true;
//			log.log( lvl, "you are considering car abailability; vsp config is not doing that.   Instead, we are using a daily monetary constant for car.");
//		}
//		return problem;
//	}
	private static boolean checkGlobalConfigGroup( Config config, Level lvl, boolean problem ){
		if ( config.global().isInsistingOnDeprecatedConfigVersion() ) {
			problem = true ;
			System.out.flush();
			log.log( lvl, "you are insisting on config v1.  vsp default is using v2." ) ;
		}
		return problem;
	}
	private static void checkTravelTimeCalculatorConfigGroup( Config config, Level lvl ){
		// added feb'19
		if ( !config.travelTimeCalculator().getSeparateModes() ) {
			System.out.flush() ;
			log.log( lvl, "travelTimeCalculator is not analyzing different modes separately; vsp default is to do that.  Otherwise, you are using the same travel times " +
						    "for, say, bike and car.") ;
		}
	}
	private static boolean checkStrategyConfigGroup( Config config, Level lvl, boolean problem ){
		boolean found = false ;
		Collection<StrategySettings> settingsColl = config.replanning().getStrategySettings();
		for ( StrategySettings settings : settingsColl ) {
			if ( settings.getStrategyName().equalsIgnoreCase("ChangeExpBeta") ) {
				found = true ;
			}
		}
		if ( !found ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You have no strategy configured that uses ChangeExpBeta. vsp default is to use ChangeExpBeta at least in one strategy." );
		}

		// added may'16
		if ( config.replanning().getFractionOfIterationsToDisableInnovation()==Double.POSITIVE_INFINITY ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You have not set fractionOfIterationsToDisableInnovation; vsp default is to set this to 0.8 or similar.  Add the following config lines:" ) ;
			log.log( lvl, "<module name=\"strategy\">" );
			log.log( lvl, "	<param name=\"fractionOfIterationsToDisableInnovation\" value=\"0.8\" />" );
			log.log( lvl, "</module>" );
		}

		// added nov'15
		boolean usingTimeMutator = false ;
		for ( StrategySettings it : config.replanning().getStrategySettings() ) {
			if ( DefaultStrategy.TimeAllocationMutator.equals( it.getName() ) ) {
				usingTimeMutator = true ;
				break ;
			}
		}
		if ( usingTimeMutator ) {
			// added before nov'12
			if ( config.timeAllocationMutator().getMutationRange() < 7200 ) {
				problem = true ;
				System.out.flush() ;
				log.log( lvl, "timeAllocationMutator mutationRange < 7200; vsp default is 7200.  This means you have to add the following lines to your config file: " ) ;
				log.log( lvl, "<module name=\"TimeAllocationMutator\">" );
				log.log( lvl, "	<param name=\"mutationRange\" value=\"7200.0\" />" );
				log.log( lvl, "</module>" );
			}
			// added jan'14
			if ( config.timeAllocationMutator().isAffectingDuration() ) {
				//			problem = true ;
				System.out.flush() ;
				log.log( lvl, "timeAllocationMutator is affecting duration; vsp default is to not do that.  This will be more strictly" +
						" enforced in the future. This means you have to add the following lines to your config file: ") ;
				log.log( lvl, "<module name=\"TimeAllocationMutator\">" );
				log.log( lvl, "	<param name=\"affectingDuration\" value=\"false\" />" );
				log.log( lvl, "</module>" );
			}
		}

		// added jun'22
		boolean usingSMC = false ;
		for ( StrategySettings it : config.replanning().getStrategySettings() ) {
			if ( DefaultStrategy.SubtourModeChoice.equals( it.getName() ) ) {
				usingSMC = true ;
				break ;
			}
		}
		if (usingSMC) {
			if ( config.subtourModeChoice().getProbaForRandomSingleTripMode() < 0.2) {
				problem = true;
				System.out.flush();
				log.log( lvl, "SubTourModeChoice 'probaForRandomSingleTripMode' is very small and below 0.2. Recommendation is, to set this to a value around 0.5." );
			}
		}
		return problem;
	}
	private static boolean checkQsimConfigGroup( Config config, Level lvl, boolean problem ){
		// jun'23
		if ( config.qsim().getVehiclesSource()==VehiclesSource.defaultVehicle ) {
			log.log( lvl, "found qsim.vehiclesSource=defaultVehicle; vsp should use one of the other settings or talk to kai");
		}
		if ( config.qsim().getLinkDynamics() != QSimConfigGroup.LinkDynamics.PassingQ && config.qsim().getMainModes().contains(TransportMode.bike) ) {
			log.log( lvl, "found qsim.linkDynamics=" + config.qsim().getLinkDynamics() + "; vsp should use PassingQ or talk to kai");
		}

		// added jun'16
		if ( config.qsim().getUsePersonIdForMissingVehicleId() ) {
			log.log( lvl, "found qsim.usePersonIdForMissingVehicleId==true; vsp should set this to false or talk to kai" ) ;
		}

		// added feb'16
		if ( !config.qsim().isUsingTravelTimeCheckInTeleportation() ) {
			log.log( lvl, "found `qsim.usingTravelTimeCheckInTeleporation==false'; vsp should try out `true' and report." ) ;
		}

		// added apr'15
//		if ( !config.qsim().isUsingFastCapacityUpdate() ) {
//			log.log( lvl,  " found 'qsim.usingFastCapacityUpdate==false'; vsp should try out `true' and report. ") ;
//		}
		switch( config.qsim().getTrafficDynamics() ) {
			case kinematicWaves:
				break;
			case withHoles:
			case queue:
			default:
				log.log( lvl,  " found 'qsim.trafficDynamics==" + config.qsim().getTrafficDynamics() + "'; vsp standard is`"
							     + TrafficDynamics.kinematicWaves + "'." ) ;
				break;
		}

		if ( config.qsim()!=null && config.qsim().isRemoveStuckVehicles() ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "found that the qsim is removing stuck vehicles.  vsp default is setting this to false." );
		}

		return problem;
	}
	private static void checkPlansCalcScoreConfigGroup( Config config, Level lvl ){
	}
	private static boolean checkPlansConfigGroup( Config config, Level lvl, boolean problem ){
		// added before nov'12
		if ( !config.plans().isRemovingUnneccessaryPlanAttributes() ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are not removing unnecessary plan attributes; vsp default is to do that." ) ;
		}

		PlansConfigGroup.ActivityDurationInterpretation actDurInterpr =  config.plans().getActivityDurationInterpretation()  ;
		if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly ) {
			// added jan'13
			log.log( lvl, PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly + " is deprecated. Use " + PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration + " instead." ) ;
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
		if ( actDurInterpr != PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are using ActivityDurationInterpretation " + config.plans().getActivityDurationInterpretation() + " ; vsp default is to use " +
					PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration +
					"This means you have to add the following lines into the vspExperimental section of your config file: ") ;
			log.log( lvl,  "   <param name=\"activityDurationInterpretation\" value=\"" + PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration + "\" />" ) ;
			log.log( lvl, "Please report if this causes odd results (this will simplify many code maintenance issues, but is unfortunately not well tested)." ) ;
		}
		return problem;
	}
	private static boolean checkPlanCalcScoreConfigGroup( Config config, Level lvl, boolean problem ){
		// use beta_brain=1 // added as of nov'12
		if ( config.scoring().getBrainExpBeta() != 1. ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are using a brainExpBeta != 1; vsp default is 1.  (Different values may cause conceptual " +
					"problems during paper writing.) This means you have to add the following lines to your config file: ") ;
			log.log( lvl, "<module name=\"planCalcScore\">" );
			log.log( lvl, "	<param name=\"BrainExpBeta\" value=\"1.0\" />" );
			log.log( lvl, "</module>" );
		}

		// added aug'13:
		if ( config.scoring().getMarginalUtlOfWaiting_utils_hr() != 0. ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "found marginal utility of waiting != 0.  vsp default is setting this to 0. " ) ;
		}

		// added apr'15:
		for ( ActivityParams params : config.scoring().getActivityParams() ) {
			if ( PtConstants.TRANSIT_ACTIVITY_TYPE.equals( params.getActivityType() ) ) {
				// they have typicalDurationScoreComputation==relative, but are not scored anyways. benjamin/kai, nov'15
				continue ;
			}
			switch( params.getTypicalDurationScoreComputation() ) {
			case relative:
				break;
			case uniform:
//				problem = true ;
				log.log( lvl,  "found `typicalDurationScoreComputation == uniform' for activity type " + params.getActivityType() + "; vsp should use `relative'. " ) ;
				break;
			default:
				throw new RuntimeException("unexpected setting; aborting ... ") ;
			}
		}
		for ( ModeParams params : config.scoring().getModes().values() ) {
			if ( params.getMonetaryDistanceRate() > 0. ) {
				problem = true ;
				System.out.flush() ;
				log.error("found monetary distance rate for mode " + params.getMode() + " > 0.  You probably want a value < 0 here.\n" ) ;
			}
			if ( params.getMonetaryDistanceRate() < -0.01 ) {
				System.out.flush() ;
				log.error("found monetary distance rate for mode " + params.getMode() + " < -0.01.  -0.01 per meter means -10 per km.  You probably want to divide your value by 1000." ) ;
			}
		}

		if ( config.scoring().getModes().get(TransportMode.car ) != null && config.scoring().getModes().get(TransportMode.car ).getMonetaryDistanceRate() > 0 ) {
			problem = true ;
		}
		final ModeParams modeParamsPt = config.scoring().getModes().get(TransportMode.pt );
		if ( modeParamsPt!=null && modeParamsPt.getMonetaryDistanceRate() > 0 ) {
			problem = true ;
			System.out.flush() ;
			log.error("found monetary distance rate pt > 0.  You probably want a value < 0 here." ) ;
		}
		if ( config.scoring().getMarginalUtilityOfMoney() < 0. ) {
			problem = true ;
			System.out.flush() ;
			log.error("found marginal utility of money < 0.  You almost certainly want a value > 0 here. " ) ;
		}

		// added feb'16
		if ( config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none ) ) {
			log.log( lvl, "found `PlansCalcRouteConfigGroup.AccessEgressType.none'; vsp should use `accessEgressModeToLink' or " +
						      "some other value or talk to Kai." ) ;
		}
		// added oct'17:
		if ( config.scoring().getFractionOfIterationsToStartScoreMSA() == null || config.scoring().getFractionOfIterationsToStartScoreMSA() >= 1. ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "You are not setting fractionOfIterationsToStartScoreMSA; vsp default is to set this to something like 0.8.  " +
					"This means you have to add the following lines to your config file: ") ;
			log.log( lvl, "<module name=\"planCalcScore\">" );
			log.log( lvl, "	<param name=\"fractionOfIterationsToStartScoreMSA\" value=\"0.8\" />" );
			log.log( lvl, "</module>" );
		}

		// added apr'21:
		for( Map.Entry<String, ScoringConfigGroup.ScoringParameterSet> entry : config.scoring().getScoringParametersPerSubpopulation().entrySet() ){
			for( ActivityParams activityParam : entry.getValue().getActivityParams() ){
				if( activityParam.getMinimalDuration().isDefined() ){
					log.log( lvl, "Vsp default is to not define minimal duration.  Activity type=" + activityParam.getActivityType() + "; subpopulation=" + entry.getKey() );
				}
			}
		}

		// added may'23
		for ( ModeParams params : config.scoring().getModes().values() ){
			if ( config.vspExperimental().getCheckingOfMarginalUtilityOfTravellng()== CheckingOfMarginalUtilityOfTravellng.allZero ){
				if( params.getMarginalUtilityOfTraveling() != 0. && !params.getMode().equals( TransportMode.ride ) && !params.getMode().equals( TransportMode.bike ) ){
					log.log( lvl, "You are setting the marginal utility of traveling with mode " + params.getMode() + " to " + params.getMarginalUtilityOfTraveling()
								      + ". VSP standard is to set this to zero.  Please document carefully why you are using a value different from zero, e.g. by showing distance distributions." );
				}
			}
			if ( params.getMode().equals( TransportMode.walk ) && params.getConstant() != 0. ) {
				problem = true;
				log.log( lvl, "You are setting the alternative-specific constant for the walk mode to " + params.getConstant()
							      + ".  Values different from zero cause problems here because the ASC is also used for access/egress modes" );
			}
		}
		return problem;
	}
	private static boolean checkLocationChoiceConfigGroup( Config config, boolean problem ){
		boolean usingLocationChoice = false ;
		if ( config.getModule("locationchoice" )!=null ) {
			usingLocationChoice = true ;
		}

		if ( usingLocationChoice ) {
			final String samplePercent = config.findParam("locationchoice", "destinationSamplePercent" );
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
		return problem;
	}
	private static boolean checkFacilitiesConfigGroup( Config config, Level lvl, boolean problem ){
		// 2023-05:
		if ( config.facilities().getFacilitiesSource()== FacilitiesConfigGroup.FacilitiesSource.none ) {
//			problem = true;
			System.out.flush();
			log.log( lvl, "vsp should move away from facilitiesSource=FacilitiesSource.none" );
		}
		return problem;
	}
	private static boolean checkControlerConfigGroup( Config config, Level lvl, boolean problem ){
		Set<EventsFileFormat> formats = config.controller().getEventsFileFormats();
		if ( !formats.contains( EventsFileFormat.xml ) ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "did not find xml as one of the events file formats. vsp default is using xml events.");
		}

		// may'21
		switch ( config.controller().getRoutingAlgorithmType() ) {
			case Dijkstra:
			case AStarLandmarks:
				log.log( lvl, "you are not using SpeedyALT as routing algorithm.  vsp default (since may'21) is to use SpeedeALT.") ;
				System.out.flush();
				break;
			case SpeedyALT:
				break;
		}

		if ( config.controller().getWritePlansInterval() <= 0 ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "found writePlansInterval==0.  vsp default is to write plans at least once (for simwrapper).") ;
		}

		if ( config.controller().getWriteTripsInterval() <= 0 ) {
			problem = true ;
			System.out.flush() ;
			log.log( lvl, "found writeTripsInterval==0.  vsp default is to write trips at least once (for simwrapper).") ;
		}

		return problem;
	}

}
