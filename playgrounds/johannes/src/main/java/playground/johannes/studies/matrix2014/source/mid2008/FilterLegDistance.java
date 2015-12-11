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

package playground.johannes.studies.matrix2014.source.mid2008;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 */
public class FilterLegDistance implements EpisodeTask {

    @Override
    public void apply(Episode episode) {
        for(Segment leg : episode.getLegs()) {
            String value = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
            if(value != null) {
                double d = Double.parseDouble(value);
                if(d > 400000) {
                    episode.setAttribute(CommonKeys.DELETE, CommonValues.TRUE);
                    break;
                }
            }
        }
    }
}
