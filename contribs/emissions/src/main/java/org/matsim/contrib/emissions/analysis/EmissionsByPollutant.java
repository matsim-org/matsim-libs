/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions.analysis;


import org.matsim.contrib.emissions.Pollutant;

import java.util.Map;

/**
 * Sums up emissions by pollutant. Basically wraps a hash map but is here for better
 * readability of {@link org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler}
 */
public class EmissionsByPollutant {
    // The EmissionsByPollutant potentially adds up the same emissions coming from cold and warm.  Thus, this cannot be combined into the enum approach
    // without some thinking.  kai, jan'20
    // yyyy todo I think that this now can be done.  kai, jan'20

    private final Map<Pollutant, Double> emissionByPollutant;

    public EmissionsByPollutant(Map<Pollutant, Double> emissions) {
        this.emissionByPollutant = emissions;
    }

    public void addEmissions( Map<Pollutant, Double> emissions ) {
        emissions.forEach(this::addEmission);
    }

    public double addEmission(Pollutant pollutant, double value) {
        return emissionByPollutant.merge(pollutant, value, Double::sum);
    }

    public Map<Pollutant, Double> getEmissions() {
        return emissionByPollutant;
    }

    public double getEmission(Pollutant pollutant) {
        return emissionByPollutant.get(pollutant);
    }


}
