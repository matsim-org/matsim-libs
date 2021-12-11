package org.matsim.contrib.freight.jsprit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author steffenaxer
 */
public class NetworkBasedTransportCostsFactory implements VRPTransportCostsFactory {
    Scenario scenario;
    Carriers carriers;
    Map<String, TravelTime> travelTimes;
    Config config;

    public NetworkBasedTransportCostsFactory(Scenario scenario, Carriers carriers, Map<String, TravelTime> travelTimes, Config config) {
        this.scenario = scenario;
        this.carriers = carriers;
        this.travelTimes = travelTimes;
        this.config = config;

    }

    @Override
    public VRPTransportCosts createVRPTransportCosts() {
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config,
                FreightConfigGroup.class);

        Set<VehicleType> vehicleTypes = new HashSet<>();
        carriers.getCarriers().values().forEach(
                carrier -> vehicleTypes.addAll(carrier.getCarrierCapabilities().getVehicleTypes()));

        NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder
                .newInstance(scenario.getNetwork(), vehicleTypes);

        netBuilder.setTimeSliceWidth(freightConfigGroup.getTravelTimeSliceWidth());
        netBuilder.setTravelTime(travelTimes.get(TransportMode.car));
        return netBuilder.build();
    }
}
