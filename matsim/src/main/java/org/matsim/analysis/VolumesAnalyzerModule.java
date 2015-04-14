/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * VolumesAnalyzerModule.java
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

import javax.inject.Inject;
import javax.inject.Provider;

public class VolumesAnalyzerModule extends AbstractModule {
    @Override
    public void install() {
        bind(VolumesAnalyzer.class).toProvider(VolumesAnalyzerProvider.class).in(Singleton.class);
        addEventHandlerBinding().to(VolumesAnalyzer.class);
    }

    static class VolumesAnalyzerProvider implements Provider<VolumesAnalyzer> {

        @Inject
        Scenario scenario;

        @Override
        public VolumesAnalyzer get() {
            return new VolumesAnalyzer(3600, 24 * 3600 - 1, scenario.getNetwork());
        }
    }

}
