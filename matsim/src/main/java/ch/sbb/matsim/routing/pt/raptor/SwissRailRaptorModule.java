/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;

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
            }
            bind(RaptorStopFinder.class).to(DefaultRaptorStopFinder.class);

            
            bind(RaptorIntermodalAccessEgress.class).to(DefaultRaptorIntermodalAccessEgress.class);
        }

    }

}
