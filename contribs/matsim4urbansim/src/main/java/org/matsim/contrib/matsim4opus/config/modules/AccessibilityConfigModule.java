/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.matsim4opus.config.modules;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.experimental.ReflectiveModule;

public class AccessibilityConfigModule extends ReflectiveModule{
	// yyyy todo: change in similar way as with other modes ("_mode") 
	
	private static final String USING_CUSTOM_BOUNDING_BOX = "usingCustomBoundingBox";

	private static final String BOUNDING_BOX_BOTTOM = "boundingBoxBottom";

	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger( AccessibilityConfigModule.class ) ;

	public static final String GROUP_NAME = "accessibility";
	
//	private static final String LOGIT_SCALE_PARAMETER = "logitScaleParameter";
	private static final String USING_RAW_SUMS_WITHOUT_LN = "usingRawSumsWithoutLn";
//	private static final String USING_PT_PARAMETERS_FROM_MATSIM = "usingPtParametersFromMATSim";
//	private static final String USING_WALK_PARAMETERS_FROM_MATSIM = "usingWalkParametersFromMATSim";
//	private static final String USING_BIKE_PARAMETERS_FROM_MATSIM = "usingBikeParametersFromMATSim";
//	private static final String USING_CAR_PARAMETERS_FROM_MATSIM = "usingCarParametersFromMATSim";

	private static final String ACCESSIBILITY_DESTINATION_SAMPLING_RATE = "accessibilityDestinationSamplingRate";

//	private static final String BETA_BIKE_LN_MONETARY_TRAVEL_COST = "betaBikeLnMonetaryTravelCost";
//	private static final String BETA_WALK_LN_MONETARY_TRAVEL_COST = "betaWalkLnMonetaryTravelCost";
//	private static final String BETA_PT_LN_MONETARY_TRAVEL_COST = "betaPtLnMonetaryTravelCost";
//	private static final String BETA_CAR_LN_MONETARY_TRAVEL_COST = "betaCarLnMonetaryTravelCost";

	// ===
	
	private Double accessibilityDestinationSamplingRate;
	
//	private static final String USING_LOGIT_SCALE_PARAMETER_FROM_MATSIM = "usingScaleParameterFromMATSim" ;
//	private Boolean usingLogitScaleParameterFromMATSim;
	
//	private Boolean usingCarParameterFromMATSim = true ;
//	private Boolean usingBikeParameterFromMATSim = true ;
//	private Boolean usingWalkParameterFromMATSim = true ;
//	private Boolean usingPtParameterFromMATSim = true ;
    
	private Boolean usingRawSumsWithoutLn = false ;
    
	private Double logitScaleParameter;
	
	private boolean usingCustomBoundingBox;
	private double boundingBoxTop;
	private double boundingBoxLeft;
    private double boundingBoxRight;
    private double boundingBoxBottom;
	
	private int cellSizeCellBasedAccessibility;
	private boolean isCellBasedAccessibilityNetwork;
	private boolean isCellbasedAccessibilityShapeFile;
	private String shapeFileCellBasedAccessibility;

	// ===
    
//	private Double betaCarTravelTime;
//	private Double betaCarTravelTimePower2;
//	private Double betaCarLnTravelTime;
//	private Double betaCarTravelDistance;
//	private Double betaCarTravelDistancePower2;
//	private Double betaCarLnTravelDistance;
//	private Double betaCarTravelMonetaryCost;
//	private Double betaCarTravelMonetaryCostPower2;
//	private Double betaCarLnTravelMonetaryCost;
//	
//	private Double betaBikeTravelTime;
//	private Double betaBikeTravelTimePower2;
//	private Double betaBikeLnTravelTime;
//	private Double betaBikeTravelDistance;
//	private Double betaBikeTravelDistancePower2;
//	private Double betaBikeLnTravelDistance;
//	private Double betaBikeTravelMonetaryCost;
//	private Double betaBikeTravelMonetaryCostPower2;
//	private Double betaBikeLnTravelMonetrayCost;
//
//	private Double betaWalkTravelTime;
//	private Double betaWalkTravelTimePower2;
//	private Double betaWalkLnTravelTime;
//	private Double betaWalkTravelDistance;
//	private Double betaWalkTravelDistancePower2;
//	private Double betaWalkLnTravelDistance;
//	private Double betaWalkTravelMonetaryCost;
//	private Double betaWalkTravelMonetrayCostPower2;
//	private Double betaWalkLnTravelMonetrayCost;
//	
//	private Double betaPtTravelTime;
//	private Double betaPtTravelTimePower2;
//	private Double betaPtLnTravelTime;
//	private Double betaPtTravelDistance;
//	private Double betaPtTravelDistancePower2;
//	private Double betaPtLnTravelDistance;
//	private Double betaPtTravelMonetrayCost;
//	private Double betaPtTravelMonetrayCostPower2;
//	private Double betaPtLnTravelMonetrayCost;

