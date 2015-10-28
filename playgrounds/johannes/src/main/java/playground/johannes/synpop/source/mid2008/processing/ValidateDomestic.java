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

package playground.johannes.synpop.source.mid2008.processing;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

/**
 * @author johannes
 */
public class ValidateDomestic implements EpisodeTask {

    @Override
    public void apply(Episode episode) {
        for(Segment leg : episode.getLegs()) {
            if(!MiDValues.DOMESTIC.equalsIgnoreCase(leg.getAttribute(MiDKeys.LEG_DESTINATION))) {
                episode.setAttribute(CommonKeys.DELETE, CommonValues.TRUE);
            }
        }
    }
}
