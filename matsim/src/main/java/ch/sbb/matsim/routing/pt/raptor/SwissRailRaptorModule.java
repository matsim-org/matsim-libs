/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouter;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorModule extends AbstractModule {

    private final OccupancyData occupancyData = new OccupancyData();

    public OccupancyData getExecutionData() {
        return this.occupancyData;
    }

    @Override
    public void install() {
        if (getConfig().transit().isUseTransit()) {
            bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
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

            bind(RaptorStopFinder.class).to(DefaultRaptorStopFinder.class);

            boolean useCapacityConstraints = srrConfig.isUseCapacityConstraints();
            bind(OccupancyData.class).toInstance(this.occupancyData);
            if (useCapacityConstraints) {
                addEventHandlerBinding().to(OccupancyTracker.class);
            }
            
            bind(RaptorIntermodalAccessEgress.class).to(DefaultRaptorIntermodalAccessEgress.class);
            bind(RaptorInVehicleCostCalculator.class).to(DefaultRaptorInVehicleCostCalculator.class);
            bind(RaptorTransferCostCalculator.class).to(DefaultRaptorTransferCostCalculator.class);
        }

    }

}
