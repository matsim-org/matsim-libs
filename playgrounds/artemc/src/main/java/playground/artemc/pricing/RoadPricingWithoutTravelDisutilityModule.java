/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RoadPricingModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.roadpricing.*;

import javax.inject.Inject;
import javax.inject.Provider;

public class RoadPricingWithoutTravelDisutilityModule extends AbstractModule {

    private final RoadPricingScheme roadPricingScheme;

    public RoadPricingWithoutTravelDisutilityModule() {
        this.roadPricingScheme = null;
    }

    public RoadPricingWithoutTravelDisutilityModule(RoadPricingScheme roadPricingScheme) {
        this.roadPricingScheme = roadPricingScheme;
    }

    @Override
    public void install() {
        // This is not optimal yet. Modules should not need to have parameters.
        // But I am not quite sure yet how to best handle custom scenario elements. mz
        if (this.roadPricingScheme != null) {
            bind(RoadPricingScheme.class).toInstance(this.roadPricingScheme);
        } else {
            bind(RoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
        }

        addControlerListenerBinding().to(RoadPricingControlerListener.class);

        // add the events handler to calculate the tolls paid by agents
        bind(CalcPaidToll.class).in(Singleton.class);

        bind(CalcAverageTolledTripLength.class).in(Singleton.class);
    }

    private static class RoadPricingSchemeProvider implements Provider<RoadPricingScheme> {

        private final Config config;

        @Inject
        RoadPricingSchemeProvider(Config config) {
            this.config = config;
        }

        @Override
        public RoadPricingScheme get() {
            RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
            String tollLinksFile = rpConfig.getTollLinksFile();
            if ( tollLinksFile == null ) {
                throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
                        + "Such an execution path is not allowed.  If you want a base case without toll, "
                        + "construct a zero toll file and insert that. ") ;
            }
            RoadPricingSchemeImpl rpsImpl = new RoadPricingSchemeImpl() ;
            new RoadPricingReaderXMLv1(rpsImpl).readFile(tollLinksFile);
            return rpsImpl;
        }
    }
}
