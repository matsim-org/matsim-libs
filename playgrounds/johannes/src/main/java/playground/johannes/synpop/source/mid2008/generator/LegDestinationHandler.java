/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.Map;

/**
 * @author johannes
 */
public class LegDestinationHandler implements LegAttributeHandler {

    @Override
    public void handle(Segment leg, Map<String, String> attributes) {
        String val = attributes.get(VariableNames.LEG_DESTINATION);

        if(val.equalsIgnoreCase("1")) leg.setAttribute(MiDKeys.LEG_DESTINATION, ActivityType.HOME);
        else if(val.equalsIgnoreCase("2")) leg.setAttribute(MiDKeys.LEG_DESTINATION, ActivityType.WORK);
        else if(val.equalsIgnoreCase("3")) leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.IN_TOWN);
        else if(val.equalsIgnoreCase("4")) leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.OUT_OF_TOWN);
        else if(val.equalsIgnoreCase("5")) leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.ROUND_TRIP);
    }
}
