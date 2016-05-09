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

package playground.singapore.springcalibration.run.roadpricing;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import com.google.inject.Singleton;

import playground.singapore.springcalibration.run.SubpopTravelDisutilityFactory;

import org.matsim.roadpricing.CalcAverageTolledTripLength;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

public class SubpopRoadPricingModule extends AbstractModule {

    private final RoadPricingScheme roadPricingScheme;
    private static final Logger log = Logger.getLogger( SubpopRoadPricingModule.class ) ;
    private Config config;

    public SubpopRoadPricingModule(Scenario scenario, Config config) {
        this.roadPricingScheme = null;
        this.config = config ;
    }

    public SubpopRoadPricingModule(RoadPricingScheme roadPricingScheme) {
        this.roadPricingScheme = roadPricingScheme;      
    }

    @Override
    public void install() {
    	log.info("installing SubpopRoadPricingModule");
        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
        // This is not optimal yet. Modules should not need to have parameters.
        // But I am not quite sure yet how to best handle custom scenario elements. mz
        if (this.roadPricingScheme != null) {
            bind(RoadPricingScheme.class).toInstance(this.roadPricingScheme);
        } else {
            bind(RoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
        }
        bind(RoadPricingInitializer.class).asEagerSingleton();
                                        
        // use ControlerDefaults configuration, replacing the TravelDisutility with a toll-dependent one
        install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new AbstractModule() {
        //install(new AbstractModule() {
            @Override
            public void install() {
            //	addTravelDisutilityFactoryBinding(TransportMode.car).toProvider(
            //			new TravelDisutilityIncludingTollFactoryProvider(scenario, roadPricingScheme, parameters, config));
            	
            	addTravelDisutilityFactoryBinding(TransportMode.car).toProvider(TravelDisutilityIncludingTollFactoryProvider.class);
            	addTravelDisutilityFactoryBinding("freight").toProvider(TravelDisutilityIncludingTollFactoryProvider.class);
            	// we do not need taxi here as the customer does pay a fare not a toll!
            }
        }));

        log.info("Adding SingaporeRoadPricingControlerListener");
        addControlerListenerBinding().to(SingaporeRoadPricingControlerListener.class);

        // add the events handler to calculate the tolls paid by agents
        bind(CalcPaidToll.class).in(Singleton.class);
        addEventHandlerBinding().to(CalcPaidToll.class);

        bind(CalcAverageTolledTripLength.class).in(Singleton.class);
        addEventHandlerBinding().to(CalcAverageTolledTripLength.class);
    }

    private static class RoadPricingInitializer {
        @Inject
        RoadPricingInitializer(RoadPricingScheme roadPricingScheme, Scenario scenario) {
            RoadPricingScheme scenarioRoadPricingScheme = (RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
            if (scenarioRoadPricingScheme == null) {
                scenario.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, roadPricingScheme);
            } else {
                if (roadPricingScheme != scenarioRoadPricingScheme) {
                    throw new RuntimeException();
                }
            }
        }
    }


    private static class RoadPricingSchemeProvider implements Provider<RoadPricingScheme> {

        private final Config config;
        private Scenario scenario;

        @Inject
        RoadPricingSchemeProvider(Config config, Scenario scenario) {
            this.config = config;
            this.scenario = scenario;
        }

        @Override
        public RoadPricingScheme get() {
            RoadPricingScheme scenarioRoadPricingScheme = (RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
            if (scenarioRoadPricingScheme != null) {
                return scenarioRoadPricingScheme;
            } else {
                RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
                String tollLinksFile = rpConfig.getTollLinksFile();
                if ( tollLinksFile == null ) {
                    throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
                            + "Such an execution path is not allowed.  If you want a base case without toll, "
                            + "construct a zero toll file and insert that. ") ;
                }
                RoadPricingSchemeImpl rpsImpl = new RoadPricingSchemeImpl() ;
                new RoadPricingReaderXMLv1(rpsImpl).parse(tollLinksFile);
                return rpsImpl;
            }
        }
    } 
    
    private static class TravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {

        private final RoadPricingScheme scheme;
        private final CharyparNagelScoringParametersForPerson parameters;
        private final Config config;
        
        @Inject
        TravelDisutilityIncludingTollFactoryProvider(RoadPricingScheme scheme, CharyparNagelScoringParametersForPerson parameters, Config config) {
            this.scheme = scheme;
            this.parameters = parameters;
            this.config = config;
        }

        @Override
        public TravelDisutilityFactory get() {
        	
            RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, 
            		RoadPricingConfigGroup.GROUP_NAME, 
            		RoadPricingConfigGroup.class);
            
            final TravelDisutilityFactory originalTravelDisutilityFactory = new SubpopTravelDisutilityFactory(parameters, TransportMode.car);
            log.info("getting TravelDisutilityFactory");
            
            SubpopRoadPricingTravelDisutilityFactory travelDisutilityFactory = new SubpopRoadPricingTravelDisutilityFactory(
                    originalTravelDisutilityFactory, scheme, parameters);
            travelDisutilityFactory.setSigma(rpConfig.getRoutingRandomness());
            return travelDisutilityFactory;
        }
    }
}
