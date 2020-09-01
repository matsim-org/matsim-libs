package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NoiseContextStub implements NoiseContext {

    private final Scenario scenario;
    private Map<Id<Link>, NoiseLink> noiseLinks = new HashMap<>();

    public NoiseContextStub(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void storeTimeInterval() {

    }

    @Override
    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints() {
        return null;
    }

    @Override
    public NoiseConfigGroup getNoiseParams() {
        return ConfigUtils.addOrGetModule(scenario.getConfig(), NoiseConfigGroup.class);
    }

    @Override
    public double getCurrentTimeBinEndTime() {
        return 0;
    }

    @Override
    public void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {

    }

    @Override
    public Map<Id<Link>, NoiseLink> getNoiseLinks() {
        return noiseLinks;
    }

    @Override
    public Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks() {
        return null;
    }

    @Override
    public void setEventTime(double time) {

    }

    @Override
    public double getEventTime() {
        return 0;
    }

    @Override
    public Grid getGrid() {
        return null;
    }

    @Override
    public Set<Id<Vehicle>> getBusVehicleIDs() {
        return null;
    }

    @Override
    public Map<Id<Link>, Map<Id<Vehicle>, Double>> getLinkId2vehicleId2lastEnterTime() {
        return null;
    }

    @Override
    public Set<Id<Vehicle>> getNotConsideredTransitVehicleIDs() {
        return null;
    }

    @Override
    public Map<Id<Vehicle>, Id<Person>> getVehicleId2PersonId() {
        return null;
    }

    @Override
    public Set<Id<Vehicle>> getIgnoredNetworkModeVehicleIDs() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public Set<Id<Link>> getPotentialLinks(NoiseReceiverPoint nrp) {
        return null;
    }
}
