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

package playground.johannes.synpop.source.mid2008.processing;

import playground.johannes.synpop.data.*;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 */
public class ResolveRoundTripsTask implements EpisodeTask {

    private final Factory factory;

    public ResolveRoundTripsTask(Factory factory) {
        this.factory = factory;
    }

    @Override
    public void apply(Episode episode) {
        List<Integer> insertPoints = new ArrayList<Integer>();

        for (int i = 0; i < episode.getLegs().size(); i++) {
            Attributable leg = episode.getLegs().get(i);
            String dest = leg.getAttribute(MiDKeys.LEG_DESTINATION);
            if (MiDValues.ROUND_TRIP.equals(dest)) {
                insertPoints.add(i + 2);
            }
        }

        int offset = 0;
        for (Integer idx : insertPoints) {
            int i = idx + offset;

            Attributable toLeg = episode.getLegs().get(i - 2);
            int toLegStart = Integer.parseInt(toLeg.getAttribute(CommonKeys.LEG_START_TIME));
            int toLegEnd = Integer.parseInt(toLeg.getAttribute(CommonKeys.LEG_END_TIME));
            int dur = toLegEnd - toLegStart;
            /*
			 * half the leg duration and distance
			 */
            toLeg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(toLegStart + dur / 2 - 1));
            String distStr = toLeg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
            if (distStr != null) {
                double dist = Double.parseDouble(distStr);
                toLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(dist / 2.0));
            }
			/*
			 * insert a dummy activity with duration 1 s.
			 */
            Segment act = factory.newSegment();
            String prevType = episode.getActivities().get(i - 2).getAttribute(CommonKeys.ACTIVITY_TYPE);
            act.setAttribute(CommonKeys.ACTIVITY_TYPE, prevType);
            episode.insertActivity(act, i);
			/*
			 * insert a return leg with half the duration and distance
			 */
            Segment fromLeg = factory.newSegment();
            fromLeg.setAttribute(CommonKeys.LEG_START_TIME, String.valueOf(toLegStart + dur / 2));
            fromLeg.setAttribute(CommonKeys.LEG_END_TIME, String.valueOf(toLegEnd));
            fromLeg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, toLeg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE));
            fromLeg.setAttribute(CommonKeys.LEG_MODE, toLeg.getAttribute(CommonKeys.LEG_MODE));

            Attributable nextAct = episode.getActivities().get(i);
            fromLeg.setAttribute(CommonKeys.LEG_PURPOSE, nextAct.getAttribute(CommonKeys.ACTIVITY_TYPE));
            episode.insertLeg(fromLeg, i - 1);

            offset += 1;
        }

    }

}
