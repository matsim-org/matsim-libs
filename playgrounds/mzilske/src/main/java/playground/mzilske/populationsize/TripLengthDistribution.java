/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TripLengthDistribution.java
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

import java.util.Map;

public class TripLengthDistribution {
    private Map<String, int[]> distribution;

    public TripLengthDistribution(Map<String, int[]> legStats) {
        this.distribution = legStats;
    }

    public Map<String, int[]> getDistribution() {
        return distribution;
    }

    public void setDistribution(Map<String, int[]> distribution) {
        this.distribution = distribution;
    }
}
