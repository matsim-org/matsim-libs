package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

final class ImmissionInfo {

    double immission;

    Map<Id<Link>, Double> linkId2IsolatedImmission;
    Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> linkId2IsolatedImmissionPlusOneVehicle;

    public double getImmission() {
        return immission;
    }

    public void setImmission(double immission) {
        this.immission = immission;
    }

    public Map<Id<Link>, Double> getLinkId2IsolatedImmission() {
        return linkId2IsolatedImmission;
    }

    public void setLinkId2IsolatedImmission(Map<Id<Link>, Double> linkId2IsolatedImmission) {
        this.linkId2IsolatedImmission = linkId2IsolatedImmission;
    }

    public Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> getLinkId2IsolatedImmissionPlusOneVehicle() {
        return linkId2IsolatedImmissionPlusOneVehicle;
    }

    public void setLinkId2IsolatedImmissionPlusOneVehicle(Map<Id<NoiseVehicleType>, Map<Id<Link>, Double>> linkId2IsolatedImmissionPlusOneVehicle) {
        this.linkId2IsolatedImmissionPlusOneVehicle = linkId2IsolatedImmissionPlusOneVehicle;
    }
}
