package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

import javax.inject.Inject;

public class BicycleLinkSpeedCalculator implements LinkSpeedCalculator {

	@Inject
    private BicycleConfigGroup bicycleConfigGroup;

	@Inject
	private BicycleLinkSpeedCalculator() {
    }

	@Override
    public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {

        if (isBike(qVehicle))
            return getMaximumVelocityForBike(qVehicle, link, time);
        else
            return getDefaultMaximumVelocity(qVehicle, link, time);
    }

    private double getMaximumVelocityForBike(QVehicle qVehicle, Link link, double time) {
        try {
            double bicycleInfrastructureSpeedFactor = (double) link.getAttributes().getAttribute(BicycleLabels.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR);
            return Math.min(qVehicle.getMaximumVelocity() * bicycleInfrastructureSpeedFactor, link.getFreespeed(time));
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve speed factor from link. Set the speed factor for bikes on each link of the network if you want to use BikLinkSpeedCalculator!");
        }
    }

    private double getDefaultMaximumVelocity(QVehicle qVehicle, Link link, double time) {
        return Math.min(qVehicle.getMaximumVelocity(), link.getFreespeed(time));
    }

    private boolean isBike(QVehicle qVehicle) {
        return qVehicle.getVehicle().getType().getId().toString().equals(bicycleConfigGroup.getBicycleMode());
    }
}