package playground.balac.twowaycarsharing.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class FtCarSharingRouteFactory
  implements RouteFactory
{

  public Route createRoute(Id startLinkId, Id endLinkId)
  {
    return new FtCarSharingRoute(startLinkId, endLinkId);
  }
}
