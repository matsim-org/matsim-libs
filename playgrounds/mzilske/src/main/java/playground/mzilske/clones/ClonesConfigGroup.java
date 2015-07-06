/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ClonesConfigGroup.java
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

package playground.mzilske.clones;

import org.matsim.core.config.ReflectiveConfigGroup;

public class ClonesConfigGroup extends ReflectiveConfigGroup {

    public static final String NAME = "clones";

    public double getCloneFactor() {
        return cloneFactor;
    }

    public void setCloneFactor(double cloneFactor) {
        this.cloneFactor = cloneFactor;
    }

    private double cloneFactor = 1.0;

    public ClonesConfigGroup() {
        super(NAME);
    }
}
