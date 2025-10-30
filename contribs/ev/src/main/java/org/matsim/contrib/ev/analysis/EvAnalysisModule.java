package org.matsim.contrib.ev.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Module handling analysis for EV contrib.
 * 
 * TODO: The content from /stats should be cleaned up and added here.
 * 
 * @author sebhoerl, IRT SystemX
 */
public class EvAnalysisModule extends AbstractModule {
    @Override
    public void install() {
        addControllerListenerBinding().to(VehicleTrajectoryListener.class);
    }

    @Provides
    @Singleton
    VehicleTrajectoryListener provideVehicleTrajectoryListener(EventsManager eventsManager, Network network,
            ElectricFleetSpecification electricFleet, OutputDirectoryHierarchy outputHierarchy,
            EvConfigGroup evConfig) {
        return new VehicleTrajectoryListener(getConfig(), eventsManager, network, electricFleet, outputHierarchy,
                evConfig.getWriteVehicleTrajectoriesInterval());
    }
}
