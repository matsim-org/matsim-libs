package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.Set;

public interface NoiseContext {
    // for routing purposes
    void storeTimeInterval();

    Scenario getScenario();

    Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints();

    NoiseConfigGroup getNoiseParams();

    double getCurrentTimeBinEndTime();

    void setCurrentTimeBinEndTime(double currentTimeBinEndTime);

    Map<Id<Link>, NoiseLink> getNoiseLinks();

    Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks();

    void setEventTime(double time);

    double getEventTime();

    Grid getGrid();

    Set<Id<Vehicle>> getBusVehicleIDs();

    Map<Id<Link>, Map<Id<Vehicle>, Double>> getLinkId2vehicleId2lastEnterTime();

    Set<Id<Vehicle>> getNotConsideredTransitVehicleIDs();

    Map<Id<Vehicle>, Id<Person>> getVehicleId2PersonId();

    Set<Id<Vehicle>> getIgnoredNetworkModeVehicleIDs();

    void reset();

    Set<Id<Link>> getPotentialLinks(NoiseReceiverPoint nrp);
}
