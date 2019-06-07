/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.accessibility;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * @author thomas, nagel, dziemke
 */
public final class AccessibilityConfigGroup extends ReflectiveConfigGroup{
	// yyyy todo: change in similar way as with other modes ("_mode") 
	
	private static final String USING_CUSTOM_BOUNDING_BOX = "usingCustomBoundingBox";
	private static final String BOUNDING_BOX_TOP = "boundingBoxTop";
	private static final String BOUNDING_BOX_BOTTOM = "boundingBoxBottom";
	private static final String BOUNDING_BOX_LEFT = "boundingBoxLeft";
	private static final String BOUNDING_BOX_RIGHT = "boundingBoxRight";

	@SuppressWarnings("unused")
	private static Logger LOG = Logger.getLogger(AccessibilityConfigGroup.class);

	public static final String GROUP_NAME = "accessibility";
	
	private static final String ACCESSIBILITY_MEASURE_TYPE = "accessibilityMeasureType";
	public static enum AccessibilityMeasureType{logSum, rawSum, gravity}
	private AccessibilityMeasureType accessibilityMeasureType = AccessibilityMeasureType.logSum;
	
	private static final String USE_OPPORTUNITY_WEIGHTS = "useOpportunityWeights";
	private boolean useOpportunityWeights = false;
	private static final String WEIGHT_EXPONENT = "weightExponent";
	private Double weightExponent = 1.;
	
//	private static final String ACCESSIBILITY_DESTINATION_SAMPLING_RATE = "accessibilityDestinationSamplingRate";
//	private Double accessibilityDestinationSamplingRate;

	private static final String MEASURE_POINT_GEOMETRY_PROVISION = "measurePointGeometryProvision";
	public static enum MeasurePointGeometryProvision{autoCreate, fromShapeFile} 
	private MeasurePointGeometryProvision measurePointGeometryProvision = MeasurePointGeometryProvision.autoCreate;
	
	private double boundingBoxTop;
	private double boundingBoxLeft;
    private double boundingBoxRight;
    private double boundingBoxBottom;
	
	private Integer tileSize_m;
	private String shapeFileCellBasedAccessibility;
	
	private static final String AREA_OF_ACC_COMP = "areaOfAccessibilityComputation"; 
	public static enum AreaOfAccesssibilityComputation{fromNetwork, fromBoundingBox, fromBoundingBoxHexagons, fromShapeFile, fromFacilitiesFile, fromFacilitiesObject} 
	private AreaOfAccesssibilityComputation areaOfAccessibilityComputation = AreaOfAccesssibilityComputation.fromNetwork;
	private Set<Modes4Accessibility> isComputingMode = EnumSet.noneOf(Modes4Accessibility.class);
	
	private String outputCrs = null;
	private static final String OUTPUT_CRS="outputCRS";

	
	@StringGetter(OUTPUT_CRS)
	public final String getOutputCrs() {
		return this.outputCrs;
	}
	@StringSetter(OUTPUT_CRS)
	public final void setOutputCrs(String outputCrs) {
		this.outputCrs = outputCrs;
	}

	private String measuringPointsFile;
	private static final String MEASURING_POINTS_FILE="measuringPointsFile";
	
	@StringGetter(MEASURING_POINTS_FILE)
	public String getMeasuringPointsFile(){
		return this.measuringPointsFile;
	}
	
	@StringSetter(MEASURING_POINTS_FILE)
	public void setMeasuringPointsFile(String measuringPointsFile){
		this.measuringPointsFile = measuringPointsFile;
	}
	
	// Optional; only used if measuring points are set directly
	private ActivityFacilities measuringPointsFacilities;
	private Map<Id<ActivityFacility>, Geometry> measurePointGeometryMap;

	public static final String TIME_OF_DAY = "timeOfDay";
	private Double timeOfDay = 8.*3600;

	public AccessibilityConfigGroup() {
		super(GROUP_NAME);
		isComputingMode.add(Modes4Accessibility.freespeed);
	}
	
	@Override
	public Map<String,String> getComments() {
		Map<String,String> map = new TreeMap<>() ;
		
		map.put(TIME_OF_DAY, "time of day at which trips for accessibility computations are assumed to start");
		
//		map.put(ACCESSIBILITY_DESTINATION_SAMPLING_RATE, "if only a sample of destinations should be used " +
//				"(reduces accuracy -- not recommended except when necessary for computational speed reasons)");
		
		map.put(ACCESSIBILITY_MEASURE_TYPE, "defines type of measure for accessibility computation.");
		
		map.put(USING_CUSTOM_BOUNDING_BOX, "true if custom bounding box should be used for accessibility computation (otherwise e.g. extent of network will be used)");
		map.put(BOUNDING_BOX_BOTTOM,"custom bounding box parameters for accessibility computation (if enabled)");
		
		StringBuilder stb = new StringBuilder() ;
		for (AreaOfAccesssibilityComputation val : AreaOfAccesssibilityComputation.values()) {
			stb.append(val.toString() ) ;
			stb.append( " " ) ;
		}
		map.put(AREA_OF_ACC_COMP, "method to determine the area for which the accessibility will be computed; possible values: " + stb);
		
		map.put(MEASURING_POINTS_FILE, "if the accibility is computed using the `fromFile` option, " +
				"the this must be the file containing the measuring points' coordinates. ");
		return map ;
	}
	
