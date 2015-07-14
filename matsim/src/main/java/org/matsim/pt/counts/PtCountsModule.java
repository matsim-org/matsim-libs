/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PtCountsModule.java
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

package org.matsim.pt.counts;

import org.matsim.core.controler.AbstractModule;

public class PtCountsModule extends AbstractModule {
    @Override
    public void install() {
        if (getConfig().transit().isUseTransit()) {
            if (getConfig().ptCounts().getAlightCountsFileName() != null) {
                // only works when all three files are defined! kai, oct'10
                addControlerListenerBinding().toInstance(new PtCountControlerListener(getConfig()));
            }
        }
    }
}