	public static final String TIME_OF_DAY = "timeOfDay";
	private Double timeOfDay = 8.*3600 ;

	public AccessibilityConfigModule() {
		super(GROUP_NAME);
		// this class feels quite dangerous to me; one can have inconsistent entries between the Map and the typed values. kai, apr'13
		// no longer.  kai, may'13
	}
	
	@Override
	public Map<String,String> getComments() {
		Map<String,String> map = new TreeMap<String,String>() ;
		
		map.put(TIME_OF_DAY, "time of day at which trips for accessibility computations are assumed to start") ;
		
		map.put(ACCESSIBILITY_DESTINATION_SAMPLING_RATE, "if only a sample of destinations should be used " +
				"(reduces accuracy -- not recommended except when necessary for computational speed reasons)" ) ;
		
//		map.put(USING_LOGIT_SCALE_PARAMETER_FROM_MATSIM, "false if you want the logit model scale parameter in the " +
//				"accessibility computation different from the one in the travel model.  May be useful if you know what you are doing") ;
//		map.put(LOGIT_SCALE_PARAMETER, "logit scale parameter for accessibility computation (if enabled)") ;

//		map.put(USING_BIKE_PARAMETERS_FROM_MATSIM, "set to false if you want to enable using other parameters for the " +
//				"accessibility computation than for the travel model.  not recommended" ) ;
//		map.put(USING_CAR_PARAMETERS_FROM_MATSIM, "set to false if you want to enable using other parameters for the " +
//				"accessibility computation than for the travel model.  not recommended" ) ;
//		map.put(USING_PT_PARAMETERS_FROM_MATSIM, "set to false if you want to enable using other parameters for the " +
//				"accessibility computation than for the travel model.  not recommended" ) ;
//		map.put(USING_WALK_PARAMETERS_FROM_MATSIM, "set to false if you want to enable using other parameters for the " +
//				"accessibility computation than for the travel model.  not recommended" ) ;
		
//		map.put(BETA_CAR_LN_MONETARY_TRAVEL_COST,"car parameters for accessibility computation. separate from parameters for travel model") ;
//		map.put(BETA_PT_LN_MONETARY_TRAVEL_COST,"public transit parameters for accessibility. separate from parameters for travel model") ;
//		map.put(BETA_WALK_LN_MONETARY_TRAVEL_COST,"walk parameters for accessibility computation. separate from parameters for travel model") ;
//		map.put(BETA_BIKE_LN_MONETARY_TRAVEL_COST,"bicycle parameters for accessibility computation.  separate from parameters for travel model") ;
		
		map.put(USING_RAW_SUMS_WITHOUT_LN, "econometric accessibility usually returns the logsum. " +
				"Set to true if you just want the sum (without the ln)") ;
		
		map.put(USING_CUSTOM_BOUNDING_BOX, "true if custom bounding box should be used for accessibility computation (otherwise e.g. extent of network will be used)") ;
		map.put(BOUNDING_BOX_BOTTOM,"custom bounding box parameters for accessibility computation (if enabled)") ;
		
		
		return map ;
	}
	
	// NOTE: It seems ok to have the string constants immediately here since having them separately really does not help
	// keeping the code compact
	
