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

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.Map;

public final class PlansConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "plans";

	public abstract static class NetworkRouteType {
		public static final String LinkNetworkRoute = "LinkNetworkRoute";
		public static final String CompressedNetworkRoute = "CompressedNetworkRoute";
	}

	public enum ActivityDurationInterpretation { minOfDurationAndEndTime, tryEndTimeThenDuration, @Deprecated endTimeOnly }
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
//		comments.put(
//				INPUT_PERSON_ATTRIBUTES_FILE,
//				"Path to a file containing person attributes (required file format: ObjectAttributes).");
		comments.put(
				SUBPOPULATION_ATTRIBUTE,
				"Name of the (Object)Attribute defining the subpopulation to which pertains a Person"+
				" (as freight, through traffic, etc.). The attribute must be of String type.  Change away from default only in desperate situations." );

		StringBuilder str = new StringBuilder() ;
		for ( PlansConfigGroup.ActivityDurationInterpretation itp : PlansConfigGroup.ActivityDurationInterpretation.values() ) {
			str.append(" ").append(itp.toString());
		}
		comments.put(ACTIVITY_DURATION_INTERPRETATION, "String:" + str + ". Anything besides " 
				+ PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime + " will internally use a different " +
		"(simpler) version of the TimeAllocationMutator.") ;
		
		comments.put(REMOVING_UNNECESSARY_PLAN_ATTRIBUTES, "(not tested) will remove plan attributes that are presumably not used, such as " +
                "activityStartTime. default=false. Use with Caution!");

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

	public URL getInputFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.inputFile);
	}
	// ---
	private boolean insistingOnUsingDeprecatedPersonAttributeFile = false ;
	private static final String INSISTING_ON_USING_DEPRECATED_PERSON_ATTRIBUTE_FILE = "insistingOnUsingDeprecatedPersonAttributeFile" ;
	@StringSetter(INSISTING_ON_USING_DEPRECATED_PERSON_ATTRIBUTE_FILE)
	public final void setInsistingOnUsingDeprecatedPersonAttributeFile( boolean val ) {
		this.insistingOnUsingDeprecatedPersonAttributeFile = val ;
	}
	@StringGetter(INSISTING_ON_USING_DEPRECATED_PERSON_ATTRIBUTE_FILE)
	public final boolean isInsistingOnUsingDeprecatedPersonAttributeFile() {
		return insistingOnUsingDeprecatedPersonAttributeFile;
	}
	
	public static enum HandlingOfPlansWithoutRoutingMode { reject, useMainModeIdentifier };
	private HandlingOfPlansWithoutRoutingMode handlingOfPlansWithoutRoutingMode = HandlingOfPlansWithoutRoutingMode.reject ;
	private static final String HANDLING_OF_PLANS_WITHOUT_ROUTING_MODE = "handlingOfPlansWithoutRoutingMode" ;
	@StringSetter(HANDLING_OF_PLANS_WITHOUT_ROUTING_MODE)
	public final void setHandlingOfPlansWithoutRoutingMode( HandlingOfPlansWithoutRoutingMode val ) {
		this.handlingOfPlansWithoutRoutingMode = val ;
	}
	@StringGetter(HANDLING_OF_PLANS_WITHOUT_ROUTING_MODE)
	public final HandlingOfPlansWithoutRoutingMode getHandlingOfPlansWithoutRoutingMode() {
		return handlingOfPlansWithoutRoutingMode;
	}
	
	@StringGetter( INPUT_PERSON_ATTRIBUTES_FILE )
	@Deprecated // I think that this should be phased out; use Attributes inside each facility.  kai, mar'19
	public String getInputPersonAttributeFile() {
		return this.inputPersonAttributeFile;
	}
	public static final String PERSON_ATTRIBUTES_DEPRECATION_MESSAGE = "using the separate person attribute file is deprecated.  Add the information directly into each person, using " +
						 "the Attributable feature.  If you insist on continuing to use the separate person attribute file, set " +
						 "insistingOnUsingDeprecatedPersonAttributeFile to true.  The file will then be read, but the values " +
						 "will be entered into each person using Attributable, and written as such to output_plans.  kai, may'19";
	@StringSetter( INPUT_PERSON_ATTRIBUTES_FILE )
	@Deprecated // I think that this should be phased out; use Attributes inside each facility.  kai, mar'19
	public void setInputPersonAttributeFile(final String inputPersonAttributeFile) {
		this.inputPersonAttributeFile = inputPersonAttributeFile;
	}

	@Deprecated // I think that this should be phased out; use Attributes inside each facility.  kai, mar'19
	public URL getInputPersonAttributeFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.inputPersonAttributeFile);
	}
	// ---
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
