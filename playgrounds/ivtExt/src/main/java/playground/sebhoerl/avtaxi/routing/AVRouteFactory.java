package playground.sebhoerl.avtaxi.routing;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;
import playground.sebhoerl.avtaxi.data.AVOperator;

@Singleton
public class AVRouteFactory implements RouteFactory {
    @Override
    public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
        return new AVRoute(startLinkId, endLinkId, null);
    }

    public AVRoute createRoute(Id<Link> startLinkId, Id<Link> endLinkId, Id<AVOperator> operatorId) {
        return new AVRoute(startLinkId, endLinkId, operatorId);
    }

    @Override
    public String getCreatedRouteType() {
        return AVRoute.AV_ROUTE;
    }
}
