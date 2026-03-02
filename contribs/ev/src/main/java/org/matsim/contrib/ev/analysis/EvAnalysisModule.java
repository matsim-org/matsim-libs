package org.matsim.contrib.ev.analysis;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.io.ZoneShpWriter;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Module handling analysis for EV contrib.
 * 
 * TODO: The content from /stats should be cleaned up and added here.
 * 
 * @author sebhoerl, IRT SystemX
 */
public class EvAnalysisModule extends AbstractModule {
    private final Logger logger = LogManager.getLogger(EvAnalysisModule.class);

    static private final String INJECTION_TAG = "ev_analysis";
    static private final String GEOMETRY_FILE = "ev_analysis_zones.shp";

    @Override
    public void install() {
        EvConfigGroup evConfig = EvConfigGroup.get(getConfig());

        if (evConfig.getWriteVehicleTrajectoriesInterval() > 0) {
            addControllerListenerBinding().to(VehicleTrajectoryListener.class);
        }

        if (evConfig.getWriteZonalEnergyDemandInterval() > 0) {
            addControllerListenerBinding().to(ZonalEnergyDemandListener.class);
        }

        if (evConfig.getWriteChargingActivitiesInterval() > 0) {
            addControllerListenerBinding().to(ChargingActivityListener.class);
        }

        if (evConfig.getWriteVehicleSocInterval() > 0) {
            addControllerListenerBinding().to(VehicleSocListener.class);
            addMobsimListenerBinding().to(VehicleSocListener.class);
        }
    }

    @Provides
    @Singleton
    VehicleTrajectoryListener provideVehicleTrajectoryListener(EventsManager eventsManager, Network network,
            ElectricFleetSpecification electricFleet, ChargingInfrastructureSpecification infrastructure,
            OutputDirectoryHierarchy outputHierarchy,
            EvConfigGroup evConfig) {
        return new VehicleTrajectoryListener(eventsManager, network, electricFleet, infrastructure, outputHierarchy,
                evConfig.getWriteVehicleTrajectoriesInterval(), getConfig().controller().getCompressionType());
    }

    @Provides
    @Singleton
    public ChargingActivityListener provideChargingAnalysisListener(
            ChargingInfrastructureSpecification chargingInfrastructureSpecification,
            Network network, EventsManager eventsManager,
            OutputDirectoryHierarchy outputDirectoryHierarchy, EvConfigGroup evConfig) {
        return new ChargingActivityListener(chargingInfrastructureSpecification, network, outputDirectoryHierarchy,
                eventsManager, evConfig.getWriteChargingActivitiesInterval(),
                getConfig().controller().getCompressionType());
    }

    @Provides
    @Singleton
    VehicleSocListener provideVehicleSocListener(EventsManager eventsManager,
            ElectricFleetSpecification electricFleet,
            OutputDirectoryHierarchy outputHierarchy,
            EvConfigGroup evConfig) {
        return new VehicleSocListener(eventsManager, electricFleet, outputHierarchy,
                evConfig.getWriteVehicleSocInterval(), evConfig.getWriteVehicleSocFrequency(),
                getConfig().controller().getCompressionType());
    }

    @Provides
    @Named(INJECTION_TAG)
    @Singleton
    ZoneSystem provideAnalysisZoneSystem(EvConfigGroup evConfig, Network network,
            ChargingInfrastructureSpecification infrastructure, OutputDirectoryHierarchy outputHierarchy) {
        String crs = getConfig().global().getCoordinateSystem();
        ZoneSystemParams zoneSystemParams = evConfig.addOrGetAnalysisZoneSystemParams();

        Predicate<Zone> zoneFilter = ZoneSystemUtils
                .createPredicateByLocations(infrastructure.getChargerSpecifications().values().stream()
                        .map(ChargerSpecification::getLinkId).map(network.getLinks()::get));

        ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network, zoneSystemParams,
                crs, zoneFilter);

        if (!crs.equals("ATLANTIS")) {
            String geometryPath = outputHierarchy.getOutputFilename(GEOMETRY_FILE);
            new ZoneShpWriter(zoneSystem.getZones(), crs).write(geometryPath);
        } else {
            logger.error("Cannot write out EV analysis zone system because no valid global CRS is given.");
        }

        return zoneSystem;
    }

    @Provides
    @Singleton
    ZonalEnergyDemandListener provideZonalEnergyDemandListener(EventsManager eventsManager,
            @Named(INJECTION_TAG) ZoneSystem zoneSystem,
            ChargingInfrastructureSpecification infrastructure, OutputDirectoryHierarchy outputHierarchy,
            EvConfigGroup evConfig) {
        return new ZonalEnergyDemandListener(eventsManager, outputHierarchy, zoneSystem, infrastructure,
                evConfig.getWriteZonalEnergyDemandInterval(), getConfig().controller().getCompressionType());
    }
}
