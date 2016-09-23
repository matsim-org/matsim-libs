package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.sebhoerl.av.logic.service.Service;

public class AVRouter {
    final private LeastCostPathCalculator pathCalculator;
    final private Network network;
    
    public AVRouter(Network network, AVTravelTime travelTime) {
        this.network = network;
        pathCalculator = new Dijkstra(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }
    
    private NetworkRoute createRoute(Id<Link> fromLinkId, Id<Link> toLinkId, double startTime) {
        // see {@org.matsim.core.router.NetworkRoutingModule}
        
        Node startNode = network.getLinks().get(fromLinkId).getToNode();
        Node endNode = network.getLinks().get(toLinkId).getFromNode();
        Path path = pathCalculator.calcLeastCostPath(startNode, endNode, startTime, null, null);
        
        NetworkRoute route = new LinkNetworkRouteImpl(fromLinkId, NetworkUtils.getLinkIds(path.links), toLinkId);
        route.setTravelTime(path.travelTime);
        route.setTravelCost(path.travelCost);
        route.setDistance(RouteUtils.calcDistance(route, 0.0, 0.0, network));
        
        // TODO: I think the distance calculation is soon to change in MATSim master
        // make sure that this is updated here as well...
        // For instance, keep an eye on QLinkImpl at
        // https://github.com/matsim-org/matsim/commits/master/matsim/src/main/java/org/matsim/core/mobsim/qsim/qnetsimengine/QLinkImpl.java
        
        return route;
    }
    
    public NetworkRoute createPickupRoute(Service service) {
        return createRoute(
                service.getStartLinkId(),
                service.getRequest().getPickupLinkId(),
                service.getDispatchmentTime()); // TODO TIme dependent!
    }
    
    public NetworkRoute createDropoffRoute(Service service) {
        return createRoute(
                service.getRequest().getPickupLinkId(),
                service.getRequest().getDropoffLinkId(),
                service.getDispatchmentTime()); // TODO TIme dependent!
    }
}
