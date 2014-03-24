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

package org.matsim.contrib.dvrp.router;

import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Predicate;


public class ConstrainedVrpPathCalculator
    implements VrpPathCalculator
{
    VrpPathCalculator createVrpPathCalculatorWithTravelCostLimit(VrpPathCalculator calculator,
            final double travelCostLimit)
    {
        return new ConstrainedVrpPathCalculator(calculator, new Predicate<VrpPathWithTravelData>() {
            public boolean apply(VrpPathWithTravelData path)
            {
                return path.getTravelCost() <= travelCostLimit;
            }
        });
    }


    VrpPathCalculator createVrpPathCalculatorWithTravelTimeLimit(VrpPathCalculator calculator,
            final double travelTimeLimit)
    {
        return new ConstrainedVrpPathCalculator(calculator, new Predicate<VrpPathWithTravelData>() {
            public boolean apply(VrpPathWithTravelData path)
            {
                return path.getTravelTime() <= travelTimeLimit;
            }
        });
    }


    private final VrpPathCalculator calculator;
    private final Predicate<VrpPathWithTravelData> constraintCheck;


    public ConstrainedVrpPathCalculator(VrpPathCalculator calculator,
            Predicate<VrpPathWithTravelData> constraintCheck)
    {
        this.calculator = calculator;
        this.constraintCheck = constraintCheck;
    }


    @Override
    public VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime)
    {
        VrpPathWithTravelData path = calculator.calcPath(fromLink, toLink, departureTime);
        return constraintCheck.apply(path) ? path : null;
    }
}
