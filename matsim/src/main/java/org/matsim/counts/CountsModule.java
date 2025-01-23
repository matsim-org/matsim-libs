/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CountSimComparisonModule.java
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

package org.matsim.counts;

import com.google.inject.Provides;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class CountsModule extends AbstractModule {
    private static final Logger log = LogManager.getLogger( CountsModule.class );

    @Override
    public void install() {
        addControlerListenerBinding().to(CountsControlerListener.class);
        bind(CountsInitializer.class).asEagerSingleton();
    }

    private static class CountsInitializer {
        @Inject
        CountsInitializer(Counts<Link> counts, Scenario scenario) {
            Counts<Link> scenarioCounts = (Counts<Link>) scenario.getScenarioElement(Counts.ELEMENT_NAME);
            if (scenarioCounts == null) {
                scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
            } else {
                if (counts != scenarioCounts) {
                    throw new RuntimeException();
                }
            }
        }
    }

    @Provides
    @Singleton
    Counts<Link> provideLinkCounts(Scenario scenario, CountsConfigGroup config) {
        Counts<Link> counts = (Counts<Link>) scenario.getScenarioElement(Counts.ELEMENT_NAME);
        if (counts != null) {
            return counts;
        } else {
            counts = new Counts<>();
            if (config.getCountsFileName() != null) {
                final String inputCRS = config.getInputCRS();
                final String internalCRS = scenario.getConfig().global().getCoordinateSystem();

                MatsimCountsReader counts_parser;
                if (inputCRS == null) {
                    counts_parser = new MatsimCountsReader(counts);
                }
                else {
                    log.info( "re-projecting counts from "+inputCRS+" to "+internalCRS+" for import" );

                    counts_parser = new MatsimCountsReader( inputCRS, internalCRS, counts );
                }
                counts_parser.parse(config.getCountsFileURL(scenario.getConfig().getContext()));
            }
            return counts;
        }
    }

}
