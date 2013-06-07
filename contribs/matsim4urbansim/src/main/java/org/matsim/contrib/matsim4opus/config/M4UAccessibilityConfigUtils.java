/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.contrib.matsim4opus.config;

import org.matsim.contrib.matsim4opus.config.modules.AccessibilityConfigGroup;
import org.matsim.contrib.matsim4opus.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * @author nagel
 *
 */
public class M4UAccessibilityConfigUtils {
	private M4UAccessibilityConfigUtils() {} // static methods only
	
	static void initAccessibilityParameters(Matsim4UrbansimType matsim4UrbanSimParamsFromU, Config config){
	
		AccessibilityConfigGroup module = getConfigModuleAndPossiblyConvert(config);
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
	
//		// logit scale parameter:
//		boolean useMATSimLogitScaleParameter 	= matsim4UrbanSimParamsFromU.getAccessibilityParameter().isUseLogitScaleParameterFromMATSim();
//		// (yyyy remove from UrbanSim)
//		if ( module.isUsingLogitScaleParameterFromMATSim() != null ) {
//			useMATSimLogitScaleParameter = module.isUsingLogitScaleParameterFromMATSim() ;
//		}
//		module.setUsingLogitScaleParameterFromMATSim(useMATSimLogitScaleParameter);
//	
//		double logitScaleParameter;	
//		if ( useMATSimLogitScaleParameter ) {
//			logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta();
//		} else {
//			logitScaleParameter = matsim4UrbanSimParamsFromU.getAccessibilityParameter().getLogitScaleParameter();
//			// (yyyy remove from UrbanSim)
//			if ( module.getLogitScaleParameter() != null ) {
//				logitScaleParameter = module.getLogitScaleParameter();
//			}
//		}
//		module.setLogitScaleParameter(logitScaleParameter);
	
		// accessibility destination sampling rate:
		double accessibilityDestinationSamplingRate = matsim4UrbanSimParamsFromU.getAccessibilityParameter().getAccessibilityDestinationSamplingRate();
		// (maybe leave in UrbanSim)
		if ( module.getAccessibilityDestinationSamplingRate() != null ) {
			accessibilityDestinationSamplingRate = module.getAccessibilityDestinationSamplingRate() ;
		}
		module.setAccessibilityDestinationSamplingRate(accessibilityDestinationSamplingRate);
		
//		// which betas?
//		// car:
//		boolean useMATSimCarParameter			= matsim4UrbanSimParamsFromU.getAccessibilityParameter().isUseCarParameterFromMATSim();
//		// (yyyy remove from UrbanSim)
//		if ( module.isUsingCarParametersFromMATSim() != null ) {
//			useMATSimCarParameter = module.isUsingCarParametersFromMATSim() ;
//		}
//		module.setUsingCarParametersFromMATSim(useMATSimCarParameter);
//		// walk:
//		boolean useMATSimWalkParameter			= matsim4UrbanSimParamsFromU.getAccessibilityParameter().isUseWalkParameterFromMATSim();
//		// (yyyy remove from UrbanSim)
//		if ( module.isUsingWalkParametersFromMATSim() != null ) {
//			useMATSimWalkParameter = module.isUsingWalkParametersFromMATSim() ;
//		}
//		module.setUsingWalkParametersFromMATSim(useMATSimWalkParameter);
//		// pt & bicycle not in UrbanSim...
	
		// raw sums?
		// (leave in urbansim)
		boolean useRawSum						= matsim4UrbanSimParamsFromU.getAccessibilityParameter().isUseRawSumsWithoutLn();
		if ( module.isUsingRawSumsWithoutLn() != null ) {
			useRawSum = module.isUsingRawSumsWithoutLn() ;
		}
		module.setUsingRawSumsWithoutLn(useRawSum);
	
//		final String noSeparateBetasMessage = "This MATSim4UrbanSim version does not support custom beta parameters such " +
//				"as \"betaBikeTravelTime\" etc. anymore (both in the UrbanSim GUI (car and walk) and the external MATSim config " +
//				"file (bike and pt)). Please let us know if this causes serious problems." +
//				"To avoid the error message please : 1) select \"use_car_parameter_from_MATSim\" " +
//				"and \"use_walk_parameter_from_MATSim\" in the UrbanSim GUI and 2) remove all beta parameters for bike and pt " +
//				"(such as \"<param name=\"betaBikeTravelTime\" value=\"-12.\" />\") from your external MATSim config file.";
//	
//		if(useMATSimCarParameter){
//			module.setBetaCarTravelTime(planCalcScoreConfigGroup.getTraveling_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr() ); // [utils/h]
//			module.setBetaCarTravelDistance( planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRateCar() ); 
//			module.setBetaCarTravelMonetaryCost( - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ); // [utils/money]
//			module.setBetaCarLnTravelDistance(0.) ;
//			module.setBetaCarLnTravelMonetaryCost(0.) ;
//			module.setBetaCarLnTravelTime(0.) ;
//			module.setBetaCarTravelTimePower2(0.) ;
//			module.setBetaCarTravelMonetaryCostPower2(0.) ;
//			module.setBetaCarTravelDistancePower2(0.) ;
//		} else{
//			throw new RuntimeException(noSeparateBetasMessage);
//		}
//	
//		if( module.isUsingBikeParametersFromMATSim() ) {
//			module.setBetaBikeTravelTime( planCalcScoreConfigGroup.getTravelingBike_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr() ) ;
//			module.setBetaBikeTravelDistance( planCalcScoreConfigGroup.getMarginalUtlOfDistanceOther() ) ; // [utils/meter]
//			module.setBetaBikeTravelMonetaryCost( - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ) ; // [utils/money]
//			module.setBetaBikeLnTravelDistance(0.) ;
//			module.setBetaBikeLnTravelMonetaryCost(0.) ;
//			module.setBetaBikeLnTravelTime(0.) ;
//			module.setBetaBikeTravelTimePower2(0.) ;
//			module.setBetaBikeTravelMonetaryCostPower2(0.) ;
//			module.setBetaBikeTravelDistancePower2(0.) ;
//		} else{
//			throw new RuntimeException(noSeparateBetasMessage);
//		}
//	
//		if(useMATSimWalkParameter){
//			module.setBetaWalkTravelTime( planCalcScoreConfigGroup.getTravelingWalk_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr() ) ; // [utils/h]
//			module.setBetaWalkTravelDistance( planCalcScoreConfigGroup.getMarginalUtlOfDistanceWalk() ); // [utils/meter]
//			module.setBetaWalkTravelMonetaryCost( - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ) ; // [utils/money]
//			module.setBetaWalkLnTravelDistance(0.) ;
//			module.setBetaWalkLnTravelMonetaryCost(0.) ;
//			module.setBetaWalkLnTravelTime(0.) ;
//			module.setBetaWalkTravelTimePower2(0.) ;
//			module.setBetaWalkTravelMonetaryCostPower2(0.) ;
//			module.setBetaWalkTravelDistancePower2(0.) ;
//		}
//		else{
//			throw new RuntimeException(noSeparateBetasMessage);
//		}
//	
//		if( module.isUsingPtParametersFromMATSim() ) {
//			module.setBetaPtTravelTime( planCalcScoreConfigGroup.getTravelingPt_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr() ) ; // [utils/h]
//			module.setBetaPtTravelDistance( planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRatePt() ); // [utils/meter]
//			module.setBetaPtTravelMonetaryCost( - planCalcScoreConfigGroup.getMarginalUtilityOfMoney() ) ; // [utils/money]
//			module.setBetaPtLnTravelDistance(0.) ;
//			module.setBetaPtLnTravelMonetaryCost(0.) ;
//			module.setBetaPtLnTravelTime(0.) ;
//			module.setBetaPtTravelTimePower2(0.) ;
//			module.setBetaPtTravelMonetaryCostPower2(0.) ;
//			module.setBetaPtTravelDistancePower2(0.) ;
//		}
//		else{
//			throw new RuntimeException(noSeparateBetasMessage);
//		}
	
	}

	public static AccessibilityConfigGroup getConfigModuleAndPossiblyConvert(Config config) {
		Module m = config.getModule(AccessibilityConfigGroup.GROUP_NAME);
		if (m instanceof AccessibilityConfigGroup) {
			return (AccessibilityConfigGroup) m;
		}
		AccessibilityConfigGroup module = new AccessibilityConfigGroup();
		//		config.getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		// yyyyyy the above code does NOT convert but throws the config entries away.
		// In contrast, config.addModule(...) would convert.  kai, may'13
		// I just changed that:
		config.addModule(AccessibilityConfigGroup.GROUP_NAME, module ) ;
		return module;
	}


}
