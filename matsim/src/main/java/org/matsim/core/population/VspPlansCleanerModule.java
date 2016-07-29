/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * VspPlansCleanerModule.java
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

package org.matsim.core.population;

import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class VspPlansCleanerModule extends AbstractModule {
    @Override
    public void install() {
        PlansConfigGroup.ActivityDurationInterpretation actDurInterpr = getConfig().plans().getActivityDurationInterpretation() ;
        if ( actDurInterpr != PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime
                || getConfig().plans().isRemovingUnneccessaryPlanAttributes() ) {
            addControlerListenerBinding().to(VspPlansCleaner.class);
        }
    }
}
