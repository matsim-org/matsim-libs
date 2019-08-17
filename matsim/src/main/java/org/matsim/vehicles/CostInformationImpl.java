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

import org.matsim.utils.objectattributes.attributable.Attributes;

public class CostInformationImpl implements CostInformation {
    private final Double fixed;
    private final Double perMeter;
    private final Double perSecond;
    private Attributes attributes = new Attributes() ;

    public CostInformationImpl(Double fixed, Double perMeter, Double perTimeUnit) {
        this.fixed = fixed;
        this.perMeter = perMeter;
        this.perSecond = perTimeUnit;
    }

    @Override
    public Double getFixedCosts() {
        return fixed;
    }

    @Override
    public Double getCostsPerMeter() {
        return perMeter;
    }

    @Override
    public Double getCostsPerSecond() {
        return perSecond;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }
}
