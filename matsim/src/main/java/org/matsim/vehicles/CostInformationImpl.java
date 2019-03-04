/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

public class CostInformationImpl implements CostInformation {
    private final double fixed ;
    private final double perMeter;
    private final double perSecond;

    public CostInformationImpl(double fixed, double perMeter, double perTimeUnit) {
        this.fixed = fixed;
        this.perMeter = perMeter;
        this.perSecond = perTimeUnit;
    }

    @Override
    public double getFixedCosts() {
        return fixed;
    }

    @Override
    public double getCostsPerMeter() {
        return perMeter;
    }

    @Override
    public double getCostsPerSecond() {
        return perSecond;
    }

}
