package org.matsim.contrib.carsharing.router;


import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.core.population.routes.RouteFactory;

@Singleton
public class CarsharingRouteFactory implements RouteFactory {
    @Override
    public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
        return new CarsharingRoute(startLinkId, endLinkId);
    }

    
    @Override
    public String getCreatedRouteType() {
        return "carsharing";
    }
}