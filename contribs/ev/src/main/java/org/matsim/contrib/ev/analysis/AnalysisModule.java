package org.matsim.contrib.ev.analysis;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class AnalysisModule extends AbstractModule {
    static private final String INJECTION_TAG = "ev_analysis";

    @Override
    public void install() {
    }

    @Named(INJECTION_TAG)
    @Singleton
    ZoneSystem provideAnalysisZoneSystem(EvConfigGroup evConfig, Network network,
            ChargingInfrastructureSpecification infrastructure) {
        String crs = getConfig().global().getCoordinateSystem();
        ZoneSystemParams zoneSystemParams = evConfig.addOrGetAnalysisZoneSystemParams();

        Predicate<Zone> zoneFilter = ZoneSystemUtils
                .createPredicateByLocations(infrastructure.getChargerSpecifications().values().stream()
                        .map(ChargerSpecification::getLinkId).map(network.getLinks()::get));

        return ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network, zoneSystemParams, crs, zoneFilter);
    }
}
