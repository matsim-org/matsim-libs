/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.mid2008.generator;

import java.util.Map;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 *
 */
public class LegPurposeHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.LEG_MAIN_TYPE);

		if(val.equalsIgnoreCase("1")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.WORK);
			
		} else if(val.equalsIgnoreCase("2")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.BUSINESS);
			
		} else if(val.equalsIgnoreCase("3")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else if(val.equalsIgnoreCase("4")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.SHOP);
			
		} else if(val.equalsIgnoreCase("5")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.PRIVATE);
			
		} else if(val.equalsIgnoreCase("6")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.PICKDROP);
			
		} else if(val.equalsIgnoreCase("7")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.LEISURE);
			
			String subtype = attributes.get(VariableNames.LEG_SUB_TYPE);
			if(subtype != null) {
				if(subtype.equalsIgnoreCase("701")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.VISIT);
				} else if(subtype.equalsIgnoreCase("702")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.CULTURE);
				} else if(subtype.equalsIgnoreCase("703")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.CULTURE);
				} else if(subtype.equalsIgnoreCase("701")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.SPORT);
				} else if(subtype.equalsIgnoreCase("706")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.GASTRO);
				} else if(subtype.equalsIgnoreCase("708")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.VACATIONS_SHORT);
				} else if(subtype.equalsIgnoreCase("709")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.VACATIONS_LONG);
				}
			}
			
		} else if(val.equalsIgnoreCase("8")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.HOME);
			
		} else if(val.equalsIgnoreCase("31")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else if(val.equalsIgnoreCase("32")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.MISC);
		}
		
	}

}
