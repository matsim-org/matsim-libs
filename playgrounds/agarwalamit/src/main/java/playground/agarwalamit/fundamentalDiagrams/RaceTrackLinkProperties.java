/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams;

import java.util.Set;

/**
 * Created by amit on 17/02/2017.
 *
 * Stores the link attributes for an equilateral triangular network.
 */

public final class RaceTrackLinkProperties {

    public double getLinkLength() {
        return linkLength;
    }

    public double getLinkCapacity() {
        return linkCapacity;
    }

    public double getFreespeedMPS() {
        return linkSpeedMPS;
    }

    public double getNumberOfLanes() {
        return numberOfLanes;
    }

    public Set<String> getAllowedModes() {
        return allowedModes;
    }

    private final double linkLength;
    private final double linkCapacity;
    private final double linkSpeedMPS;
    private final double numberOfLanes;
    private final Set<String> allowedModes;

    public RaceTrackLinkProperties (final double linkLength,
                                    final double linkCapacity,
                                    final double linkSpeedMPS,
                                    final double numberOfLanes,
                                    final Set<String> allowedModes){

        this.linkLength = linkLength;
        this.linkCapacity = linkCapacity;
        this.linkSpeedMPS = linkSpeedMPS;
        this.numberOfLanes = numberOfLanes;
        this.allowedModes = allowedModes;
    }
}
