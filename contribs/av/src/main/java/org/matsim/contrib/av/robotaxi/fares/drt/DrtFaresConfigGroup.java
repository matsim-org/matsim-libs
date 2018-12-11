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

package org.matsim.contrib.av.robotaxi.fares.drt;/*
 * created by jbischoff, 11.12.2018
 */

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;

public class DrtFaresConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "drtfares";

    public DrtFaresConfigGroup() {
        super(GROUP_NAME);
    }

    public static DrtFaresConfigGroup get(Config config) {
        return (DrtFaresConfigGroup) config.getModules().get(GROUP_NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        if (type.equals(DrtFareConfigGroup.GROUP_NAME)) {
            return new DrtFareConfigGroup();
        }
        throw new IllegalArgumentException(type);
    }

    @SuppressWarnings("unchecked")
    public Collection<DrtFareConfigGroup> getTaxiFareConfigGroups() {
        return (Collection<DrtFareConfigGroup>) getParameterSets(DrtFareConfigGroup.GROUP_NAME);
    }
}