	@StringGetter("usingShapeFileForExtentOfAccessibilityComputation")
    public boolean isCellBasedAccessibilityShapeFile() {
        return this.isCellbasedAccessibilityShapeFile;
    }
	@StringSetter("usingShapeFileForExtentOfAccessibilityComputation")
    public void setCellBasedAccessibilityShapeFile(boolean value) {
        this.isCellbasedAccessibilityShapeFile = value;
    }
	@StringGetter("usingNetworkForExtentOfAccessibilityComputation")
    public boolean isCellBasedAccessibilityNetwork() {
        return this.isCellBasedAccessibilityNetwork;
    }
	@StringSetter("usingNetworkForExtentOfAccessibilityComputation")
    public void setCellBasedAccessibilityNetwork(boolean value) {
        this.isCellBasedAccessibilityNetwork = value;
    }
	@StringGetter("cellSizeForCellBasedAccessibility") 
    public int getCellSizeCellBasedAccessibility() {
        return this.cellSizeCellBasedAccessibility;
    }
	@StringSetter("cellSizeForCellBasedAccessibility")  // size in meters (whatever that is since the coord system does not know about meters)
    public void setCellSizeCellBasedAccessibility(int value) {
        this.cellSizeCellBasedAccessibility = value;
    }
	@StringGetter("extentOfAccessibilityComputationShapeFile")
    public String getShapeFileCellBasedAccessibility() {
        return this.shapeFileCellBasedAccessibility;
    }
	@StringSetter("extentOfAccessibilityComputationShapeFile")
    public void setShapeFileCellBasedAccessibility(String value) {
        this.shapeFileCellBasedAccessibility = value;
    }

	@StringGetter(TIME_OF_DAY)
	public Double getTimeOfDay() {
		return this.timeOfDay ;
	}
	@StringSetter(TIME_OF_DAY)
	public void setTimeOfDay( Double val ) {
		this.timeOfDay = val ;
	}
	
