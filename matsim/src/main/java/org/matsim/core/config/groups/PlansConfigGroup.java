/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class PlansConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "plans";

	public abstract static class NetworkRouteType {
		public static final String LinkNetworkRoute = "LinkNetworkRoute";
		public static final String CompressedNetworkRoute = "CompressedNetworkRoute";
	}

	public static enum ActivityDurationInterpretation { minOfDurationAndEndTime, tryEndTimeThenDuration, @Deprecated endTimeOnly }
	private static final String INPUT_FILE = "inputPlansFile";
	private static final String INPUT_PERSON_ATTRIBUTES_FILE = "inputPersonAttributesFile";
	private static final String NETWORK_ROUTE_TYPE = "networkRouteType";
	private static final String SUBPOPULATION_ATTRIBUTE = "subpopulationAttributeName";
	private static final String INPUT_CRS = "inputCRS";

	private String inputFile = null;
	private String networkRouteType = NetworkRouteType.LinkNetworkRoute;
	private String inputPersonAttributeFile = null;
	private String subpopulationAttributeName = "subpopulation";
	private String inputCRS = null;
	
	//--
	
	private static final String ACTIVITY_DURATION_INTERPRETATION="activityDurationInterpretation" ;
	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.tryEndTimeThenDuration ;

	//--

	public PlansConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(
				NETWORK_ROUTE_TYPE,
				"Defines how routes are stored in memory. Currently supported: " +
				NetworkRouteType.LinkNetworkRoute + ", " +
				NetworkRouteType.CompressedNetworkRoute + ".");
		comments.put(
				INPUT_PERSON_ATTRIBUTES_FILE,
				"Path to a file containing person attributes (required file format: ObjectAttributes).");
		comments.put(
				SUBPOPULATION_ATTRIBUTE,
				"Name of the (Object)Attribute defining the subpopulation to which pertains a Person"+
				" (as freight, through traffic, etc.). The attribute must be of String type." );

		StringBuilder str = new StringBuilder() ;
		for ( PlansConfigGroup.ActivityDurationInterpretation itp : PlansConfigGroup.ActivityDurationInterpretation.values() ) {
			str.append(" ").append(itp.toString());
		}
		comments.put(ACTIVITY_DURATION_INTERPRETATION, "String:" + str + ". Anything besides " 
				+ PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime + " will internally use a different " +
		"(simpler) version of the TimeAllocationMutator.") ;
		
		comments.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, "(not tested) will remove plan attributes that are presumably not used, such as " +
		"activityStartTime. default=false") ;

		comments.put( INPUT_CRS , "The Coordinates Reference System in which the coordinates are expressed in the input file." +
				" At import, the coordinates will be converted to the coordinate system defined in \"global\", and will" +
				"be converted back at export. If not specified, no conversion happens." );

		return comments;
	}

	@StringGetter( INPUT_FILE )
	public String getInputFile() {
		return this.inputFile;
	}

	@StringSetter( INPUT_FILE )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}
	
	@StringGetter( INPUT_PERSON_ATTRIBUTES_FILE )
	public String getInputPersonAttributeFile() {
		return this.inputPersonAttributeFile;
	}

	@StringSetter( INPUT_PERSON_ATTRIBUTES_FILE )
	public void setInputPersonAttributeFile(final String inputPersonAttributeFile) {
		this.inputPersonAttributeFile = inputPersonAttributeFile;
	}

	@StringGetter( NETWORK_ROUTE_TYPE )
	public String getNetworkRouteType() {
		return this.networkRouteType;
	}

	@StringSetter( NETWORK_ROUTE_TYPE )
	public void setNetworkRouteType(final String routeType) {
		this.networkRouteType = routeType;
	}

	@StringGetter( SUBPOPULATION_ATTRIBUTE )
	public String getSubpopulationAttributeName() {
		return subpopulationAttributeName;
	}

	@StringSetter( SUBPOPULATION_ATTRIBUTE )
	public void setSubpopulationAttributeName(String subpopulationAttributeName) {
		this.subpopulationAttributeName = subpopulationAttributeName;
	}
	
	@StringGetter(ACTIVITY_DURATION_INTERPRETATION)
	public PlansConfigGroup.ActivityDurationInterpretation getActivityDurationInterpretation() {
		return this.activityDurationInterpretation ;
	}
//	public void setActivityDurationInterpretation(final String str) {
//		ActivityDurationInterpretation actDurInterpret = ActivityDurationInterpretation.valueOf(str) ;
//		this.setActivityDurationInterpretation(actDurInterpret);
//	}
	@StringSetter(ACTIVITY_DURATION_INTERPRETATION)
	public void setActivityDurationInterpretation( final PlansConfigGroup.ActivityDurationInterpretation actDurInterpret ) {
		if ( PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly.equals(actDurInterpret) ){
			/*
			 * I don't think this is the correct place for consistency checks but this bug is so hard to find that the user should be warned in any case. dg 08-2012
			 */
			Logger.getLogger(this.getClass()).warn("You are using " + actDurInterpret + " as activityDurationInterpretation. " +
			"This is not working in conjunction with the pt module as pt interaction activities then will never end!");
			Logger.getLogger(this.getClass()).warn("ActivityDurationInterpreation " + actDurInterpret + " is deprecated; use " 
					+ PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime + " instead. kai, jan'13") ;
		}
		this.activityDurationInterpretation = actDurInterpret;
	}

	// ---
	
	private static final String REMOVING_UNNECESSARY_PLAN_ATTRIBUTES = "removingUnnecessaryPlanAttributes";
	private boolean removingUnneccessaryPlanAttributes = false;
	@StringGetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public boolean isRemovingUnneccessaryPlanAttributes() {
		return this.removingUnneccessaryPlanAttributes;
	}
	@StringSetter(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES)
	public void setRemovingUnneccessaryPlanAttributes(final boolean removingUnneccessaryPlanAttributes) {
		this.removingUnneccessaryPlanAttributes = removingUnneccessaryPlanAttributes;
	}


	@StringGetter( INPUT_CRS )
	public String getInputCRS() {
		return inputCRS;
	}

	@StringSetter( INPUT_CRS )
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}


}
