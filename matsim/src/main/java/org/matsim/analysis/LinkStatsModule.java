/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LinkStatsModules.java
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

package org.matsim.analysis;

import org.matsim.core.controler.AbstractModule;

public final class LinkStatsModule extends AbstractModule {

    @Override
    public void install() {
        bind(CalcLinkStats.class).asEagerSingleton();
        if (getConfig().linkStats().getWriteLinkStatsInterval() > 0) {
            // "Do not use this, as it may not contain values in every iteration."
            // says the original comment on the getter in the Controler.
            // I assume this is still true.
            /*TODO [MR] linkStats uses ttcalc and volumes, but ttcalc has
		    15min-steps, while volumes uses 60min-steps! It works a.t.m., but the
		    traveltimes in linkStats are the avg. traveltimes between xx.00 and
		    xx.15, and not between xx.00 and xx.59*/
            bind(LinkStatsControlerListener.class).asEagerSingleton();
            addControlerListenerBinding().to(LinkStatsControlerListener.class);
        }
    }

}
