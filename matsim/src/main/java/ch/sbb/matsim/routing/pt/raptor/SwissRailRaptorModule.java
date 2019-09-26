/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModule;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorModule extends AbstractModule {

    @Override
    public void install() {
        if (getConfig().transit().isUseTransit()) {
            bind(SwissRailRaptor.class).toProvider(SwissRailRaptorFactory.class);

            for (String mode : getConfig().transit().getTransitModes()) {
                addRoutingModuleBinding(mode).toProvider(SwissRailRaptorRoutingModuleProvider.class);
            }
            addRoutingModuleBinding(TransportMode.transit_walk).to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
            
            SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(getConfig(), SwissRailRaptorConfigGroup.class);

            if (srrConfig.isUseRangeQuery()) {
                bind(RaptorRouteSelector.class).to(ConfigurableRaptorRouteSelector.class);
            } else {
                bind(RaptorRouteSelector.class).to(LeastCostRaptorRouteSelector.class); // just a simple default in case it ever gets used.
            }
            
            switch (srrConfig.getScoringParameters()) {
            case Default:
                bind(RaptorParametersForPerson.class).to(DefaultRaptorParametersForPerson.class);
                break;
            case Individual:
                bind(RaptorParametersForPerson.class).to(IndividualRaptorParametersForPerson.class);
                break;
            }

            if (srrConfig.isUseIntermodalAccessEgress()) {
                bind(MainModeIdentifier.class).to(IntermodalAwareRouterModeIdentifier.class);
                switch (srrConfig.getIntermodalAccessEgressModeSelection()) {
                case CalcLeastCostModePerStop:
                    bind(RaptorStopFinder.class).to(DefaultRaptorStopFinder.class);
                    break;
                case RandomSelectOneModePerRoutingRequestAndDirection:
                    bind(RaptorStopFinder.class).to(RandomAccessEgressModeRaptorStopFinder.class);
                    break;
                }
            } else {
	            bind(RaptorStopFinder.class).to(DefaultRaptorStopFinder.class);
            }
            
            bind(RaptorIntermodalAccessEgress.class).to(DefaultRaptorIntermodalAccessEgress.class);
        }

    }

}
