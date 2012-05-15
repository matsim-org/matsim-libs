/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCapacityAttribute.java
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

package facilities;

//import input.FacilitiesCreation;
import input.FacilitiesProduction;

import java.io.IOException;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.core.scenario.ScenarioImpl;
//import org.apache.log4j.Logger;

public class CreateFacilityAttributes {
	

	//private static Logger log = Logger.getLogger(CreateFacilityAttributes.class);
	
	public static final String LOWERBOUND = "LowerThreshold";
	public static final String UPPERBOUND = "UpperThreshold";
	public static final String LOWERMARGINALUTILITY = "MarginalUtilityOfUnderArousal";
	public static final String UPPERMARGINALUTILITY = "MarginalUtilityOfOverArousal";
	
	public static void main(String[] args) throws IOException {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
		ObjectAttributes facilityAttributes = new ObjectAttributes();
		
		for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
			if (f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GT2500) || f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET1000) || f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET400) || f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET100) || f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_LT100) || f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_OTHER)) {
				facilityAttributes.putAttribute(f.getId().toString(),LOWERBOUND,0.1);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERBOUND,0.75);
				facilityAttributes.putAttribute(f.getId().toString(),LOWERMARGINALUTILITY,-12);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERMARGINALUTILITY,-12);
				//log.info("parameters for shop retail facility created");
			}
			else if (f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_SERVICE)){
				facilityAttributes.putAttribute(f.getId().toString(),LOWERBOUND,0.1);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERBOUND,0.9);
				facilityAttributes.putAttribute(f.getId().toString(),LOWERMARGINALUTILITY,-12);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERMARGINALUTILITY,-6);
				//log.info("parameters for shop service facility created");
			}
			else if (f.getActivityOptions().containsKey(FacilitiesProduction.SPORTS_FUN)){
				facilityAttributes.putAttribute(f.getId().toString(),LOWERBOUND,0.2);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERBOUND,1.0);
				facilityAttributes.putAttribute(f.getId().toString(),LOWERMARGINALUTILITY,-12);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERMARGINALUTILITY,-18);
				//log.info("parameters for sports & fun facility created");

			}
			else if (f.getActivityOptions().containsKey(FacilitiesProduction.GASTRO_CULTURE)){
				facilityAttributes.putAttribute(f.getId().toString(),LOWERBOUND,0.1);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERBOUND,0.9);
				facilityAttributes.putAttribute(f.getId().toString(),LOWERMARGINALUTILITY,-12);
				facilityAttributes.putAttribute(f.getId().toString(),UPPERMARGINALUTILITY,-12);
				//log.info("parameters for gastro & culture facility created");
			}
		}
		new ObjectAttributesXmlWriter(facilityAttributes).writeFile("./input/facilityAttributes.xml");
	}
	

	public String getLowerBound() {
		return LOWERBOUND;
	}
	
	public String getUpperBound() {
		return UPPERBOUND;
	}
	
	public String getLowerMarginalUtility() {
		return LOWERMARGINALUTILITY;
	}
	
	public String getUpperMarginalUtility() {
		return UPPERMARGINALUTILITY;
	}
}
