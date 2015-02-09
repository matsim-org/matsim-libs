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

package playground.artemc.pricing;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

import javax.inject.Inject;
import javax.inject.Provider;

public class LinkOccupancyAnalyzerModule extends AbstractModule {
    @Override
    public void install() {
        bindToProviderAsSingleton(LinkOccupancyAnalyzer.class, LinkOccupancyAnalyzerProvider.class);
        addEventHandler(LinkOccupancyAnalyzer.class);
        addControlerListener(LinkOccupancyAnalyzer.class);
    }

    static class LinkOccupancyAnalyzerProvider implements Provider<LinkOccupancyAnalyzer> {

        @Inject
        Scenario scenario;

        @Override
        public LinkOccupancyAnalyzer get() {
            return new LinkOccupancyAnalyzer(scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), 30 * 3600 - 1, scenario.getNetwork());
        }
    }

}
