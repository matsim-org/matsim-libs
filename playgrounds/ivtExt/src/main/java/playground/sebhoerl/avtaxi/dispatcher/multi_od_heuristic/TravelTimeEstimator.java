package playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordUtils;

public class TravelTimeEstimator {
    final private LeastCostPathCalculator router;
    private final double threshold;

    public TravelTimeEstimator(LeastCostPathCalculator router, double threshold) {
        this.router = router;
        this.threshold = threshold;
    }

    public double getDistance(Link fromLink, Link toLink, double startTime) {
        return 3.6 * CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord()) / 40.0;

        /*if (fromLink.equals(toLink)) {
            return 0.0;
        }

        return router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), startTime, null, null).travelTime;*/
    }

    public double getThreshold() {
        return threshold;
    }
}
