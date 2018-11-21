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

package org.matsim.contrib.drt.routing;/*
 * created by jbischoff, 20.11.2018
 */

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiModeDrtMainModeIdentifier implements MainModeIdentifier {

    private final MainModeIdentifier delegate = new MainModeIdentifierImpl();
    private final List<String> modes;
    private final Map<String, String> drtStageActivityTypes;
    private final Map<String, String> drtWalkTypes;

    @Inject
    public MultiModeDrtMainModeIdentifier(MultiModeDrtConfigGroup drtCfg) {
        modes = drtCfg.getDrtConfigGroups().stream().map(c -> c.getMode()).collect(Collectors.toList());
        drtStageActivityTypes = drtCfg.getDrtConfigGroups().stream().map(drtConfigGroup -> drtConfigGroup.getMode()).collect(Collectors.toMap(s -> new DrtStageActivityType(s).drtStageActivity, s -> s));
        drtWalkTypes = drtCfg.getDrtConfigGroups().stream().map(drtConfigGroup -> drtConfigGroup.getMode()).collect(Collectors.toMap(s -> new DrtStageActivityType(s).drtWalk, s -> s));
    }

    @Override
    public String identifyMainMode(List<? extends PlanElement> tripElements) {
        for (PlanElement pe : tripElements) {
            if (pe instanceof Activity) {
                Activity a = (Activity) pe;
                if (drtStageActivityTypes.containsKey(a.getType())) {
                    return drtStageActivityTypes.get(a.getType());
                }
            } else if (pe instanceof Leg) {
                Leg l = (Leg) pe;
                if (drtWalkTypes.containsKey(l.getMode())) {
                    return drtWalkTypes.get(l.getMode());
                }
            }
        }
        return delegate.identifyMainMode(tripElements);
    }
}