	public void setComputingAccessibilityForMode(Modes4Accessibility mode, boolean val) {
		if (val) {
			this.isComputingMode.add(mode);
		} else {
			this.isComputingMode.remove(mode);
		}
	}

	public Set<Modes4Accessibility> getIsComputingMode() {
		return isComputingMode;
	}

	
	// NOTE: It seems ok to have the string constants immediately here since having them separately really does not help
	// keeping the code compact
	
	@StringGetter("tileSize_m") 
    public Integer getTileSize() {
		return this.tileSize_m;
    }
	
	/**
	 * Set the size of the tiles (in meters if a metric CRS is used).
	 * If a square grid is created, this value will be used as the side length of each square.
	 * If a hexagon pattern is created, this value will be used as the maximum diameter of every hexagon.
	 */
	@StringSetter("tileSize_m")
    public void setTileSize_m(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("Tile size must be greater than zero.");
		}
		if (value < 100) {
			LOG.warn("Tile size = " + value + ". This is a comparatively small value, which may lead to computational problems.");
		}
		this.tileSize_m = value;
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
	public void setTimeOfDay(Double timeOfDay) {
		this.timeOfDay = timeOfDay;
	}
	
    @StringGetter(MEASURE_POINT_GEOMETRY_PROVISION)
    public MeasurePointGeometryProvision getMeasurePointGeometryProvision() {
        return this.measurePointGeometryProvision;
    }
    @StringSetter(MEASURE_POINT_GEOMETRY_PROVISION)
    public void setMeasurePointGeometryProvision(MeasurePointGeometryProvision measurePointGeometryProvision) {
        this.measurePointGeometryProvision = measurePointGeometryProvision;
    }
    @StringGetter(ACCESSIBILITY_MEASURE_TYPE)
    public AccessibilityMeasureType getAccessibilityMeasureType() {
        return this.accessibilityMeasureType;
    }
    @StringSetter(ACCESSIBILITY_MEASURE_TYPE)
    public void setAccessibilityMeasureType(AccessibilityMeasureType accessibilityMeasureType) {
        this.accessibilityMeasureType = accessibilityMeasureType;
    }
    @StringGetter(USE_OPPORTUNITY_WEIGHTS)
    public boolean isUseOpportunityWeights() {
    	return useOpportunityWeights;
    }
    @StringSetter(USE_OPPORTUNITY_WEIGHTS)
    public void setUseOpportunityWeights(Boolean useOpportunityWeights) {
    	this.useOpportunityWeights = useOpportunityWeights;
    }
    @StringGetter(WEIGHT_EXPONENT)
    public double getWeightExponent() {
    	return weightExponent;
    }
    @StringSetter(WEIGHT_EXPONENT)
    public void setWeightExponent(double weightExponent) {
    	this.weightExponent = weightExponent;
    }
    @StringGetter(BOUNDING_BOX_TOP)
    public double getBoundingBoxTop() {
        return this.boundingBoxTop;
    }
    @StringSetter(BOUNDING_BOX_TOP)
    public void setBoundingBoxTop(double value) {
        this.boundingBoxTop = value;
    }
    @StringGetter(BOUNDING_BOX_LEFT)
    public double getBoundingBoxLeft() {
        return this.boundingBoxLeft;
    }
    @StringSetter(BOUNDING_BOX_LEFT)
    public void setBoundingBoxLeft(double value) {
        this.boundingBoxLeft = value;
    }
    @StringGetter(BOUNDING_BOX_RIGHT)
    public double getBoundingBoxRight() {
        return this.boundingBoxRight;
    }
    @StringSetter(BOUNDING_BOX_RIGHT)
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
    
    /**
	 * helper method to set bounding box in code
	 */
    public void setEnvelope( Envelope envelope ) {
	    this.boundingBoxBottom = envelope.getMinY() ;
	    this.boundingBoxLeft = envelope.getMinX() ;
	    this.boundingBoxRight = envelope.getMaxX() ;
	    this.boundingBoxTop = envelope.getMaxY() ;
    }

    @StringGetter(AREA_OF_ACC_COMP)
	public AreaOfAccesssibilityComputation getAreaOfAccessibilityComputation() {
		return areaOfAccessibilityComputation;
	}

    @StringSetter(AREA_OF_ACC_COMP)
	public void setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation areaOfAccessibilityComputation) {
	    this.areaOfAccessibilityComputation = areaOfAccessibilityComputation ;
	}
    
    /**
	 * helper method to set measuring points in code
	 */
    public void setMeasuringPointsFacilities(ActivityFacilities measuringPointsFacilities) {
		this.measuringPointsFacilities = measuringPointsFacilities;
    }
    
    public ActivityFacilities getMeasuringPointsFacilities(){
		return this.measuringPointsFacilities;
	}
    
    public void setMeasurePointGeometryMap(Map<Id<ActivityFacility>, Geometry> measurePointGeometryMap) {
    	if (measuringPointsFacilities.getFacilities() == null) {
    		throw new RuntimeException("Setting geometries of measure points does not make sense if measure points are not yet set!");
    	}
		this.measurePointGeometryMap = measurePointGeometryMap;
    }
  
    public Map<Id<ActivityFacility>, Geometry> getMeasurePointGeometryMap(){
		return this.measurePointGeometryMap;
	}
}