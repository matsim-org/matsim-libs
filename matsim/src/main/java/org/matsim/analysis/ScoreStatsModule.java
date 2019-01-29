/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ScoreStatsModule.java
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

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class ScoreStatsModule extends AbstractModule {
    @Override
    public void install() {
        bind(ScoreStatsControlerListener.class).in(Singleton.class);
        addControlerListenerBinding().to(ScoreStatsControlerListener.class);
        bind(ScoreStats.class).to(ScoreStatsControlerListener.class);
    }
}
