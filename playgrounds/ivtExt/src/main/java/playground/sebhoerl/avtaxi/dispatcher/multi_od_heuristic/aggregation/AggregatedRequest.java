package playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.aggregation;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.TravelTimeEstimator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import java.util.Collection;
import java.util.LinkedList;

public class AggregatedRequest {
    final private AVRequest master;
    final Collection<AVRequest> slaves = new LinkedList<>();

    final long occupancyThreshold = 4;
    final double distanceThreshold;

    private AVVehicle vehicle = null;

    final private TravelTimeEstimator estimator;

    public AggregatedRequest(AVRequest master, TravelTimeEstimator estimator) {
        this.master = master;
        this.estimator = estimator;

        distanceThreshold = estimator.getThreshold();
    }

    public AVRequest getMasterRequest() {
        return master;
    }

    public void addSlaveRequest(AVRequest slave) {
        slaves.add(slave);
    }

    public Collection<AVRequest> getSlaveRequests() {
        return slaves;
    }

    public Double accept(AVRequest candidate) {
        if (slaves.size() >= occupancyThreshold - 1) {
            return null;
        }

        double distance1 = estimator.getDistance(candidate.getToLink(), master.getToLink(), master.getEarliestStartTime());

        if (distance1 > distanceThreshold) {
            return null;
        }

        double distance2 = estimator.getDistance(candidate.getFromLink(), master.getFromLink(), master.getEarliestStartTime());

        if (distance2 > distanceThreshold) {
            return null;
        }

        return distance1 + distance2;
    }

    public void setVehicle(AVVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public AVVehicle getVehicle() {
        return vehicle;
    }
}
