package org.matsim.contrib.ev.extensions.placement;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.access.AttributeBasedChargerAccess;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.access.SubscriptionRegistry;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Enables charger placement
 */
public class ChargerPlacementModule extends AbstractModule {
    @Override
    public void install() {
        ChargerPlacementConfigGroup placementConfig = ChargerPlacementConfigGroup.get(getConfig(), false);

        if (placementConfig != null) {
            addControllerListenerBinding().to(ChargerPlacementManager.class);
            addControllerListenerBinding().to(ChargerPlacementListener.class);
            addEventHandlerBinding().to(ChargerPlacementCollector.class);

            bind(ChargerAccess.class).to(ChargerPlacementAccess.class);

            installOverridingQSimModule(new AbstractQSimModule() {
                @Override
                protected void configureQSim() {
                    addMobsimScopeEventHandlerBinding().to(ChargerPlacementScorer.class);
                }

                @Provides
                @Singleton
                ChargerPlacementScorer provideChargerPlacementScorer(ChargerPlacementManager manager,
                        ChargingPlanScoring scoring) {
                    return new ChargerPlacementScorer(manager, scoring, placementConfig.getBlacklistPenalty());
                }
            });
        }
    }

    @Provides
    @Singleton
    ChargerPlacementAccess provideChargerPlacementAccess(ChargerPlacementManager manager,
            SubscriptionRegistry subscriptionRegistry) {
        ChargerAccess delegate = new AttributeBasedChargerAccess(subscriptionRegistry);
        return new ChargerPlacementAccess(manager, delegate);
    }

    @Provides
    @Singleton
    ChargerPlacementCollector provideChargerPlacementCollector() {
        return new ChargerPlacementCollector();
    }

    @Provides
    @Singleton
    ChargerPlacementListener provideChargerPlacementListener(ChargerPlacementManager chargerManager,
            EventsManager eventsManager, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        return new ChargerPlacementListener(chargerManager, eventsManager, outputDirectoryHierarchy);
    }

    @Provides
    @Singleton
    ChargerPlacementManager provideChargerPlacementManager(ChargingInfrastructureSpecification infrastructure,
            OutputDirectoryHierarchy outputDirectoryHierarchy, ChargerPlacementCollector collector, Network network,
            ChargerPlacementConfigGroup placementConfig) {
        return new ChargerPlacementManager(infrastructure, outputDirectoryHierarchy, collector, network,
                placementConfig.getRemovalInterval(),
                placementConfig.getRemovalQuantile(), placementConfig.getRemoveUnused(),
                placementConfig.getObjective());
    }
}