	@StringGetter(ACCESSIBILITY_DESTINATION_SAMPLING_RATE)
	public Double getAccessibilityDestinationSamplingRate(){
		return this.accessibilityDestinationSamplingRate;
	}
	@StringSetter(ACCESSIBILITY_DESTINATION_SAMPLING_RATE)
	public void setAccessibilityDestinationSamplingRate(Double sampleRate){
		this.accessibilityDestinationSamplingRate = sampleRate;
	}
//    @StringGetter(USING_LOGIT_SCALE_PARAMETER_FROM_MATSIM)
//    public Boolean isUsingLogitScaleParameterFromMATSim() {
//        return usingLogitScaleParameterFromMATSim;
//    }
//    @StringSetter(USING_LOGIT_SCALE_PARAMETER_FROM_MATSIM)
//    public void setUsingLogitScaleParameterFromMATSim(Boolean value) {
//        this.usingLogitScaleParameterFromMATSim = value;
//    }
//    @StringGetter(USING_CAR_PARAMETERS_FROM_MATSIM)
//    public Boolean isUsingCarParametersFromMATSim() {
//        return usingCarParameterFromMATSim;
//    }
//    @StringSetter(USING_CAR_PARAMETERS_FROM_MATSIM)
//    public void setUsingCarParametersFromMATSim(Boolean value) {
//        this.usingCarParameterFromMATSim = value;
//    }
//    @StringGetter(USING_BIKE_PARAMETERS_FROM_MATSIM)
//    public Boolean isUsingBikeParametersFromMATSim() {
//        return usingBikeParameterFromMATSim;
//    }
//    @StringSetter(USING_BIKE_PARAMETERS_FROM_MATSIM)
//    public void setUsingBikeParameterFromMATSim(Boolean value) {
//        this.usingBikeParameterFromMATSim = value;
//    }
//    @StringGetter(USING_WALK_PARAMETERS_FROM_MATSIM)
//    public Boolean isUsingWalkParametersFromMATSim() {
//        return usingWalkParameterFromMATSim;
//    }
//    @StringSetter(USING_WALK_PARAMETERS_FROM_MATSIM)
//    public void setUsingWalkParametersFromMATSim(Boolean value) {
//        this.usingWalkParameterFromMATSim = value;
//    }
//    @StringGetter(USING_PT_PARAMETERS_FROM_MATSIM)    
//    public Boolean isUsingPtParametersFromMATSim() {
//        return usingPtParameterFromMATSim;
//    }
//    @StringSetter(USING_PT_PARAMETERS_FROM_MATSIM)
//    public void setUsingPtParametersFromMATSim(Boolean value) {
//        this.usingPtParameterFromMATSim = value;
//    }
    @StringGetter(USING_RAW_SUMS_WITHOUT_LN)
    public Boolean isUsingRawSumsWithoutLn() {
        return usingRawSumsWithoutLn;
    }
    @StringSetter(USING_RAW_SUMS_WITHOUT_LN)
    public void setUsingRawSumsWithoutLn(Boolean value) {
        this.usingRawSumsWithoutLn = value;
    }
//    @StringGetter(LOGIT_SCALE_PARAMETER)
//    public Double getLogitScaleParameter() {
//        return logitScaleParameter;
//    }
//    @StringSetter(LOGIT_SCALE_PARAMETER)
//    public void setLogitScaleParameter(Double value) {
//        this.logitScaleParameter = value;
//    }
    // === 
    @StringGetter(USING_CUSTOM_BOUNDING_BOX)
    public boolean usingCustomBoundingBox() {
        return this.usingCustomBoundingBox;
    }
    @StringSetter(USING_CUSTOM_BOUNDING_BOX)
    public void setUsingCustomBoundingBox(boolean value) {
        this.usingCustomBoundingBox = value;
    }
    @StringGetter("boundingBoxTop")
    public double getBoundingBoxTop() {
        return this.boundingBoxTop;
    }
    @StringSetter("boundingBoxTop")
    public void setBoundingBoxTop(double value) {
        this.boundingBoxTop = value;
    }
    @StringGetter("boundingBoxLeft")
    public double getBoundingBoxLeft() {
        return this.boundingBoxLeft;
    }
    @StringSetter("boundingBoxLeft")
    public void setBoundingBoxLeft(double value) {
        this.boundingBoxLeft = value;
    }
    @StringGetter("boundingBoxRight")
    public double getBoundingBoxRight() {
        return this.boundingBoxRight;
    }
    @StringSetter("boundingBoxRight")
    public void setBoundingBoxRight(double value) {
        this.boundingBoxRight = value;
    }
    @StringGetter(BOUNDING_BOX_BOTTOM)
    public double getBoundingBoxBottom() {
        return this.boundingBoxBottom;
    }
    @StringSetter(BOUNDING_BOX_BOTTOM)
    public void setBoundingBoxBottom(double value) {
        this.boundingBoxBottom = value;
    }
    // ===
    // only betas below this line
//    @StringGetter("betaCarTravelTime")
//    public Double getBetaCarTravelTime() {
//        return betaCarTravelTime;
//    }
//    @StringSetter("betaCarTravelTime")
//    public void setBetaCarTravelTime(Double value) {
//        this.betaCarTravelTime = value;
//    }
//    @StringGetter("betaCarTravelTimePower2")
//    public Double getBetaCarTravelTimePower2() {
//        return betaCarTravelTimePower2;
//    }
//    @StringSetter("betaCarTravelTimePower2")
//    public void setBetaCarTravelTimePower2(Double value) {
//        this.betaCarTravelTimePower2 = value;
//    }
//    @StringGetter("betaCarLnTravelTime")
//    public Double getBetaCarLnTravelTime() {
//        return betaCarLnTravelTime;
//    }
//    @StringSetter("betaCarLnTravelTime")
//    public void setBetaCarLnTravelTime(Double value) {
//        this.betaCarLnTravelTime = value;
//    }
//    @StringGetter("betaCarTravelDistance")
//    public Double getBetaCarTravelDistance() {
//        return betaCarTravelDistance;
//    }
//    @StringSetter("betaCarTravelDistance")
//    public void setBetaCarTravelDistance(Double value) {
//        this.betaCarTravelDistance = value;
//    }
//    @StringGetter("betaCarTravelDistancePower2")
//    public Double getBetaCarTravelDistancePower2() {
//        return betaCarTravelDistancePower2;
//    }
//    @StringSetter("betaCarTravelDistancePower2")
//    public void setBetaCarTravelDistancePower2(Double value) {
//        this.betaCarTravelDistancePower2 = value;
//    }
//    @StringGetter("betaCarLnTravelDistance")
//    public Double getBetaCarLnTravelDistance() {
//        return betaCarLnTravelDistance;
//    }
//    @StringSetter("betaCarLnTravelDistance")
//    public void setBetaCarLnTravelDistance(Double value) {
//        this.betaCarLnTravelDistance = value;
//    }
//    @StringGetter("betaCarMonetaryTravelCost")
//    public Double getBetaCarTravelMonetaryCost() {
//        return betaCarTravelMonetaryCost;
//    }
//    @StringSetter("betaCarMonetaryTravelCost")
//    public void setBetaCarTravelMonetaryCost(Double value) {
//        this.betaCarTravelMonetaryCost = value;
//    }
//    @StringGetter("betaCarMonetaryTravelCostPower2")
//    public Double getBetaCarTravelMonetaryCostPower2() {
//        return betaCarTravelMonetaryCostPower2;
//    }
//    @StringSetter("betaCarMonetaryTravelCostPower2")
//    public void setBetaCarTravelMonetaryCostPower2(Double value) {
//        this.betaCarTravelMonetaryCostPower2 = value;
//    }
//    @StringGetter(BETA_CAR_LN_MONETARY_TRAVEL_COST)
//    public Double getBetaCarLnTravelMonetaryCost() {
//        return betaCarLnTravelMonetaryCost;
//    }
//    @StringSetter(BETA_CAR_LN_MONETARY_TRAVEL_COST)
//    public void setBetaCarLnTravelMonetaryCost(Double value) {
//        this.betaCarLnTravelMonetaryCost = value;
//    }
//    // ===
//    @StringGetter("betaBikeTravelTime")
//    public Double getBetaBikeTravelTime() {
//        return betaBikeTravelTime;
//    }
//    @StringSetter("betaBikeTravelTime")
//    public void setBetaBikeTravelTime(Double value) {
//        this.betaBikeTravelTime = value;
//    }
//    @StringGetter("betaBikeTravelTimePower2")
//    public Double getBetaBikeTravelTimePower2() {
//        return betaBikeTravelTimePower2;
//    }
//    @StringSetter("betaBikeTravelTimePower2")
//    public void setBetaBikeTravelTimePower2(Double value) {
//        this.betaBikeTravelTimePower2 = value;
//    }
//    @StringGetter("betaBikeLnTravelTime")
//    public Double getBetaBikeLnTravelTime() {
//        return betaBikeLnTravelTime;
//    }
//    @StringSetter("betaBikeLnTravelTime")
//    public void setBetaBikeLnTravelTime(Double value) {
//        this.betaBikeLnTravelTime = value;
//    }
//    @StringGetter("betaBikeTravelDistance")
//    public Double getBetaBikeTravelDistance() {
//        return betaBikeTravelDistance;
//    }
//    @StringSetter("betaBikeTravelDistance")
//    public void setBetaBikeTravelDistance(Double value) {
//        this.betaBikeTravelDistance = value;
//    }
//    @StringGetter("betaBikeTravelDistancePower2")
//    public Double getBetaBikeTravelDistancePower2() {
//        return betaBikeTravelDistancePower2;
//    }
//    @StringSetter("betaBikeTravelDistancePower2")
//    public void setBetaBikeTravelDistancePower2(Double value) {
//        this.betaBikeTravelDistancePower2 = value;
//    }
//    @StringGetter("betaBikeLnTravelDistance")
//    public Double getBetaBikeLnTravelDistance() {
//        return betaBikeLnTravelDistance;
//    }
//    @StringSetter("betaBikeLnTravelDistance")
//    public void setBetaBikeLnTravelDistance(Double value) {
//        this.betaBikeLnTravelDistance = value;
//    }
//    @StringGetter("betaBikeMonetaryTravelCost")
//    public Double getBetaBikeTravelMonetaryCost() {
//        return betaBikeTravelMonetaryCost;
//    }
//    @StringSetter("betaBikeMonetaryTravelCost")
//    public void setBetaBikeTravelMonetaryCost(Double value) {
//        this.betaBikeTravelMonetaryCost = value;
//    }
//    @StringGetter("betaBikeMonetaryTravelCostPower2")
//    public Double getBetaBikeTravelMonetaryCostPower2() {
//        return betaBikeTravelMonetaryCostPower2;
//    }
//    @StringSetter("betaBikeMonetaryTravelCostPower2")
//    public void setBetaBikeTravelMonetaryCostPower2(Double value) {
//        this.betaBikeTravelMonetaryCostPower2 = value;
//    }
//    @StringGetter(BETA_BIKE_LN_MONETARY_TRAVEL_COST)
//    public Double getBetaBikeLnTravelMonetaryCost() {
//        return betaBikeLnTravelMonetrayCost;
//    }
//    @StringSetter(BETA_BIKE_LN_MONETARY_TRAVEL_COST)
//    public void setBetaBikeLnTravelMonetaryCost(Double value) {
//        this.betaBikeLnTravelMonetrayCost = value;
//    }
//    // ===
//    @StringGetter("betaWalkTravelTime")
//    public Double getBetaWalkTravelTime() {
//        return betaWalkTravelTime;
//    }
//    @StringSetter("betaWalkTravelTime")
//    public void setBetaWalkTravelTime(Double value) {
//        this.betaWalkTravelTime = value;
//    }
//    @StringGetter("betaWalkTravelTimePower2")
//    public Double getBetaWalkTravelTimePower2() {
//        return betaWalkTravelTimePower2;
//    }
//    @StringSetter("betaWalkTravelTimePower2")
//    public void setBetaWalkTravelTimePower2(Double value) {
//        this.betaWalkTravelTimePower2 = value;
//    }
//    @StringGetter("betaWalkLnTravelTime")
//    public Double getBetaWalkLnTravelTime() {
//        return betaWalkLnTravelTime;
//    }
//    @StringSetter("betaWalkLnTravelTime")
//    public void setBetaWalkLnTravelTime(Double value) {
//        this.betaWalkLnTravelTime = value;
//    }
//    @StringGetter("betaWalkTravelDistance")
//    public Double getBetaWalkTravelDistance() {
//        return betaWalkTravelDistance;
//    }
//    @StringSetter("betaWalkTravelDistance")
//    public void setBetaWalkTravelDistance(Double value) {
//        this.betaWalkTravelDistance = value;
//    }
//    @StringGetter("betaWalkTravelDistancePower2")
//    public Double getBetaWalkTravelDistancePower2() {
//        return betaWalkTravelDistancePower2;
//    }
//    @StringSetter("betaWalkTravelDistancePower2")
//    public void setBetaWalkTravelDistancePower2(Double value) {
//        this.betaWalkTravelDistancePower2 = value;
//    }
//    @StringGetter("betaWalkLnTravelDistance")
//    public Double getBetaWalkLnTravelDistance() {
//        return betaWalkLnTravelDistance;
//    }
//    @StringSetter("betaWalkLnTravelDistance")
//    public void setBetaWalkLnTravelDistance(Double value) {
//        this.betaWalkLnTravelDistance = value;
//    }
//    @StringGetter("betaWalkMonetaryTravelCost")
//    public Double getBetaWalkTravelMonetaryCost() {
//        return betaWalkTravelMonetaryCost;
//    }
//    @StringSetter("betaWalkMonetaryTravelCost")
//    public void setBetaWalkTravelMonetaryCost(Double value) {
//        this.betaWalkTravelMonetaryCost = value;
//    }
//    @StringGetter("betaWalkMonetaryTravelCostPower2")
//    public Double getBetaWalkTravelMonetaryCostPower2() {
//        return betaWalkTravelMonetrayCostPower2;
//    }
//    @StringSetter("betaWalkMonetaryTravelCostPower2")
//    public void setBetaWalkTravelMonetaryCostPower2(Double value) {
//        this.betaWalkTravelMonetrayCostPower2 = value;
//    }
//    @StringGetter(BETA_WALK_LN_MONETARY_TRAVEL_COST)
//    public Double getBetaWalkLnTravelMonetaryCost() {
//        return betaWalkLnTravelMonetrayCost;
//    }
//    @StringSetter(BETA_WALK_LN_MONETARY_TRAVEL_COST)
//    public void setBetaWalkLnTravelMonetaryCost(Double value) {
//        this.betaWalkLnTravelMonetrayCost = value;
//    }
//    // ---
//    @StringGetter( "betaPtTravelTime" )
//    public Double getBetaPtTravelTime() {
//        return betaPtTravelTime;
//    }
//    @StringSetter( "betaPtTravelTime" )
//    public void setBetaPtTravelTime(Double value) {
//        this.betaPtTravelTime = value;
//    }
//    @StringGetter( "betaPtTravelTimePower2" )
//    public Double getBetaPtTravelTimePower2() {
//        return betaPtTravelTimePower2;
//    }
//    @StringSetter( "betaPtTravelTimePower2" )
//    public void setBetaPtTravelTimePower2(Double value) {
//        this.betaPtTravelTimePower2 = value;
//    }
//    @StringGetter( "betaPtLnTravelTime" )
//    public Double getBetaPtLnTravelTime() {
//        return betaPtLnTravelTime;
//    }
//    @StringSetter( "betaPtLnTravelTime" )
//    public void setBetaPtLnTravelTime(Double value) {
//        this.betaPtLnTravelTime = value;
//    }
//    @StringGetter( "betaPtTravelDistance" )
//    public Double getBetaPtTravelDistance() {
//        return betaPtTravelDistance;
//    }
//    @StringSetter( "betaPtTravelDistance" )
//    public void setBetaPtTravelDistance(Double value) {
//        this.betaPtTravelDistance = value;
//    }
//    @StringGetter( "betaPtTravelDistancePower2" )
//    public Double getBetaPtTravelDistancePower2() {
//        return betaPtTravelDistancePower2;
//    }
//    @StringSetter( "betaPtTravelDistancePower2" )
//    public void setBetaPtTravelDistancePower2(Double value) {
//        this.betaPtTravelDistancePower2 = value;
//    }
//    @StringGetter( "betaPtLnTravelDistance" )
//    public Double getBetaPtLnTravelDistance() {
//        return betaPtLnTravelDistance;
//    }
//    @StringSetter( "betaPtLnTravelDistance" )
//    public void setBetaPtLnTravelDistance(Double value) {
//        this.betaPtLnTravelDistance = value;
//    }
//    @StringGetter( "betaPtMonetaryTravelCost" )
//    public Double getBetaPtTravelMonetaryCost() {
//        return betaPtTravelMonetrayCost;
//    }
//    @StringSetter( "betaPtMonetaryTravelCost" )
//    public void setBetaPtTravelMonetaryCost(Double value) {
//        this.betaPtTravelMonetrayCost = value;
//    }
//    @StringGetter( "betaPtMonetaryTravelCostPower2" )
//    public Double getBetaPtTravelMonetaryCostPower2() {
//        return betaPtTravelMonetrayCostPower2;
//    }
//    @StringSetter( "betaPtMonetaryTravelCostPower2" )
//    public void setBetaPtTravelMonetaryCostPower2(Double value) {
//        this.betaPtTravelMonetrayCostPower2 = value;
//    }
//    @StringGetter( BETA_PT_LN_MONETARY_TRAVEL_COST )
//    public Double getBetaPtLnTravelMonetaryCost() {
//        return betaPtLnTravelMonetrayCost;
//    }
//    @StringSetter( BETA_PT_LN_MONETARY_TRAVEL_COST )
//    public void setBetaPtLnTravelMonetaryCost(Double value) {
//        this.betaPtLnTravelMonetrayCost = value;
//    }
    
}
