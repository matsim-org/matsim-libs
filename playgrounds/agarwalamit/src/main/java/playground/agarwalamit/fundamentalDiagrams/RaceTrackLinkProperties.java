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

    private final double linkLength;
    private final double linkCapacity;
    private final double linkFreeSpeedMPS;
    private final double numberOfLanes;
    private final Set<String> allowedModes;

    private final int subDivisionalFactor; //all sides of the triangle will be divided into subdivisionFactor

    /**
     * A container to use the link properties to generate the triangular network.
     * @param linkLength link length wihout sub-division
     * @param linkCapacity
     * @param linkSpeedMPS
     * @param numberOfLanes
     * @param allowedModes could be same as the "mainModes" in {@link org.matsim.core.config.groups.QSimConfigGroup}
     * @param subDivisionalFactor
     */
    public RaceTrackLinkProperties(final double linkLength,
                                   final double linkCapacity,
                                   final double linkSpeedMPS,
                                   final double numberOfLanes,
                                   final Set<String> allowedModes,
                                   final int subDivisionalFactor ){

        this.linkLength = linkLength;
        this.linkCapacity = linkCapacity;
        this.linkFreeSpeedMPS = linkSpeedMPS;
        this.numberOfLanes = numberOfLanes;
        this.allowedModes = allowedModes;
        this.subDivisionalFactor = subDivisionalFactor;
    }

    /**
     * With this constructor, no sub-division of the links is used.
     * @param linkLength
     * @param linkCapacity
     * @param linkSpeedMPS
     * @param numberOfLanes
     * @param allowedModes
     */
    public RaceTrackLinkProperties(final double linkLength,
                                   final double linkCapacity,
                                   final double linkSpeedMPS,
                                   final double numberOfLanes,
                                   final Set<String> allowedModes
                                   ){
        this(linkLength, linkCapacity, linkSpeedMPS, numberOfLanes, allowedModes, 1);
    }

    public double getLinkLength() {
        return linkLength;
    }

    public double getLinkCapacity() {
        return linkCapacity;
    }

    public double getLinkFreeSpeedMPS() {
        return linkFreeSpeedMPS;
    }

    public double getNumberOfLanes() {
        return numberOfLanes;
    }

    public Set<String> getAllowedModes() {
        return allowedModes;
    }

    public int getSubDivisionalFactor() {
        return subDivisionalFactor;
    }
}
