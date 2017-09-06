package playground.clruch.traveltimetracker;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public class AVTravelTime implements TravelTime {
    final private AVTravelTimeTracker travelTimeTracker;
    final private TravelTime delegate;

    final private double maximumInterpolationTime = 300.0;
    final private double exponent = 1.0;

    public AVTravelTime(AVTravelTimeTracker travelTimeTracker, TravelTime delegate) {
        this.delegate = delegate;
        this.travelTimeTracker = travelTimeTracker;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        AVTravelTimeTracker.LinkTravelTime travelTime = travelTimeTracker.getLinkTravelTime(link.getId());
        double delegateTravelTime = delegate.getLinkTravelTime(link, time, person, vehicle);

        double returnTravelTime = 0.0;
        if (travelTime.updateTime > time - maximumInterpolationTime) {
            returnTravelTime = interpolate(delegateTravelTime, travelTime.travelTime, (time - travelTime.updateTime) / maximumInterpolationTime);
        } else {
            returnTravelTime = delegateTravelTime;
        }

        GlobalAssert.that(returnTravelTime >= 0.0);
        return returnTravelTime;
    }

    private double interpolate(double freespeedTravelTime, double measuredTravelTime, double relativeElapsedTime) {
        return Math.pow(1.0 - relativeElapsedTime, exponent) * measuredTravelTime + Math.pow(relativeElapsedTime, exponent) * freespeedTravelTime;
    }

}
