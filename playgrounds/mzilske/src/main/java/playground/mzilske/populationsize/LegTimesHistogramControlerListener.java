/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LegTimesHistogramControlerListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import com.google.inject.Inject;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.HashMap;
import java.util.Map;

public class LegTimesHistogramControlerListener implements IterationEndsListener {

    @Inject
    CalcLegTimes calcLegTimes;

    @Inject
    Scenario scenario;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        Map<String, int[]> copy = new HashMap<>();
        Map<String, int[]> legStats = calcLegTimes.getLegStats();
        for (Map.Entry<String, int[]> e : legStats.entrySet()) {
            int[] c = new int[e.getValue().length];
            System.arraycopy(e.getValue(), 0, c, 0, e.getValue().length);
            copy.put(e.getKey(), c);
        }
        ((TripLengthDistribution) scenario.getScenarioElement("actualTripLengthDistribution")).setDistribution(copy);
    }
}
