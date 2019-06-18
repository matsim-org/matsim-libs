/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic.scoring;/*
 * created by jbischoff, 18.06.2019
 */

public class DefaultCommercialServiceScore implements DeliveryScoreCalculator {

    final double maxPerformedScore;
    final double minPerformedScore;
    final double negativeScoreThreshold;

    public DefaultCommercialServiceScore(double maxPerformedScore, double minPerformedScore, double negativeScoreThreshold) {
        this.maxPerformedScore = maxPerformedScore;
        this.minPerformedScore = minPerformedScore;
        this.negativeScoreThreshold = negativeScoreThreshold;
    }

    @Override
    public double calcScore(double timeDifference) {
        double a = (-maxPerformedScore) / negativeScoreThreshold;
        return Math.max(minPerformedScore, maxPerformedScore + a * Math.abs(timeDifference));
    }
}
